package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.lpc.JLPCActor;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ThreadFactory;

abstract public class BytesProcessor extends JLPCActor {
    abstract public void processBytes(byte[] bytes) throws Exception;

    public void open(SocketChannel socketChannel, int maxPacketSize, ThreadFactory threadFactory)
            throws Exception {
        BytesReader bytesReader = new BytesReader();
        bytesReader.bytesProcessor = this;
        bytesReader.initialize(getMailboxFactory().createAsyncMailbox());
        bytesReader.open(socketChannel, maxPacketSize, threadFactory);
    }
}
