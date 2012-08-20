package org.agilewiki.jasocket.server;

import org.agilewiki.jasocket.NullSocketApplication;
import org.agilewiki.jasocket.SocketApplication;

import java.nio.channels.SocketChannel;

public class NullBytesSocketAcceptor extends SocketAcceptor {
    @Override
    public void acceptSocket(SocketChannel socketChannel) {
        try {
            SocketApplication socketApplication = new NullSocketApplication();
            socketApplication.initialize(getMailboxFactory().createAsyncMailbox());
            socketApplication.serverOpen(socketChannel, maxPacketSize, threadFactory);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
