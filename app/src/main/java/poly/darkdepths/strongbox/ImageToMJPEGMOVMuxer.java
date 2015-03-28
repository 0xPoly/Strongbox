package poly.darkdepths.strongbox;

/**
 * Created by poly on 3/28/15.
 */
import android.media.AudioFormat;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.Rational;
import org.jcodec.common.model.Size;
import org.jcodec.common.model.Unit;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.WebOptimizedMP4Muxer;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.containers.mp4.muxer.PCMMP4MuxerTrack;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 *
 */
public class ImageToMJPEGMOVMuxer {
    private SeekableByteChannel ch;

    private FramesMP4MuxerTrack videoTrack;
    private PCMMP4MuxerTrack audioTrack;

    private int frameNo = 0;
    private WebOptimizedMP4Muxer muxer;
    private Size size;

    private String imageType = "jpeg "; //or "png ";
    private org.jcodec.common.AudioFormat af = null;

    private int timeScale = 20;

    public ImageToMJPEGMOVMuxer(SeekableByteChannel ch, org.jcodec.common.AudioFormat af) throws IOException {
        this.ch = ch;
        this.af = af;

        // Muxer that will store the encoded frames
        muxer = new WebOptimizedMP4Muxer(ch, Brand.MOV, 16000);

        // Add video track to muxer
        videoTrack = muxer.addTrack(TrackType.VIDEO, timeScale);
        //    videoTrack.setTgtChunkDuration(new Rational(2, 1), Unit.SEC);

        if (af != null)
            audioTrack = muxer.addPCMAudioTrack(af);

    }

    public void addFrame(int width, int height, ByteBuffer buff, int timeScaleFPS) throws IOException {
        if (this.size == null) {
            this.size = new Size(width,height);
            videoTrack.addSampleEntry(MP4Muxer.videoSampleEntry(imageType, size, "JCodec"));

            if (af != null)
                audioTrack.addSampleEntry(MP4Muxer.audioSampleEntry(af));
        }

        // Add packet to video track

        videoTrack.addFrame(new MP4Packet(buff, frameNo, timeScaleFPS, 1, frameNo, true, null, frameNo, 0));

        frameNo++;
    }

    public void addAudio (ByteBuffer buffer) throws IOException
    {
        audioTrack.addSamples(buffer);
    }

    public void finish() throws IOException {
        // Push saved SPS/PPS to a special storage in MP4
        //  videoTrack.addSampleEntry(MP4Muxer.videoSampleEntry("png ", size, "JCodec"));
        videoTrack.addSampleEntry(MP4Muxer.videoSampleEntry(imageType, size, "JCodec"));

        // Write MP4 header and finalize recording

        if (af != null)
            audioTrack.addSampleEntry(MP4Muxer.audioSampleEntry(af));

        muxer.writeHeader();
        NIOUtils.closeQuietly(ch);
    }
}
