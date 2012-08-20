package org.agilewiki.jasocket.server;

import java.nio.ByteBuffer;

public class NullRawSocket extends RawSocket {
    @Override
    void receiveByteBuffer(ByteBuffer byteBuffer) {
        System.out.println("read " + byteBuffer.remaining() + " bytes");
    }

    @Override
    public void processException(Exception exception) {
        exception.printStackTrace();
    }
}
