package org.agilewiki.jasocket.server;

import org.agilewiki.jasocket.NullRawSocket;

public class NullRawSocketAcceptor extends SocketAcceptor {
    public int deadIn;

    @Override
    protected ServerProtocol createServerOpened() throws Exception {
        NullRawSocket rawSocket = new NullRawSocket();
        rawSocket.initialize(getMailboxFactory().createAsyncMailbox());
        rawSocket.socketAcceptor = this;
        rawSocket.deadIn = deadIn;
        return rawSocket;
    }
}
