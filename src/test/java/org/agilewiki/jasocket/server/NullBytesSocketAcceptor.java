package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jasocket.NullSocketApplication;
import org.agilewiki.jasocket.SocketApplication;

import java.nio.channels.SocketChannel;

public class NullBytesSocketAcceptor extends SocketAcceptor {
    @Override
    protected ServerOpened createServerOpened() throws Exception {
        NullSocketApplication nullSocketApplication = new NullSocketApplication();
        nullSocketApplication.initialize(getMailboxFactory().createAsyncMailbox());
        return nullSocketApplication;
    }
}
