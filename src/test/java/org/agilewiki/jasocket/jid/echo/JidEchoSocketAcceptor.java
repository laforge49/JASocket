package org.agilewiki.jasocket.jid.echo;

import org.agilewiki.jasocket.server.ServerApplication;
import org.agilewiki.jasocket.server.SocketAcceptor;

public class JidEchoSocketAcceptor extends SocketAcceptor {
    @Override
    protected ServerApplication createServerOpened() throws Exception {
        JidEcho jidEcho = new JidEcho();
        jidEcho.initialize(getMailboxFactory().createAsyncMailbox(), this);
        return jidEcho;
    }
}
