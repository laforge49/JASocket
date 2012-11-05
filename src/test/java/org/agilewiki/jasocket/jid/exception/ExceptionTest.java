package org.agilewiki.jasocket.jid.exception;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jactor.MailboxFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.jid.ExceptionJidFactory;
import org.agilewiki.jasocket.server.SocketManager;

public class ExceptionTest extends TestCase {
    public void test() throws Exception {
        MailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(10);
        JASocketFactories factory = new JASocketFactories();
        factory.initialize();
        int maxPacketSize = 300;
        SocketManager socketManager = new ExceptionSocketManager();
        socketManager.initialize(mailboxFactory.createMailbox(), factory);
        socketManager.openServerSocket(8884, maxPacketSize);
        DriverProtocol driverProtocol = new DriverProtocol();
        driverProtocol.initialize(mailboxFactory.createMailbox(), factory);
        driverProtocol.openLocal(8884, maxPacketSize, socketManager);
        try {
            DoIt.req.send(new JAFuture(), driverProtocol);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            socketManager.close();
            mailboxFactory.close();
        }
    }
}
