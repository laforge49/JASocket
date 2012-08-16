package org.agilewiki.jasocket.client;

import org.agilewiki.jactor.lpc.JLPCActor;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SocketChannel;

public class RawClient extends JLPCActor {
    int maxPacketSize;
    SocketChannel socketChannel;

    public void open(InetAddress inetAddress, int port, int maxPacketSize)
            throws Exception {
        this.maxPacketSize = maxPacketSize;
        InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, port);
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(true);
        socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, maxPacketSize);
        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        socketChannel.connect(inetSocketAddress);
    }

    public void close() {
        try {
            socketChannel.close();
        } catch (Exception ex) {
        }
    }
}
