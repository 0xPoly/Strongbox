package poly.darkdepths.strongbox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

/**
 * Created by poly on 3/8/15.
 */
public class MainActivity extends Activity {
    public Security securestore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //first time running application
        if (Security.getSalt(this.getBaseContext()) == null) {
            Intent intent = new Intent(MainActivity.this, Welcome.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(intent);
        }


        // TODO remove this later
        //setContentView(R.layout.activity_main);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    
    public void openWelcome(View view) {
        Intent intent = new Intent(MainActivity.this, Welcome.class);
        startActivity(intent);
    }

    public void openCamera(View view) {
        Intent intent = new Intent(MainActivity.this, CameraActivity.class);
        startActivity(intent);
    }

    public Security getSecureStore(){
        return this.securestore;
    }
}
