package org.agilewiki.jasocket.server;

import java.nio.channels.SocketChannel;

public class NullBytesSocketAcceptor extends SocketAcceptor {
    @Override
    public void acceptSocket(SocketChannel socketChannel) {
        try {
            BytesReceiver bytesReceiver = new NullBytesReceiver();
            bytesReceiver.initialize(getMailboxFactory().createAsyncMailbox());
            bytesReceiver.serverOpen(socketChannel, maxPacketSize, threadFactory);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
