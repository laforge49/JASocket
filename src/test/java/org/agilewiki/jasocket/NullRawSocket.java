package org.agilewiki.jasocket;

import org.agilewiki.jasocket.server.SocketAcceptor;

import java.nio.ByteBuffer;

public class NullRawSocket extends RawSocket {
    public SocketAcceptor socketAcceptor;
    public int deadIn;

    @Override
    protected void receiveByteBuffer(ByteBuffer byteBuffer) {
        int l = byteBuffer.remaining();
        System.out.println("read " + l + " bytes");
        deadIn -= l;
        if (deadIn == 0) {
            socketAcceptor.close();
            getMailboxFactory().close();
        }
    }

    @Override
    public void processException(Exception exception) {
        exception.printStackTrace();
    }
}
