package org.agilewiki.jasocket.jid.echo;

import org.agilewiki.jasocket.server.ServerProtocol;
import org.agilewiki.jasocket.server.SocketAcceptor;

public class JidEchoSocketAcceptor extends SocketAcceptor {
    @Override
    protected ServerProtocol createServerOpened() throws Exception {
        JidEcho jidEcho = new JidEcho();
        jidEcho.initialize(getMailboxFactory().createAsyncMailbox(), this);
        return jidEcho;
    }
}
