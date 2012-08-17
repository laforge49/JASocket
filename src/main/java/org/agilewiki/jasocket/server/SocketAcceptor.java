package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.concurrent.JAThreadFactory;
import org.agilewiki.jactor.lpc.JLPCActor;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

abstract public class SocketAcceptor extends JLPCActor {
    int maxPacketSize;
    ServerSocketChannel serverSocketChannel;
    Thread thread;

    public void open(InetSocketAddress inetSocketAddress, int maxPacketSize)
            throws Exception {
        open(inetSocketAddress, maxPacketSize, new JAThreadFactory());
    }

    public void open(InetSocketAddress inetSocketAddress,
                     int maxPacketSize,
                     JAThreadFactory threadFactory)
            throws Exception {
        this.maxPacketSize = maxPacketSize;
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, maxPacketSize);
        serverSocketChannel.bind(inetSocketAddress);
        thread = threadFactory.newThread(new Acceptor());
        thread.start();
    }

    abstract public void acceptSocket(SocketChannel socketChannel);

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
                    (new AcceptSocket(socketChannel)).sendEvent(SocketAcceptor.this);
                }
            } catch (ClosedByInterruptException cbie) {
            } catch (ClosedChannelException cce) {
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(1);
            }
        }
    }
}
