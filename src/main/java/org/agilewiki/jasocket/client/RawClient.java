package org.agilewiki.jasocket.client;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.concurrent.JAThreadFactory;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ThreadFactory;

abstract public class RawClient extends JLPCActor {
    int maxPacketSize;
    SocketChannel socketChannel;
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
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(true);
        socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, maxPacketSize);
        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        socketChannel.connect(inetSocketAddress);
        thread = threadFactory.newThread(new Reader());
        thread.start();
    }

    abstract void processByteBuffer(ByteBuffer byteBuffer);

    abstract void processException(Exception exception);

    public void close() {
        try {
            socketChannel.close();
        } catch (Exception ex) {
        }
    }

    class Reader implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(maxPacketSize);
                    socketChannel.read(byteBuffer);
                    (new ProcessByteBuffer(byteBuffer)).sendEvent(RawClient.this);
                }
            } catch (ClosedChannelException cce) {
            } catch (Exception ex) {
            }
        }
    }
}

class ProcessByteBuffer extends Request<Object, RawClient> {
    ByteBuffer byteBuffer;

    public ProcessByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public boolean isTargetType(Actor targetActor) {
        return targetActor instanceof RawClient;
    }

    @Override
    public void processRequest(JLPCActor targetActor, RP rp) throws Exception {
        ((RawClient) targetActor).processByteBuffer(byteBuffer);
        rp.processResponse(null);
    }
}

class ProcessException extends Request<Object, RawClient> {
    Exception exception;

    public ProcessException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public boolean isTargetType(Actor targetActor) {
        return targetActor instanceof RawClient;
    }

    @Override
    public void processRequest(JLPCActor targetActor, RP rp) throws Exception {
        ((RawClient) targetActor).processException(exception);
        rp.processResponse(null);
    }
}
