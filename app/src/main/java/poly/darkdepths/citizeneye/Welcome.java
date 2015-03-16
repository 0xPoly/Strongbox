package poly.darkdepths.citizeneye;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by poly on 3/8/15.
 */
public class Welcome extends ActionBarActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);

        Security.writeSalt(this.getBaseContext());

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
                Security passStore = new Security();
                passStore.generateKey(password.toCharArray(), Security.getSalt(this.getBaseContext()));

                byte[] input = "Hello World".getBytes();
                byte[] cipher = Security.encrypt(passStore.getKey(),input);

                String plaintext = new String(Security.decrypt(passStore.getKey(), cipher), "UTF-8");
                Log.v("Strongbox decrypt", plaintext);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
