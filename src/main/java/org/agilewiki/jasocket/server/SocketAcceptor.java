package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.concurrent.JAThreadFactory;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ThreadFactory;

abstract public class SocketAcceptor extends JLPCActor {
    int maxPacketSize;
    ServerSocketChannel serverSocketChannel;
    ThreadFactory threadFactory;
    Thread thread;

    public void open(InetSocketAddress inetSocketAddress, int maxPacketSize)
            throws Exception {
        open(inetSocketAddress, maxPacketSize, new JAThreadFactory());
    }

    public void open(InetSocketAddress inetSocketAddress,
                     int maxPacketSize,
                     ThreadFactory threadFactory)
            throws Exception {
        this.maxPacketSize = maxPacketSize;
        this.threadFactory = threadFactory;
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, maxPacketSize);
        serverSocketChannel.bind(inetSocketAddress);
        thread = threadFactory.newThread(new Acceptor());
        thread.start();
    }

    abstract protected ServerOpened createServerOpened() throws Exception;

    public void acceptSocket(SocketChannel socketChannel) {
        try {
            ServerOpened serverOpened = createServerOpened();
            serverOpened.serverOpen(socketChannel, maxPacketSize, threadFactory);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
                    socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, maxPacketSize);
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

class AcceptSocket extends Request<Object, SocketAcceptor> {
    SocketChannel socketChannel;

    public AcceptSocket(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    @Override
    public boolean isTargetType(Actor targetActor) {
        return targetActor instanceof SocketAcceptor;
    }

    @Override
    public void processRequest(JLPCActor targetActor, RP rp) throws Exception {
        ((SocketAcceptor) targetActor).acceptSocket(socketChannel);
        rp.processResponse(null);
    }
}
