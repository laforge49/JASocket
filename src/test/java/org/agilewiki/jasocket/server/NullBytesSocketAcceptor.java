package org.agilewiki.jasocket.server;

import org.agilewiki.jasocket.NullSocketApplication;

public class NullBytesSocketAcceptor extends SocketAcceptor {
    @Override
    protected ServerOpened createServerOpened() throws Exception {
        NullSocketApplication nullSocketApplication = new NullSocketApplication();
        nullSocketApplication.initialize(getMailboxFactory().createAsyncMailbox());
        return nullSocketApplication;
    }
}
