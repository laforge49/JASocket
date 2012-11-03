package org.agilewiki.jasocket.server;

import org.agilewiki.jasocket.NullBytesProtocol;

public class NullBytesSocketAcceptor extends SocketAcceptor {
    @Override
    protected ServerApplication createServerOpened() throws Exception {
        NullBytesProtocol nullSocketApplication = new NullBytesProtocol();
        nullSocketApplication.initialize(getMailboxFactory().createAsyncMailbox());
        nullSocketApplication.socketAcceptor = this;
        return nullSocketApplication;
    }
}
