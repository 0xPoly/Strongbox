package poly.darkdepths.strongbox;

import java.io.File;
import net.sqlcipher.database.SQLiteDatabase;
import android.app.Activity;
import android.os.Bundle;

import javax.crypto.SecretKey;

public class DataStore extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);
        SQLiteDatabase.loadLibs(this);
    }

    public DataStore() {
    }

    public boolean  databaseExists() {
        File databaseFile = getDatabasePath("store.db");
        return databaseFile.exists();
    }

    public void InitializeSQLDatabase(SecretKey key) {

        File databaseFile = getDatabasePath("store.db");
        databaseFile.mkdirs();
        databaseFile.delete();

        SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(databaseFile, new String(key.getEncoded()), null);
        database.execSQL("CREATE TABLE videos(Id INTEGER, Name STRING, Iv STRING)");
        database.close();
    }
}
