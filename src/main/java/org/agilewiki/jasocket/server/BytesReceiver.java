package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.concurrent.JAThreadFactory;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jasocket.client.WriteBytes;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ThreadFactory;

abstract public class BytesReceiver extends JLPCActor implements ExceptionProcessor {
    BytesSocket bytesSocket;

    abstract public void receiveBytes(byte[] bytes) throws Exception;

    public void writeBytes(byte[] bytes) throws Exception {
        (new WriteBytes(bytes)).sendEvent(this, bytesSocket);
    }

    public void clientOpen(InetSocketAddress inetSocketAddress, int maxPacketSize)
            throws Exception {
        clientOpen(inetSocketAddress, maxPacketSize, new JAThreadFactory());
    }

    public void clientOpen(InetSocketAddress inetSocketAddress, int maxPacketSize, ThreadFactory threadFactory)
            throws Exception {
        bytesSocket = new BytesSocket();
        bytesSocket.setBytesReceiver(this);
        bytesSocket.initialize(getMailboxFactory().createAsyncMailbox());
        bytesSocket.clientOpen(inetSocketAddress, maxPacketSize, threadFactory);
    }

    public void serverOpen(SocketChannel socketChannel, int maxPacketSize, ThreadFactory threadFactory)
            throws Exception {
        bytesSocket = new BytesSocket();
        bytesSocket.setBytesReceiver(this);
        bytesSocket.initialize(getMailboxFactory().createAsyncMailbox());
        bytesSocket.serverOpen(socketChannel, maxPacketSize, threadFactory);
    }
}
