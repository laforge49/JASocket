package org.agilewiki.jasocket.client;

import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.pubsub.publisher.Subscribe;
import org.agilewiki.jactor.pubsub.subscriber.JASubscriber;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SocketChannel;

public class RawClient extends JASubscriber {
    int maxPacketSize;
    SocketChannel socketChannel;

    public void open(String hostName, int port, int maxPacketSize, RP rp)
            throws Exception {
        this.maxPacketSize = maxPacketSize;
        InetSocketAddress inetSocketAddress = new InetSocketAddress(hostName, port);
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(true);
        socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, maxPacketSize);
        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        socketChannel.connect(inetSocketAddress);
        setActorName(socketChannel.getRemoteAddress().toString());
        (new Subscribe(this)).send(this, this, rp);
    }
}
