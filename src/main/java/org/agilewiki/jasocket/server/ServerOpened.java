package org.agilewiki.jasocket.server;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ThreadFactory;

public interface ServerOpened {
    public void serverOpen(SocketChannel socketChannel, int maxPacketSize, ThreadFactory threadFactory)
            throws Exception;
}
