package poly.darkdepths.strongbox;

import android.app.Application;

/**
 * Created by poly on 3/26/15.
 */
public class Globals extends Application {
    private Security securestore = new Security();

    public Security getSecurestore() {

        return this.securestore;
    }
}
