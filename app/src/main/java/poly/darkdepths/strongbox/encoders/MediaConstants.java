package poly.darkdepths.strongbox.encoders;

/**
 * Created by poly on 3/28/15.
 */

import android.media.AudioFormat;
public class MediaConstants {
    public static int sJpegQuality = 50; //70 is the quality!
    public static int sPreviewWidth = 720; //defualt width
    public static int sPreviewHeight = 480; //default height "480p"
    public static int sAudioSampleRate = 11025; //keep it low for now
    public static int sAudioChannels = 1;
    public static int sAudioBitRate = 64;
    public static int sChannelConfigIn = AudioFormat.CHANNEL_IN_MONO;
    public static int sChannelConfigOut = AudioFormat.CHANNEL_OUT_MONO;
}
