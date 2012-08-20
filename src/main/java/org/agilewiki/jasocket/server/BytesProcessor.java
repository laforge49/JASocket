package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.lpc.JLPCActor;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ThreadFactory;

abstract public class BytesProcessor extends JLPCActor {
    abstract public void processBytes(byte[] bytes) throws Exception;

    public void open(SocketChannel socketChannel, int maxPacketSize, ThreadFactory threadFactory)
            throws Exception {
        BytesSocket bytesSocket = new BytesSocket();
        bytesSocket.bytesProcessor = this;
        bytesSocket.initialize(getMailboxFactory().createAsyncMailbox());
        bytesSocket.serverOpen(socketChannel, maxPacketSize, threadFactory);
    }
}
