package poly.darkdepths.strongbox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;

/**
 * Created by poly on 3/26/15.
 */
public class Unlock extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.unlock);
        watch();
    }

    private void watch() {
        EditText passwordField = (EditText) findViewById(R.id.password_prompt);
        passwordField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextView warning = (TextView) findViewById(R.id.pass_warn);
                warning.setText("");

                Button createButton = (Button) findViewById(R.id.unlock_button);
                createButton.setEnabled(true);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
    });

}

    public void checkPass(View view){
        EditText passwordField = (EditText) findViewById(R.id.password_prompt);
        String password = passwordField.getText().toString();
        try {
            Globals appState = (Globals) getApplicationContext();
            Security securestore = appState.getSecurestore();
            securestore.generateKey(password.toCharArray(), Security.getSalt(this.getBaseContext()));

            SQLiteDatabase.loadLibs(this);
            File databaseFile = getDatabasePath("store.db");

            SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(databaseFile, new String(securestore.getKey().getEncoded()), null);
            database.close();

            Intent intent = new Intent(Unlock.this, MainActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            TextView warning = (TextView) findViewById(R.id.pass_warn);
            warning.setText("Wrong password or corrupted database");
            Button createButton = (Button) findViewById(R.id.unlock_button);
            createButton.setEnabled(false);
        }
    }
}
