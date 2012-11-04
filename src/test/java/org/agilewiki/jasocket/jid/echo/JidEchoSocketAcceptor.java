package org.agilewiki.jasocket.jid.echo;

import org.agilewiki.jasocket.BytesProtocol;
import org.agilewiki.jasocket.server.SocketAcceptor;

public class JidEchoSocketAcceptor extends SocketAcceptor {
    @Override
    protected BytesProtocol createServerOpened() throws Exception {
        JidEcho jidEcho = new JidEcho();
        jidEcho.initialize(getMailboxFactory().createAsyncMailbox(), this);
        return jidEcho;
    }
}
