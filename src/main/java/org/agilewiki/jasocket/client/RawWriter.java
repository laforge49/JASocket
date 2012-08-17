package org.agilewiki.jasocket.client;

import org.agilewiki.jactor.lpc.JLPCActor;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

public class RawWriter extends JLPCActor {
    SocketChannel socketChannel;
    ByteBuffer byteBuffer;

    public void open(InetSocketAddress inetSocketAddress, int maxPacketSize)
            throws Exception {
        byteBuffer = ByteBuffer.allocateDirect(maxPacketSize);
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(true);
        socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, maxPacketSize);
        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        socketChannel.connect(inetSocketAddress);
    }

    public void writeBytes(byte[] bytes) throws Exception {
        int i = 0;
        while (i < bytes.length) {
            int l = bytes.length - i;
            int r = byteBuffer.remaining();
            if (l > r)
                l = r;
            byteBuffer.put(bytes, i, l);
            i += l;
            if (!byteBuffer.hasRemaining())
                write();
        }
        if (byteBuffer.position() > 0 && getMailbox().isEmpty())
            write();
    }

    void write() throws Exception {
        byteBuffer.flip();
        try {
            while (byteBuffer.hasRemaining())
                socketChannel.write(byteBuffer);
        } catch (ClosedChannelException ex) {
        }
        byteBuffer.clear();
    }

    public void close() {
        try {
            socketChannel.close();
        } catch (Exception ex) {
        }
    }
}
