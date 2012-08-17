package org.agilewiki.jasocket.server;

import java.nio.channels.SocketChannel;

public class NullSocketAcceptor extends SocketAcceptor {
    @Override
    public void acceptSocket(SocketChannel socketChannel) {
        RawReader rawReader = new NullRawReader();
        try {
            rawReader.open(socketChannel, maxPacketSize, threadFactory);
        } catch (Exception ex) {
        }
    }
}
