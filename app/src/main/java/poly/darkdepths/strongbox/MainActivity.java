package poly.darkdepths.strongbox;

import android.app.Activity;
import android.content.Intent;

import java.io.File;

import poly.darkdepths.strongbox.miscGUI.Unlock;
import poly.darkdepths.strongbox.miscGUI.Welcome;

/**
 * Entry point for app
 */

public class MainActivity extends Activity{
    @Override
    protected void onResume(){
        super.onResume();

        Globals   appState    = (Globals) getApplicationContext();
        Security  securestore = appState.getSecurestore();

        Boolean SQLfileExists = getDatabasePath(appState.getDatabaseName()).exists();
        Boolean IOCdbExists = new File(appState.getDbFile()).exists();
        Boolean saltExists = Security.getSalt(getApplicationContext()) != null;

        if (!(SQLfileExists && saltExists && IOCdbExists)) {
            // first time running app
            Intent intent = new Intent(MainActivity.this, Welcome.class);
            startActivity(intent);
        } else if (securestore.getKey() == null) {
            // key not in memory, prompt user
            Intent intent = new Intent(MainActivity.this, Unlock.class);
            startActivity(intent);
        } else {
            // all good, start recording
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(intent);
        }
    }
}

