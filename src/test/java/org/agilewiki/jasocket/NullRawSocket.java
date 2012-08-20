package org.agilewiki.jasocket;

import org.agilewiki.jasocket.RawSocket;

import java.nio.ByteBuffer;

public class NullRawSocket extends RawSocket {
    @Override
    protected void receiveByteBuffer(ByteBuffer byteBuffer) {
        System.out.println("read " + byteBuffer.remaining() + " bytes");
    }

    @Override
    public void processException(Exception exception) {
        exception.printStackTrace();
    }
}
