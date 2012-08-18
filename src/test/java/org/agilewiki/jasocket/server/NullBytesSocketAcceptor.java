package org.agilewiki.jasocket.server;

import java.nio.channels.SocketChannel;

public class NullBytesSocketAcceptor extends SocketAcceptor {
    @Override
    public void acceptSocket(SocketChannel socketChannel) {
        try {
            BytesProcessor bytesProcessor = new NullBytesProcessor();
            bytesProcessor.initialize(getMailboxFactory().createAsyncMailbox());
            bytesProcessor.open(socketChannel, maxPacketSize, threadFactory);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
