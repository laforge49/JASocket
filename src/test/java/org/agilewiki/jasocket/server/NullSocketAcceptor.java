package org.agilewiki.jasocket.server;

import java.nio.channels.SocketChannel;

public class NullSocketAcceptor extends SocketAcceptor {
    @Override
    public void acceptSocket(SocketChannel socketChannel) {
        try {
            socketChannel.close();
        } catch (Exception ex) {}
    }
}
