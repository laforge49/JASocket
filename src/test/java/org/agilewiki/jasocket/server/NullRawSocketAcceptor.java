package org.agilewiki.jasocket.server;

import org.agilewiki.jasocket.NullRawSocket;
import org.agilewiki.jasocket.RawSocket;

import java.nio.channels.SocketChannel;

public class NullRawSocketAcceptor extends SocketAcceptor {
    @Override
    protected ServerOpened createServerOpened() throws Exception {
        RawSocket rawSocket = new NullRawSocket();
        rawSocket.initialize(getMailboxFactory().createAsyncMailbox());
        return rawSocket;
    }
}
