package org.agilewiki.jasocket.server;

import java.nio.ByteBuffer;

public class NullRawReader extends RawReader {
    @Override
    void processByteBuffer(ByteBuffer byteBuffer) {
        System.out.println("read " + byteBuffer.remaining() + " bytes");
    }

    @Override
    void processException(Exception exception) {
        exception.printStackTrace();
    }
}
