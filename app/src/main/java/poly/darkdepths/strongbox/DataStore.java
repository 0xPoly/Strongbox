package poly.darkdepths.strongbox;

import android.content.ContentValues;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Handles Video objects and storing them into the encrypted SQL database
 */
public class DataStore {
    public static void storeVideo(Globals appState, SQLiteDatabase database, Video video){
        ContentValues values = new ContentValues();

        values.put(appState.getCOLUMN_NAME_TITLE(), video.getTitle());
        values.put(appState.getCOLUMN_NAME_LENGTH(), video.getLength());
        values.put(appState.getCOLUMN_NAME_TIME(), video.getDate());

        database.insert(appState.getTableName(), null, values);
    }
}

class Video {
    private String Title;
    private Long Length;
    private Long Date;

    public Video(String title, Long length) {
        this.Title = title;
        this.Length = length;

        Date date = new java.util.Date();
        setDate(date.getTime());
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public Long getLength() {
        return Length;
    }

    public void setLength(Long length) {
        Length = length;
    }

    public Long getDate() {
        return Date;
    }

    public void setDate(Long date) {
        Date = date;
    }

}
