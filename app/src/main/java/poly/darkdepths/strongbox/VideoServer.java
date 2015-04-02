package poly.darkdepths.strongbox;

import android.os.Environment;
import android.util.Log;

import java.io.FileNotFoundException;
import java.util.Map;

import info.guardianproject.iocipher.FileInputStream;

/**
 * Created by poly on 4/2/15.
 */
public class VideoServer extends NanoHTTPD {
    private FileInputStream fis = null;
    private String fileName;

    public VideoServer(String file, FileInputStream fileInputStream){
        super(8080);
        this.fis = fileInputStream;
        this.fileName = file;
    }


    @Override
    public Response serve(String uri, Method method,
                          Map<String, String> header, Map<String, String> parameters,
                          Map<String, String> files) {

        Log.d("Streaming", "Server should be up now");

        Response res = new NanoHTTPD.Response(Response.Status.OK, "video/quicktime", fis);
        res.addHeader("Content-Disposition: attachment; filename=", fileName);

        return res;
    }
}

