package org.agilewiki.jasocket.server;

import org.agilewiki.jasocket.NullRawSocket;
import org.agilewiki.jasocket.RawSocket;

public class NullRawSocketAcceptor extends SocketAcceptor {
    public int deadIn;

    @Override
    protected ServerApplication createServerOpened() throws Exception {
        NullRawSocket rawSocket = new NullRawSocket();
        rawSocket.initialize(getMailboxFactory().createAsyncMailbox());
        rawSocket.socketAcceptor = this;
        rawSocket.deadIn = deadIn;
        return rawSocket;
    }
}
