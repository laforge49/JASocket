package org.agilewiki.jasocket.echo;

import org.agilewiki.jasocket.server.ServerProtocol;
import org.agilewiki.jasocket.server.SocketAcceptor;

public class EchoSocketAcceptor extends SocketAcceptor {
    @Override
    protected ServerProtocol createServerOpened() throws Exception {
        Echo echo = new Echo();
        echo.initialize(getMailboxFactory().createAsyncMailbox());
        echo.socketAcceptor = this;
        return echo;
    }
}
