package org.agilewiki.jasocket;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.concurrent.JAThreadFactory;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;
import org.agilewiki.jasocket.server.ServerOpened;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ThreadFactory;

abstract public class RawSocket extends SocketWriter implements ServerOpened {
    int maxPacketSize;
    Thread readerThread;

    @Override
    public void clientOpen(InetSocketAddress inetSocketAddress, int maxPacketSize)
            throws Exception {
        clientOpen(inetSocketAddress, maxPacketSize, new JAThreadFactory());
    }

    public void clientOpen(InetSocketAddress inetSocketAddress, int maxPacketSize, ThreadFactory threadFactory)
            throws Exception {
        super.clientOpen(inetSocketAddress, maxPacketSize);
        this.maxPacketSize = maxPacketSize;
        readerThread = threadFactory.newThread(new Reader());
        readerThread.start();
    }

    @Override
    public void serverOpen(SocketChannel socketChannel, int maxPacketSize, ThreadFactory threadFactory)
            throws Exception {
        writeBuffer = ByteBuffer.allocateDirect(maxPacketSize);
        this.socketChannel = socketChannel;
        this.maxPacketSize = maxPacketSize;
        readerThread = threadFactory.newThread(new Reader());
        readerThread.start();
    }

    protected abstract void receiveByteBuffer(ByteBuffer byteBuffer) throws Exception;

    class Reader implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(maxPacketSize);
                    int i = socketChannel.read(byteBuffer);
                    if (i == -1)
                        return;
                    byteBuffer.flip();
                    (new ReceiveByteBuffer(byteBuffer)).sendEvent(RawSocket.this);
                }
            } catch (ClosedChannelException cce) {
            } catch (Exception ex) {
                try {
                    (new ProcessException(ex)).sendEvent(exceptionProcessor);
                } catch (Exception x) {
                    x.printStackTrace();
                }
            }
        }
    }
}

class ReceiveByteBuffer extends Request<Object, RawSocket> {
    ByteBuffer byteBuffer;

    public ReceiveByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public boolean isTargetType(Actor targetActor) {
        return targetActor instanceof RawSocket;
    }

    @Override
    public void processRequest(JLPCActor targetActor, RP rp) throws Exception {
        ((RawSocket) targetActor).receiveByteBuffer(byteBuffer);
        rp.processResponse(null);
    }
}
