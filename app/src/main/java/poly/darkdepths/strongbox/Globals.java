package poly.darkdepths.strongbox;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import info.guardianproject.iocipher.VirtualFileSystem;
import info.guardianproject.iocipher.File;

/**
 * Created by poly on 3/26/15.
 */
public class Globals extends Application {
    // password management globals
    private Security securestore = new Security();

    // IOCipher globals
    VirtualFileSystem vfs = VirtualFileSystem.get();
    private final String dbFile = Environment.getExternalStorageDirectory().getPath()+"/Strongbox/videos.db";

    // SQL Database globals
    private final String SQLdatabaseName = "store.db";
    private SQLiteDatabase SQLdatabase = null;

    private final String TABLE_NAME         = "videos";
    private final String COLUMN_NAME_ID     = "_id";
    private final String COLUMN_NAME_TITLE  = "title";
    private final String COLUMN_NAME_TIME   = "time";
    private final String COLUMN_NAME_LENGTH = "length";
    private final String COLUMN_NAME_IV     = "iv";

    private String databaseInitializer =
            "CREATE TABLE " + TABLE_NAME + "("
            + COLUMN_NAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_NAME_TITLE + " TEXT, "
            + COLUMN_NAME_TIME + " INTEGER, "
            + COLUMN_NAME_LENGTH + " INTEGER, "
            + COLUMN_NAME_IV + " TEXT"
            + ")";

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

    public VirtualFileSystem getVFS() { return vfs; }

    public String getSQLdatabaseName() { return SQLdatabaseName; }

    public SQLiteDatabase getSQLdatabase() { return SQLdatabase; }

    public void setSQLdatabase(SQLiteDatabase db) { this.SQLdatabase = db; }
}
