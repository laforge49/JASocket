package org.agilewiki.jasocket.echo;

import org.agilewiki.jasocket.server.ServerApplication;
import org.agilewiki.jasocket.server.SocketAcceptor;

public class EchoSocketAcceptor extends SocketAcceptor {
    @Override
    protected ServerApplication createServerOpened() throws Exception {
        Echo echo = new Echo();
        echo.initialize(getMailboxFactory().createAsyncMailbox());
        echo.socketAcceptor = this;
        return echo;
    }
}
