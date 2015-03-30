package poly.darkdepths.strongbox;

/**
 * Created by poly on 3/30/15.
 */

import android.app.Activity;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.jcodec.common.SeekableByteChannel;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.FileOutputStream;
import info.guardianproject.iocipher.VirtualFileSystem;
import poly.darkdepths.strongbox.encoders.AACHelper;
import poly.darkdepths.strongbox.encoders.ImageToMJPEGMOVMuxer;
import poly.darkdepths.strongbox.encoders.MediaConstants;
import poly.darkdepths.strongbox.io.IOCipherFileChannelWrapper;

/**
 * The left fragment, containing the camera preview and recording button
 */
public class CameraFragment extends Fragment {
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
        releaseCameraAndPreview();

        Globals appState = (Globals) getActivity().getApplicationContext();
        VirtualFileSystem vfs = appState.getVFS();

        if (vfs.isMounted())
            vfs.unmount();

        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
        safeCameraOpenInView(getView());

        Globals appState = (Globals) getActivity().getApplicationContext();
        Security securestore = appState.getSecurestore();
        VirtualFileSystem vfs = appState.getVFS();

        if (!vfs.isMounted())
            vfs.mount(appState.getDbFile(), securestore.getKey().getEncoded());
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
            Camera.PreviewCallback callback = new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    //Log.d("OnPreviewFrame", "New Frame Recieved");

                    if (mIsRecording && mFrameQ != null) {

                        Camera.Parameters parameters = camera.getParameters();
                        mLastWidth = parameters.getPreviewSize().width;
                        mLastHeight = parameters.getPreviewSize().height;

                        mPreviewFormat = parameters.getPreviewFormat();

                        byte[] dataResult = data;

                        if (mPreCompressFrames) {
                            YuvImage yuv = new YuvImage(dataResult, mPreviewFormat, mLastWidth, mLastHeight, null);
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            yuv.compressToJpeg(new Rect(0, 0, mLastWidth, mLastHeight), MediaConstants.sJpegQuality, out);
                            dataResult = out.toByteArray();
                        }

                        synchronized (mFrameQ) {
                            if (data != null) {
                                mFrameQ.add(dataResult);
                                mFramesTotal++;

                                frameCounter++;
                                if ((System.currentTimeMillis() - start) >= 1000) {
                                    mFPS = frameCounter;
                                    Log.d("Strongbox","FPS: " + mFPS);
                                    frameCounter = 0;
                                    start = System.currentTimeMillis();
                                }
                            }
                        }
                    }
                }
            };
            mCamera.setPreviewCallback(callback);
            Log.d("Strongbox", "Camera callback setup successfully");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("Strongbox", "Error setting up callback");
            return false;
        }
    }


    private void startRecording ()
    {
        mFrameQ = new ArrayDeque<byte[]>();

        mFramesTotal = 0;

        String fileName = "video.mov";// + new java.util.Date().getTime() + ".mov";
        info.guardianproject.iocipher.File fileOut = new info.guardianproject.iocipher.File(mFileBasePath,fileName);

        try {
            mIsRecording = true;

            if (useAAC)
                initAudio(fileOut.getAbsolutePath()+".aac");
            else
                initAudio(fileOut.getAbsolutePath()+".pcm");

            new Encoder(fileOut).start();
            //start capture
            startAudioRecording();

            //progress.setText("[REC]");
            Log.d("Strongbox", "Started Recording");
        } catch (Exception e) {
            Log.d("Video","error starting video",e);
            //Toast.makeText(this, "Error init'ing video: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
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

            org.jcodec.common.AudioFormat af = null;//new org.jcodec.common.AudioFormat(org.jcodec.common.AudioFormat.MONO_S16_LE(MediaConstants.sAudioSampleRate));

            muxer = new ImageToMJPEGMOVMuxer(sbc,af);
        }

        public void run ()
        {
            try {
                while (mIsRecording || (!mFrameQ.isEmpty()))
                {
                    if (mFrameQ.peek() != null)
                    {
                        byte[] data = mFrameQ.pop();
                        muxer.addFrame(mLastWidth, mLastHeight, ByteBuffer.wrap(data), mFPS);
                    }
                }

                muxer.finish();

                fos.close();

                //setResult(Activity.RESULT_OK, new Intent().putExtra(MediaStore.EXTRA_OUTPUT, fileOut.getAbsolutePath()));

                //if (isRequest)
                //    getActivity().finish();

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
            if (msg.what == 0) {
                int frames = msg.getData().getInt("frames");
                    /*
                    if (!mIsRecording)
                        if (frames == 0)
                            progress.setText("");
                        else
                            progress.setText("Processing: " + (mFramesTotal-frames) + '/' +  mFramesTotal);
                    else
                        progress.setText("Recording: " + mFramesTotal);
                        */
            }
            else if (msg.what == 1) {
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
                if (this.getCameraDirection() == Camera.CameraInfo.CAMERA_FACING_FRONT)
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

        //mIsRecording = false;
        //isRecording = false;

        Log.d("Strongbox", "Stopped Recording");
        Log.d("Strongbox", "Total Frames " + mFramesTotal);

        Globals appState = (Globals) getActivity().getApplicationContext();
        Security securestore = appState.getSecurestore();

        // load encrypted SQLlite database
        SQLiteDatabase.loadLibs(getActivity());
        java.io.File databaseFile = getActivity().getDatabasePath(appState.getDatabaseName());

        // attempt to open database, this will fail if password is wrong
        // or if database is otherwise corrupted
        SQLiteDatabase database = SQLiteDatabase.openDatabase(
                databaseFile.getPath(),
                new String(securestore.getKey().getEncoded()), null, SQLiteDatabase.OPEN_READWRITE);

        Long time = (long) mFramesTotal/mFPS;

        Video temp_video = new Video("Video_" + new java.util.Date().getTime(), time);
        DataStore.storeVideo(appState, database, temp_video);

        Cursor cursor = database.rawQuery("SELECT  * FROM " + appState.getTableName(), null);

        TodoCursorAdapter adapter = new TodoCursorAdapter(getActivity().getApplicationContext(), cursor);
        ListView listView = (ListView) getActivity().findViewById(R.id.listView);
        listView.setAdapter(adapter);
        listView.invalidateViews();

        // TODO properly close cursor
            /*
            if( cursor != null && cursor.moveToFirst() ){
                cursor.close();
            }
            */

        database.close();

        mCamera.setPreviewCallback(null);

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
                                Log.d("STRONGBOX", "Failed to init camera");
                                isRecording = false;
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
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
}
