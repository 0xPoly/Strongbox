package poly.darkdepths.strongbox;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.ContentValues;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.YuvImage;
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
import android.widget.FrameLayout;
import android.widget.ImageButton;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

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

        private Camera.Parameters params = null;
        private int mPreviewFormat = Integer.MIN_VALUE;
        private int mPreviewWidth = Integer.MIN_VALUE;
        private int mPreviewHeight = Integer.MIN_VALUE;
        private Rect mPreviewRect = null;
        Camera.PreviewCallback callback = null;
        BufferedOutputStream out = null;

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
            params = mCamera.getParameters();
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

        protected boolean prepareForVideoRecording(){
            // setup callback preferences
            mPreviewFormat = params.getPreviewFormat();
            final Camera.Size previewSize = params.getPreviewSize();
            mPreviewWidth = previewSize.width;
            mPreviewHeight = previewSize.height;
            mPreviewRect = new Rect(0, 0, mPreviewWidth, mPreviewHeight);

            callback = new Camera.PreviewCallback()
            {
                public void onPreviewFrame(byte[] data, Camera camera)
                {
                    // Create JPEG
                    YuvImage image = new YuvImage(data, mPreviewFormat, mPreviewWidth, mPreviewHeight,
                            null /* strides */);
                    // TODO quality adjustment
                    image.compressToJpeg(mPreviewRect, 50, out);

                    // Send it over the network ...
                }
            };
            return true;
        }

        private void startRecording(){
            out = StreamHandler.getStreamOs();
            mCamera.setPreviewCallback(callback);
        }

        private void stopRecording() {
            mCamera.setPreviewCallback(null);
            if (out != null) {
                StreamHandler.closeStreamOs(out);
            }
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

