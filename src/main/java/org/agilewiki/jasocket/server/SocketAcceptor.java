package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.lpc.JLPCActor;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class SocketAcceptor extends JLPCActor {
    int maxPacketSize;
    ServerSocketChannel serverSocketChannel;
    Thread thread;

    public void open(InetAddress inetAddress, int port, int maxPacketSize) throws Exception {
        this.maxPacketSize = maxPacketSize;
        InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, port);
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, maxPacketSize);
        serverSocketChannel.bind(inetSocketAddress);
        thread = new Thread(new Acceptor());
        thread.start();
    }

    public void close() {
        thread.interrupt();
        try {
            serverSocketChannel.close();
        } catch (Exception ex) {
        }
    }

    class Acceptor implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
                    socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
                    socketChannel.close();
                }
            } catch (Exception ex) {}
        }
    }
}
