package org.agilewiki.jasocket.server;

import java.nio.channels.SocketChannel;

public class NullSocketAcceptor extends SocketAcceptor {
    @Override
    public void acceptSocket(SocketChannel socketChannel) {
        RawReader rawReader = new NullRawReader();
        try {
            rawReader.initialize(getMailboxFactory().createAsyncMailbox());
            rawReader.open(socketChannel, maxPacketSize, threadFactory);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
