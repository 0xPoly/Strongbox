package poly.darkdepths.strongbox;

/**
 * Created by poly on 3/27/15.
 */

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * The right fragment, containing the gallery previews and access to settings.
 */
public class GalleryFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static GalleryFragment newInstance(int sectionNumber) {
        GalleryFragment fragment = new GalleryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public GalleryFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        // get Globals
        Globals appState = (Globals) getActivity().getApplicationContext();
        Security securestore = appState.getSecurestore();

        // load encrypted SQLlite database
        SQLiteDatabase.loadLibs(getActivity());
        File databaseFile = getActivity().getDatabasePath(appState.getDatabaseName());

        // attempt to open database, this will fail if password is wrong
        // or if database is otherwise corrupted
        SQLiteDatabase database = SQLiteDatabase.openDatabase(
                databaseFile.getPath(),
                new String(securestore.getKey().getEncoded()), null, SQLiteDatabase.OPEN_READONLY);

        Cursor cursor = database.rawQuery("SELECT  * FROM " + appState.getTableName(), null);

        TodoCursorAdapter adapter = new TodoCursorAdapter(getActivity().getApplicationContext(), cursor);
        ListView listView = (ListView) view.findViewById(R.id.listView);
        listView.setAdapter(adapter);

        if (cursor.getCount() != 0) {
            TextView textView = (TextView) view.findViewById(R.id.lonelyView);
            textView.setText("");
        }

        //cursor.close();
        database.close();
        return view;
    }
}

class TodoCursorAdapter extends CursorAdapter {

    public TodoCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }

    @Override
    public void bindView(View view, Context context, android.database.Cursor cursor) {
        bindView(view, context, (Cursor)cursor);
    }

    public View newView(Context context, android.database.Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    // The newView method is used to inflate a new view and return it,
    // you don't bind any data to the view at this point.
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    // The bindView method is used to bind all data to a given view
    // such as setting the text on a TextView.
    public void bindView(View view, Context context, Cursor cursor) {
        // Find fields to populate in inflated template
        TextView tvBody = (TextView) view.findViewById(R.id.tvBody);
        TextView tvDate = (TextView) view.findViewById(R.id.tvDate);
        TextView tvLength = (TextView) view.findViewById(R.id.tvLength);

        // Extract properties from cursor
        String body = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        long raw_date = cursor.getLong(cursor.getColumnIndexOrThrow("time"));
        long raw_length = cursor.getLong(cursor.getColumnIndex("length"));

        //formatting
        DateFormat df = DateFormat.getDateTimeInstance();
        String date = df.format(raw_date);
        long hours  = raw_length / 3600;
        long minutes = (raw_length % 3600) / 60;
        long seconds =  raw_length % 60;
        String length = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        // Populate fields with extracted properties
        tvBody.setText(body);
        tvDate.setText(String.valueOf(date));
        tvLength.setText(String.valueOf(length));
    }


}