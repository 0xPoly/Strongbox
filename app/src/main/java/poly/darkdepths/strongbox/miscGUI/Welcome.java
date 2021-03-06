package poly.darkdepths.strongbox.miscGUI;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import net.sqlcipher.database.SQLiteDatabase;

import info.guardianproject.iocipher.VirtualFileSystem;
import poly.darkdepths.strongbox.Globals;
import poly.darkdepths.strongbox.R;
import poly.darkdepths.strongbox.Security;

/**
 * This activity handles setting up the app for the very first time and setting up a user password.
 * Creates IOCipher and SQLCipher containers, as well as generating a salt.
 */

public class Welcome extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);

        Security.storeSalt(getBaseContext());

        EditText passwordField = (EditText) findViewById(R.id.password_prompt);
        EditText repeatField = (EditText) findViewById(R.id.repeat_prompt);

        passwordField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextView warning = (TextView) findViewById(R.id.match_warn);
                warning.setText("");

                Button createButton = (Button) findViewById(R.id.create_button);
                createButton.setEnabled(true);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        repeatField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextView warning = (TextView) findViewById(R.id.match_warn);
                warning.setText("");

                Button createButton = (Button) findViewById(R.id.create_button);
                createButton.setEnabled(true);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void initialize(View view) {
        EditText passwordField = (EditText) findViewById(R.id.password_prompt);
        String password = passwordField.getText().toString();

        EditText repeatField = (EditText) findViewById(R.id.repeat_prompt);
        String repeat = repeatField.getText().toString();

        if (!password.equals(repeat)) {
            TextView warning = (TextView) findViewById(R.id.match_warn);
            warning.setText("Passwords do not match");

            Button createButton = (Button) findViewById(R.id.create_button);
            createButton.setEnabled(false);
        } else {
            try {
                Globals appState    = (Globals) getApplicationContext();
                Security  securestore = appState.getSecurestore();
                securestore.generateKey(password.toCharArray(), Security.getSalt(this.getBaseContext()));

                SQLiteDatabase.loadLibs(this);
                java.io.File SQLdbFile = getDatabasePath(appState.getDatabaseName());
                SQLdbFile.mkdirs();
                SQLdbFile.delete();

                SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(SQLdbFile,
                        new String(securestore.getKey().getEncoded()), null);
                database.execSQL(appState.getDatabaseInitializer());
                database.close();

                java.io.File file = new java.io.File(appState.getDbFile());
                file.mkdirs();
                file.delete();

                VirtualFileSystem vfs = appState.getVFS();
                vfs.createNewContainer(appState.getDbFile(), securestore.getKey().getEncoded());

                finish();
            } catch (Exception e) {
                TextView warning = (TextView) findViewById(R.id.match_warn);
                warning.setText("Error setting up database");
                e.printStackTrace();
            }
        }
    }
}
