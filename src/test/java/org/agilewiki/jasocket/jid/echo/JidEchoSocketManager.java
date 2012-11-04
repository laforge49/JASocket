package org.agilewiki.jasocket.jid.echo;

import org.agilewiki.jasocket.BytesProtocol;
import org.agilewiki.jasocket.server.SocketManager;

public class JidEchoSocketManager extends SocketManager {
    @Override
    protected BytesProtocol createServerOpened() throws Exception {
        JidEcho jidEcho = new JidEcho();
        jidEcho.initialize(getMailboxFactory().createAsyncMailbox(), this);
        return jidEcho;
    }
}
