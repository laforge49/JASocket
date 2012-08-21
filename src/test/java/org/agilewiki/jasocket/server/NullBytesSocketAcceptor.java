package org.agilewiki.jasocket.server;

import org.agilewiki.jasocket.NullBytesApplication;

public class NullBytesSocketAcceptor extends SocketAcceptor {
    @Override
    protected ServerApplication createServerOpened() throws Exception {
        NullBytesApplication nullSocketApplication = new NullBytesApplication();
        nullSocketApplication.initialize(getMailboxFactory().createAsyncMailbox());
        return nullSocketApplication;
    }
}
