package poly.darkdepths.strongbox.miscGUI;

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

import info.guardianproject.iocipher.VirtualFileSystem;
import poly.darkdepths.strongbox.Globals;
import poly.darkdepths.strongbox.MainActivity;
import poly.darkdepths.strongbox.R;
import poly.darkdepths.strongbox.Security;

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
            // get Globals
            Globals appState = (Globals) getApplicationContext();
            Security securestore = appState.getSecurestore();
            securestore.generateKey(password.toCharArray(),
                    Security.getSalt(this.getBaseContext()));

            /*
            // load encrypted SQLlite database
            // TODO get rid of sqlcipher, use SQLITE within VFS
            SQLiteDatabase.loadLibs(this);
            File databaseFile = getDatabasePath(appState.getDatabaseName());

            // attempt to open database, this will fail if password is wrong
            // or if database is otherwise corrupted
            SQLiteDatabase database = SQLiteDatabase.openDatabase(
                    databaseFile.getPath(),
                    new String(securestore.getKey().getEncoded()), null, 0);

            // error returned if database isn't closed correctly
            database.close();
            */

            // attempt to open virtual file system
            VirtualFileSystem vfs = appState.getVFS();
            vfs.mount(appState.getDbFile(), securestore.getKey().getEncoded());


            // load up SQLDatabase
            info.guardianproject.iocipher.File SQL = new info.guardianproject.iocipher.File(appState.getSQLdatabaseName());
            SQL.mkdirs();
            SQL.delete();

            android.database.sqlite.SQLiteDatabase database = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(SQL, null);
            database.close();

            appState.setSQLdatabase(database);

            finish();
        } catch (Exception e) {
            e.printStackTrace();
            TextView warning = (TextView) findViewById(R.id.pass_warn);
            warning.setText("Wrong password or corrupted database");
            Button createButton = (Button) findViewById(R.id.unlock_button);
            createButton.setEnabled(false);
        }
    }
}
