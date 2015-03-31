package poly.darkdepths.strongbox;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;

import java.util.List;
import java.util.Locale;

import poly.darkdepths.strongbox.player.MjpegViewerActivity;

public class CameraActivity extends ActionBarActivity {

    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    public void magic(View view) {

        String fileName = "/video.mov";
        info.guardianproject.iocipher.File file = new info.guardianproject.iocipher.File(fileName);
        file.exists();

        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);

        if (fileExtension.equals("mp4") || mimeType.startsWith("video")) {
            Intent intent = new Intent(CameraActivity.this, MjpegViewerActivity.class);
            intent.setType(mimeType);
            intent.putExtra("video", file.getAbsolutePath());
            startActivity(intent);
        }


        /*
        java.io.File fileOut = new java.io.File("/sdcard/test.pcm");

        try {
            InputStream in = new FileInputStream(fileIn);
            OutputStream out = new java.io.FileOutputStream(fileOut);

            // Transfer bytes from in to out
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("Copying", "Failed at copying file to sdcard");
        }
        */

    }

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
    protected void onPause(){
        super.onPause();
        timeout();
    }

    private void timeout() {
        final Globals appState = (Globals) getApplicationContext();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!isAppOnForeground(getApplicationContext())) {
                    Log.d("CameraActivity", "Timeout reached. Closing down application.");
                    appState.getSecurestore().destroyKey();
                    finish();
                }
            }
        }, appState.getTimeout());
    }

    private boolean isAppOnForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
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
                Globals appState = (Globals) getApplicationContext();
                if(appState.getVFS().isMounted()) {
                    appState.getVFS().unmount();
                }

                // THIS IS STUPID
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
}
