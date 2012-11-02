package org.agilewiki.jasocket;

import org.agilewiki.jasocket.server.SocketAcceptor;

public class NullBytesApplication extends BytesApplication {
    public SocketAcceptor socketAcceptor;

    @Override
    public void receiveBytes(byte[] bytes) throws Exception {
        System.out.println("received " + bytes.length + " bytes");
        if (bytes[bytes.length - 1] == '!') {
            socketAcceptor.close();
            getMailboxFactory().close();
        }
    }

    @Override
    public void processException(Exception exception) {
        exception.printStackTrace();
    }

    @Override
    protected void closed() {
    }
}
