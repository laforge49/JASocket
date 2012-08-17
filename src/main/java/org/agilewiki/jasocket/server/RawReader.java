package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ThreadFactory;

abstract public class RawReader extends JLPCActor {
    int maxPacketSize;
    SocketChannel socketChannel;
    Thread thread;

    public void open(SocketChannel socketChannel, int maxPacketSize, ThreadFactory threadFactory)
            throws Exception {
        this.socketChannel = socketChannel;
        this.maxPacketSize = maxPacketSize;
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
                    int i = socketChannel.read(byteBuffer);
                    if (i == -1)
                        return;
                    byteBuffer.flip();
                    (new ProcessByteBuffer(byteBuffer)).sendEvent(RawReader.this);
                }
            } catch (ClosedChannelException cce) {
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}

class ProcessByteBuffer extends Request<Object, RawReader> {
    ByteBuffer byteBuffer;

    public ProcessByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public boolean isTargetType(Actor targetActor) {
        return targetActor instanceof RawReader;
    }

    @Override
    public void processRequest(JLPCActor targetActor, RP rp) throws Exception {
        ((RawReader) targetActor).processByteBuffer(byteBuffer);
        rp.processResponse(null);
    }
}

class ProcessException extends Request<Object, RawReader> {
    Exception exception;

    public ProcessException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public boolean isTargetType(Actor targetActor) {
        return targetActor instanceof RawReader;
    }

    @Override
    public void processRequest(JLPCActor targetActor, RP rp) throws Exception {
        ((RawReader) targetActor).processException(exception);
        rp.processResponse(null);
    }
}
