package poly.darkdepths.strongbox;

import java.io.IOException;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.Toast;

public class CameraActivity extends ActionBarActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
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

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
        private MediaRecorder recorder = null;
        private boolean isRecording = false;

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
            releaseMediaRecorder();
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

        protected boolean prepareForVideoRecording() {
            mCamera.unlock();
            recorder = new MediaRecorder();

            recorder.setCamera(mCamera);

            recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(/*MediaRecorder.OutputFormat.OUTPUT_FORMAT_MPEG2TS*/8);
            //recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            recorder.setAudioSamplingRate(48000);
            recorder.setAudioEncodingBitRate(128000);

            Camera.Parameters params = mCamera.getParameters();
            Camera.Size optimalSize = mPreview.getOptimalPreviewSize(params.getSupportedPreviewSizes(),  getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
            recorder.setVideoSize(optimalSize.width,optimalSize.height);

            recorder.setOutputFile(FileManagement.getStreamFd());

            recorder.setPreviewDisplay(mPreview.getHolder().getSurface());

            try {
                recorder.prepare();
            } catch (IllegalStateException e) {
                Log.e("Strongbox", "IllegalStateException when preparing MediaRecorder "
                        + e.getMessage());
                e.getStackTrace();
                releaseMediaRecorder();
                return false;
            } catch (IOException e) {
                Log.e("Strongbox", "IOException when preparing MediaRecorder "
                        + e.getMessage());
                e.getStackTrace();
                releaseMediaRecorder();
                return false;
            }
            return true;
        }

        private void releaseMediaRecorder() {
            if (recorder != null) {
                recorder.reset();
                recorder.release();
                recorder = null;
                mCamera.lock();
            }
        }

        private void setUpVideoButton(View view) {
            final ImageButton recordVideoButton = (ImageButton)view.findViewById(R.id.recordButton);

            recordVideoButton.setOnClickListener(
                    new View.OnClickListener() {
                        public void onClick(View v) {
                            if (isRecording) {
                                recorder.stop();
                                releaseMediaRecorder();
                                mCamera.lock();

                                isRecording = false;
                            } else {
                                if (prepareForVideoRecording()) {
                                    recordVideoButton.setColorFilter(Color.RED);
                                    recorder.start();

                                    isRecording = true;
                                } else {
                                    // Something has gone wrong! Release the camera
                                    releaseMediaRecorder();
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

    /**
     * The right fragment, containing the gallery previews and access to settings.
     */
    public static class GalleryFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static GalleryFragment newInstance(int sectionNumber) {
            GalleryFragment fragment = new GalleryFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public GalleryFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_gallery, container, false);
            return rootView;
        }
    }
}

