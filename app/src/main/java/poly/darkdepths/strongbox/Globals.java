package poly.darkdepths.strongbox;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import info.guardianproject.iocipher.VirtualFileSystem;

/**
 * Created by poly on 3/26/15.
 */
public class Globals extends Application {
    private Security securestore = new Security();

    VirtualFileSystem vfs = VirtualFileSystem.get();
    private final String dbFile = Environment.getExternalStorageDirectory().getPath()+"/Strongbox/videos.db";
    private final String dbDir = Environment.getExternalStorageDirectory().getPath()+"/Strongbox/";

    private final String TABLE_NAME         = "videos";
    private final String COLUMN_NAME_ID     = "_id";
    private final String COLUMN_NAME_TITLE  = "title";
    private final String COLUMN_NAME_TIME   = "time";
    private final String COLUMN_NAME_LENGTH = "length";
    private final String COLUMN_NAME_IV     = "iv";

    private SQLiteDatabase SQLdatabase = null;
    private final String SQLdatabaseName = "store.db";
    private String databaseInitializer =
            "CREATE TABLE " + TABLE_NAME + "("
            + COLUMN_NAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_NAME_TITLE + " TEXT, "
            + COLUMN_NAME_TIME + " INTEGER, "
            + COLUMN_NAME_LENGTH + " INTEGER, "
            + COLUMN_NAME_IV + " TEXT"
            + ")";

    private int timeout = 300000; // 5 minutes by default

    public Security getSecurestore() {
        return this.securestore;
    }

    public String getTableName() {
        return this.TABLE_NAME;
    }

    public String getDatabaseName() {
        return this.SQLdatabaseName;
    }

    public String getDatabaseInitializer(){
        return this.databaseInitializer;
    }

    public String getCOLUMN_NAME_ID() {
        return COLUMN_NAME_ID;
    }

    public String getCOLUMN_NAME_TITLE() {
        return COLUMN_NAME_TITLE;
    }

    public String getCOLUMN_NAME_TIME() {
        return COLUMN_NAME_TIME;
    }

    public String getCOLUMN_NAME_LENGTH() {
        return COLUMN_NAME_LENGTH;
    }

    public String getCOLUMN_NAME_IV() {
        return COLUMN_NAME_IV;
    }

    public String getDbFile() { return dbFile; }

    public String getDbDir() { return  dbDir; }

    public VirtualFileSystem getVFS() { return vfs; }

    public int getTimeout() { return timeout; }
}
