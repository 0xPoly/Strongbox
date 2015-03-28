package poly.darkdepths.strongbox;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.logging.Handler;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.logging.Logger;
import org.jcodec.movtool.Remux;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileDescriptor;
import info.guardianproject.iocipher.FileOutputStream;
import info.guardianproject.iocipher.VirtualFileSystem;

public class CameraActivity extends ActionBarActivity {

    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);


        // Create the adapter that will return a fragment for each of the two
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // no screenshots for you!
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_lock:
                System.runFinalization();
                System.exit(0);
                return true;
            case R.id.action_settings:
                //openSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position) {
                case 0:
                    return CameraFragment.newInstance(position + 1);
                case 1:
                    return GalleryFragment.newInstance(position + 1);
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
            }
            return null;
        }
    }

    /**
     * The left fragment, containing the camera preview and recording button
     */
    public static class CameraFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private Camera mCamera = null;
        private CameraPreview mPreview;
        private boolean isRecording = false;

        private final static String LOG = "VideoJPEGRecorder";
        private String mFileBasePath = null;
        private boolean mIsRecording = false;
        private ArrayDeque<byte[]> mFrameQ = null;
        private int mLastWidth = -1;
        private int mLastHeight = -1;
        private int mPreviewFormat = -1;
        private ImageToMJPEGMOVMuxer muxer;
        private AACHelper aac;
        private boolean useAAC = false;
        private byte[] audioData;
        private AudioRecord audioRecord;
        private int mFramesTotal = 0;
        private int mFPS = 0;
        private boolean mPreCompressFrames = true;
        private OutputStream outputStreamAudio;
        private info.guardianproject.iocipher.File fileAudio;
        private int frameCounter = 0;
        private long start = 0;
        private boolean isRequest = false;
        private boolean mInTopHalf = false;

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static CameraFragment newInstance(int sectionNumber) {
            CameraFragment fragment = new CameraFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public CameraFragment() {
        }

        public Camera getmCamera(){
            return this.mCamera;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_camera, container, false);
            setUpVideoButton(rootView);
            setUpGalleryButton(rootView);
            return rootView;
        }

        @Override
        public void onPause(){
            super.onPause();
            stopRecording();
            releaseCameraAndPreview();
        }

        @Override
        public void onResume(){
            super.onResume();
            safeCameraOpenInView(getView());
            setUpVideoButton(getView());
        }

        /**
         * safely returns instance of camera
         * @return
         */
        public static Camera getCameraInstance(){
            Camera c = null;
            try {
                c = Camera.open();
            }
            catch (Exception e) {
                // Camera is not available
                // TODO use R.string.app_name instead
                Log.e("Strongbox", "failed to open Camera");
                e.printStackTrace();
            }
            return c;
        }

        private boolean safeCameraOpenInView(View view) {
            boolean qOpened = false;
            releaseCameraAndPreview();
            mCamera = getCameraInstance();
            qOpened = (mCamera != null);
            mPreview = new CameraPreview(getActivity().getBaseContext(), mCamera);
            FrameLayout preview = (FrameLayout) view.findViewById(R.id.camera_preview);
            preview.addView(mPreview);
            // TODO change 0 to proper camera ID detection
            setCameraDisplayOrientation(this.getActivity(), 0, mCamera);
            return qOpened;
        }



        public static void setCameraDisplayOrientation(Activity activity,
                                                       int cameraId, android.hardware.Camera camera) {
            android.hardware.Camera.CameraInfo info =
                    new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(cameraId, info);
            int rotation = activity.getWindowManager().getDefaultDisplay()
                    .getRotation();
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0: degrees = 0; break;
                case Surface.ROTATION_90: degrees = 90; break;
                case Surface.ROTATION_180: degrees = 180; break;
                case Surface.ROTATION_270: degrees = 270; break;
            }

            int result;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360;
                result = (360 - result) % 360;  // compensate the mirror
            } else {  // back-facing
                result = (info.orientation - degrees + 360) % 360;
            }
            camera.setDisplayOrientation(result);
        }

         boolean prepareForVideoRecording(){
            try {
                FileDescriptor fileDescriptor = new FileOutputStream("file.java").getFD();

                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }


        private void startRecording ()
        {
            mFrameQ = new ArrayDeque<byte[]>();
            mFramesTotal = 0;
            String fileName = "video" + new java.util.Date().getTime() + ".mov";
            info.guardianproject.iocipher.File fileOut = new info.guardianproject.iocipher.File(fileName);

            try {
                mIsRecording = true;
                if (useAAC)
                    initAudio(fileOut.getAbsolutePath()+".aac");
                else
                    initAudio(fileOut.getAbsolutePath()+".pcm");
                new Encoder(fileOut).start();
//start capture
                startAudioRecording();
            } catch (Exception e) {
                Log.d("Video","error starting video",e);
                Toast.makeText(getActivity(), "Error init'ing video: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                //finish();
            }
        }

        private class Encoder extends Thread {
            private static final String TAG = "ENCODER";

            private File fileOut;
            private FileOutputStream fos;

            public Encoder (File fileOut) throws IOException
            {
                this.fileOut = fileOut;

                fos = new info.guardianproject.iocipher.FileOutputStream(fileOut);
                SeekableByteChannel sbc = new IOCipherFileChannelWrapper(fos.getChannel());

                org.jcodec.common.AudioFormat af = null; //new org.jcodec.common.AudioFormat(org.jcodec.common.AudioFormat.MONO_S16_LE(MediaConstants.sAudioSampleRate));

                muxer = new ImageToMJPEGMOVMuxer(sbc,af);
            }

            public void run ()
            {

                try {

                    while (mIsRecording || (!mFrameQ.isEmpty()))
                    {
                        Log.d("recording", "this part");

                        if (mFrameQ.peek() != null)
                        {
                            byte[] data = mFrameQ.pop();

                            Log.d("recording", "frame received");

                            muxer.addFrame(mLastWidth, mLastHeight, ByteBuffer.wrap(data),mFPS);

                        }

                    }

                    muxer.finish();

                    fos.close();

                    //setResult(Activity.RESULT_OK, new Intent().putExtra(MediaStore.EXTRA_OUTPUT, fileOut.getAbsolutePath()));

                    //if (isRequest)
                        //finish();

                } catch (Exception e) {
                    Log.e(TAG, "IO", e);
                }

            }



        }

        android.os.Handler h = new android.os.Handler()
        {
            @Override
            public void handleMessage(Message msg) {
                // TODO Auto-generated method stub
                super.handleMessage(msg);

                if (msg.what == 0)
                {
                    int frames = msg.getData().getInt("frames");
                }
                else if (msg.what == 1)
                {
                    mIsRecording = false; //stop recording

                    if (aac != null)
                        aac.stopRecording();
                }
            }

        };

        private void initAudio(final String audioPath) throws Exception {

            fileAudio  = new File(audioPath);

            outputStreamAudio = new BufferedOutputStream(new info.guardianproject.iocipher.FileOutputStream(fileAudio),8192*8);

            if (useAAC)
            {
                aac = new AACHelper();
                aac.setEncoder(MediaConstants.sAudioSampleRate, MediaConstants.sAudioChannels, MediaConstants.sAudioBitRate);
            }
            else
            {

                int minBufferSize = AudioRecord.getMinBufferSize(MediaConstants.sAudioSampleRate,
                        MediaConstants.sChannelConfigIn,
                        AudioFormat.ENCODING_PCM_16BIT)*8;

                audioData = new byte[minBufferSize];

                int audioSource = MediaRecorder.AudioSource.CAMCORDER;

                /*
                if (mCamera.getCameraDirection() == Camera.CameraInfo.CAMERA_FACING_FRONT)
                {
                    audioSource = MediaRecorder.AudioSource.MIC;

                }
                */

                audioRecord = new AudioRecord(audioSource,
                        MediaConstants.sAudioSampleRate,
                        MediaConstants.sChannelConfigIn,
                        AudioFormat.ENCODING_PCM_16BIT,
                        minBufferSize);
            }
        }

        private void startAudioRecording ()
        {
            Thread thread = new Thread ()
            {
                public void run ()
                {
                    if (useAAC)
                    {
                        try {
                            aac.startRecording(outputStreamAudio);
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    else
                    {
                        audioRecord.startRecording();

                        while(mIsRecording){
                            int audioDataBytes = audioRecord.read(audioData, 0, audioData.length);
                            if (AudioRecord.ERROR_INVALID_OPERATION != audioDataBytes
                                    && outputStreamAudio != null) {
                                try {
                                    outputStreamAudio.write(audioData,0,audioDataBytes);

                                    //muxer.addAudio(ByteBuffer.wrap(audioData, 0, audioData.length));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        audioRecord.stop();
                        try {
                            outputStreamAudio.flush();
                            outputStreamAudio.close();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }


                }
            };
            thread.start();
        }

        private void stopRecording() {
            h.sendEmptyMessageDelayed(1, 2000);
            mIsRecording = false;
        }

        private void setUpVideoButton(View view) {
            final ImageButton recordVideoButton = (ImageButton)view.findViewById(R.id.recordButton);

            recordVideoButton.setOnClickListener(
                    new View.OnClickListener() {
                        public void onClick(View v) {
                            if (isRecording) {
                                recordVideoButton.setBackgroundResource(R.drawable.ic_video_call);
                                stopRecording();

                                isRecording = false;
                            } else {
                                if (prepareForVideoRecording()) {
                                    recordVideoButton.setBackgroundResource(R.drawable.ic_stop);
                                    startRecording();

                                    isRecording = true;
                                } else {
                                    //recordVideoButton.setBackgroundResource(R.drawable.ic_video_call);
                                    // Something has gone wrong! Release the camera

                                }
                            }
                        }
                    }
            );
        }

        private void setUpGalleryButton(final View view) {
            final ImageButton galleryButton = (ImageButton)view.findViewById(R.id.galleryButton);
            galleryButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // switch to gallery tab on click
                            ViewPager viewPager = (ViewPager) getActivity().findViewById(R.id.pager);
                            viewPager.setCurrentItem(1);

                        }
                    }
            );
        }



        /**
         * clean up after preview is finished
         */
        private void releaseCameraAndPreview(){
            // TODO
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        }
    }
}

