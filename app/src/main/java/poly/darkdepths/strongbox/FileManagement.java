package poly.darkdepths.strongbox;

import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by poly on 3/23/15.
 */
public class FileManagement {
    static class TransferThread extends Thread {
        InputStream in;
        FileOutputStream out;

        TransferThread(InputStream in, FileOutputStream out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            byte[] buf = new byte[8192];
            int len;
            try {
                while ((len = in.read(buf)) >= 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.flush();
                out.getFD().sync();
                out.close();
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(),
                        "Exception transferring file", e);
            }
        }
    }

    public static FileDescriptor getStreamFd() {
        File dir = new File(Environment.getExternalStorageDirectory().getPath()+"/Strongbox/");
        dir.mkdirs();

        File temp = new File(Environment.getExternalStorageDirectory().getPath()+"/Strongbox/video.mp4");

        ParcelFileDescriptor[] pipe=null;
        try {
            pipe=ParcelFileDescriptor.createPipe();
            new TransferThread(new ParcelFileDescriptor.AutoCloseInputStream(pipe[0]),
                    new FileOutputStream(temp)).start();
        }
        catch (IOException e) {
            Log.e("Strongbox", "Exception opening pipe", e);
        }
        return(pipe[1].getFileDescriptor());
    }
}
