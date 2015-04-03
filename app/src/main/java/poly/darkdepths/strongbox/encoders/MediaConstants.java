package poly.darkdepths.strongbox.encoders;

/**
 * This class is largely based on the IOCipher example (https://github.com/n8fr8/IOCipherCameraExample).
 * @author Nathan of the Guardian Project
 */

import android.media.AudioFormat;
public class MediaConstants {
    public static int sJpegQuality = 50; //70 is the quality!
    public static int sAudioSampleRate = 11025; //keep it low for now
    public static int sAudioChannels = 1;
    public static int sAudioBitRate = 64;
    public static int sChannelConfigIn = AudioFormat.CHANNEL_IN_MONO;
    public static int sChannelConfigOut = AudioFormat.CHANNEL_OUT_MONO;
}
