package poly.darkdepths.strongbox;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;
import java.io.InputStream;
import java.text.DateFormat;

import info.guardianproject.iocipher.FileInputStream;
import info.guardianproject.iocipher.VirtualFileSystem;
import poly.darkdepths.strongbox.player.MjpegViewerActivity;

/**
 * The right fragment, containing the gallery previews and access to settings.
 */
public class GalleryFragment extends Fragment {
    ListView listview;
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private Cursor cursor;
    private VideoServer videoServer = null;

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
        if (!appState.getVFS().isMounted())
            appState.getVFS().mount(securestore.getKey().getEncoded());

        // load encrypted SQLlite database
        SQLiteDatabase.loadLibs(getActivity());
        File databaseFile = getActivity().getDatabasePath(appState.getDatabaseName());

        // attempt to open database, this will fail if password is wrong
        // or if database is otherwise corrupted
        SQLiteDatabase database = SQLiteDatabase.openDatabase(
                databaseFile.getPath(),
                new String(securestore.getKey().getEncoded()), null, SQLiteDatabase.OPEN_READONLY);

        cursor = database.rawQuery("SELECT  * FROM " + appState.getTableName(), null);

        ListView listView = (ListView) view.findViewById(R.id.listView);
        listview = listView;
        TodoCursorAdapter adapter = new TodoCursorAdapter(getActivity(), cursor, getActivity());
        listView.setAdapter(adapter);

        if (cursor.getCount() != 0) {
            TextView textView = (TextView) view.findViewById(R.id.lonelyView);
            textView.setText("");
        }

        database.close();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView tvBody = (TextView) view.findViewById(R.id.tvBody);
                String filename = tvBody.getText().toString() + ".mov";
                info.guardianproject.iocipher.File file = new info.guardianproject.iocipher.File(filename);
                Log.d("GalleryFragment", "Attempting to play " + file.getAbsolutePath());

                String fileExtension = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);

                if (fileExtension.equals("mp4") || mimeType.startsWith("video")) {
                    Intent intent = new Intent(getActivity(), MjpegViewerActivity.class);
                    intent.setType(mimeType);
                    intent.putExtra("video", file.getAbsolutePath());
                    startActivity(intent);
                }
            }
        });

        return view;
    }


    @Override
    public void onDestroy(){
        super.onDestroy();
        cursor.close();
    }
}

/**
 * Adapter for the Video ListView
 */
class TodoCursorAdapter extends CursorAdapter {
    private VideoServer videoserver;
    private Context context;
    private Activity activity;

    public TodoCursorAdapter(Context context, Cursor cursor, Activity activity) {
        super(context, cursor, 0);
        this.context = context;
        this.activity = activity;
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
    public void bindView(final View view, final Context context, Cursor cursor) {
        // Find fields to populate in inflated template
        final TextView tvBody = (TextView) view.findViewById(R.id.tvBody);
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

        // setup share button callback
        ImageButton button = (ImageButton) view.findViewById(R.id.shareButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String filename = tvBody.getText().toString() + ".mov";
                info.guardianproject.iocipher.File file = new info.guardianproject.iocipher.File(filename);
                try {
                    videoserver = new VideoServer(filename, new FileInputStream(file));
                    videoserver.start();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("VideoSever", "Does file exist?");
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                // Add the buttons
                WifiManager wm = (WifiManager) view.getContext().getSystemService(Context.WIFI_SERVICE);
                String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

                String message = "To share this file with a computer, open a web browser and go to:\nhttp://" + ip +":8080";
                builder.setMessage(message);

                builder.setTitle("HTTP Server");

                builder.setPositiveButton("Stop Sharing", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        videoserver.stop();
                        videoserver = null;
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        ImageButton deleteButton = (ImageButton) view.findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String filename = tvBody.getText().toString();

                String vfsFileName = tvBody.getText().toString() + ".mov";
                info.guardianproject.iocipher.File file = new info.guardianproject.iocipher.File(vfsFileName);
                file.delete();

                Globals appState = (Globals) context.getApplicationContext();
                Security securestore = appState.getSecurestore();

                // load encrypted SQLlite database
                SQLiteDatabase.loadLibs(context);
                File databaseFile = context.getDatabasePath(appState.getDatabaseName());

                // attempt to open database, this will fail if password is wrong
                // or if database is otherwise corrupted
                SQLiteDatabase database = SQLiteDatabase.openDatabase(
                        databaseFile.getPath(),
                        new String(securestore.getKey().getEncoded()), null, 0);

                DataStore.deleteVideo(appState, database, filename);

                Cursor cursor = database.rawQuery("SELECT  * FROM " + appState.getTableName(), null);

                ListView listView = (ListView) activity.findViewById(R.id.listView);
                TodoCursorAdapter adapter = new TodoCursorAdapter(activity,
                        cursor, activity);

                listView.setAdapter(adapter);
                listView.invalidateViews();

                TextView textView = (TextView) activity.findViewById(R.id.lonelyView);
                if (cursor.getCount() != 0) {
                    textView.setText("");
                } else {
                    textView.setText("No Recorded Videos");
                }

                database.close();
            }
        });
    }
}
