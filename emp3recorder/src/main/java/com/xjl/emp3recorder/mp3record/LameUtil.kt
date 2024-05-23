package com.xjl.emp3recorder.mp3record

/**
 * 可使用此语句直接生成头文件
 * javac JNIDemo.java -h .
 */
object LameUtil {
    init {
        System.loadLibrary("mp3lame")
    }

    /**
     * Initialize LAME.
     *
     * @param inSamplerate  input sample rate in Hz.
     * @param inChannel     number of channels in input stream.
     * @param outSamplerate output sample rate in Hz.
     * @param outBitrate    brate compression ratio in KHz.
     * @param quality
     *
     *quality=0..9. 0=best (very slow). 9=worst.
     *
     * recommended:
     *
     * 2 near-best quality, not too slow
     *
     * 5 good quality, fast
     * 7 ok quality, really fast
     */
    @JvmStatic
    external fun init(
        inSamplerate: Int, inChannel: Int,
        outSamplerate: Int, outBitrate: Int, quality: Int
    )

    /**
     * Encode buffer to mp3.
     *
     * @param bufferLeft  PCM data for left channel.
     * @param bufferRight PCM data for right channel.
     * @param samples     number of samples per channel.
     * @param mp3buf      result encoded MP3 stream. You must specified
     * "7200 + (1.25 * buffer_l.length)" length array.
     * @return
     *
     *number of bytes output in mp3buf. Can be 0.
     *
     * -1: mp3buf was too small
     *
     * -2: malloc() problem
     *
     * -3: lame_init_params() not called
     * -4: psycho acoustic problems
     */
    @JvmStatic
    external fun encode(
        bufferLeft: ShortArray?, bufferRight: ShortArray?,
        samples: Int, mp3buf: ByteArray?
    ): Int

    /**
     * Flush LAME buffer.
     *
     *
     * REQUIRED:
     * lame_encode_flush will flush the intenal PCM buffers, padding with
     * 0's to make sure the final frame is complete, and then flush
     * the internal MP3 buffers, and thus may return a
     * final few mp3 frames.  'mp3buf' should be at least 7200 bytes long
     * to hold all possible emitted data.
     *
     *
     * will also write id3v1 tags (if any) into the bitstream
     *
     *
     * return code = number of bytes output to mp3buf. Can be 0
     *
     * @param mp3buf result encoded MP3 stream. You must specified at least 7200
     * bytes.
     * @return number of bytes output to mp3buf. Can be 0.
     */
    @JvmStatic
    external fun flush(mp3buf: ByteArray?): Int

    /**
     * Close LAME.
     */
    @JvmStatic
    external fun close()
}
