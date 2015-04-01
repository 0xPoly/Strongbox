package poly.darkdepths.strongbox;

import android.content.ContentValues;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by poly on 3/27/15.
 */
public class DataStore {
    public static void storeVideo(Globals appState, SQLiteDatabase database, Video video){
        ContentValues values = new ContentValues();

        values.put(appState.getCOLUMN_NAME_TITLE(), video.getTitle());
        values.put(appState.getCOLUMN_NAME_LENGTH(), video.getLength());
        values.put(appState.getCOLUMN_NAME_TIME(), video.getDate());
        values.put(appState.getCOLUMN_NAME_IV(), video.getIV());

        database.insert(appState.getTableName(), null, values);
    }

    public static List<Video> getAllVideos(Globals appState, SQLiteDatabase database) {
        List<Video> videoList = new ArrayList<Video>();

        Cursor cursor = database.rawQuery("SELECT  * FROM " + appState.getTableName(), null);
        if (cursor.moveToFirst()) {
            do {
                Video video = new Video();
                video.setTitle(cursor.getString(1));
                video.setDate(cursor.getLong(2));
                video.setLength(cursor.getLong(3));
                video.setIV(cursor.getString(4));
                // Adding video to list
                videoList.add(video);
            } while (cursor.moveToNext());
        }
        return  videoList;
    }
}

class Video {
    private String Title;
    private Long Length;
    private Long Date;     // 2038 awaits ;)
    private String IV;

    public Video(){

    }

    public Video(String title, Long length) {
        this.Title = title;
        this.Length = length;

        Date date = new java.util.Date();
        setDate(date.getTime());

        this.IV = Arrays.toString(Security.generateIV());
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

    public String getIV() {
        return IV;
    }

    public void setIV(String iv) {
        this.IV = iv;
    }
}
