package org.agilewiki.jasocket.server;

import java.nio.channels.SocketChannel;

public class NullRawSocketAcceptor extends SocketAcceptor {
    @Override
    public void acceptSocket(SocketChannel socketChannel) {
        RawSocket rawSocket = new NullRawSocket();
        try {
            rawSocket.initialize(getMailboxFactory().createAsyncMailbox());
            rawSocket.serverOpen(socketChannel, maxPacketSize, threadFactory);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
