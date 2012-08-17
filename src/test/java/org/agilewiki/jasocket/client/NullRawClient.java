package org.agilewiki.jasocket.client;

import java.nio.ByteBuffer;

public class NullRawClient extends RawClient {
    @Override
    void processByteBuffer(ByteBuffer byteBuffer) {
        System.out.println("read " + byteBuffer.position() + " bytes");
    }

    @Override
    void processException(Exception exception) {
        exception.printStackTrace();
    }
}
