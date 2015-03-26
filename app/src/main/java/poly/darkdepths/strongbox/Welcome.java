package poly.darkdepths.strongbox;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
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

import java.io.File;

/**
 * This activity handles setting up the app for the very first time and setting up a user password.
 */

public class Welcome extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);



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
    public boolean onCreateOptionsMenu(Menu menu){
        //getMenuInflater().inflate(R.menu.menu_main, menu);
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
                Security.storeSalt(getBaseContext());

                Globals   appState    = (Globals) getApplicationContext();
                Security  securestore = appState.getSecurestore();
                securestore.generateKey(password.toCharArray(), Security.getSalt(this.getBaseContext()));

                SQLiteDatabase.loadLibs(this);
                File databaseFile = getDatabasePath("store.db");
                databaseFile.mkdirs();
                databaseFile.delete();

                SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(databaseFile, new String(securestore.getKey().getEncoded()), null);
                database.execSQL("CREATE TABLE videos(Id INTEGER, Name STRING, Iv STRING)");
                database.close();

                Intent intent = new Intent(Welcome.this, MainActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                TextView warning = (TextView) findViewById(R.id.match_warn);
                warning.setText("Error setting up database");
                e.printStackTrace();
            }
        }
    }
}
