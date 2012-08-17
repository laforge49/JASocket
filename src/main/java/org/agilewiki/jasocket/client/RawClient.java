package org.agilewiki.jasocket.client;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class RawClient extends JLPCActor {
    int maxPacketSize;
    SocketChannel socketChannel;

    public void open(InetSocketAddress inetSocketAddress, int maxPacketSize)
            throws Exception {
        this.maxPacketSize = maxPacketSize;
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(true);
        socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, maxPacketSize);
        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        socketChannel.connect(inetSocketAddress);
    }

    void processByteBuffer(ByteBuffer byteBuffer) {

    }

    public void close() {
        try {
            socketChannel.close();
        } catch (Exception ex) {
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
