package org.agilewiki.jasocket.jid.echo;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jactor.MailboxFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.server.SocketManager;

public class JidEchoTest extends TestCase {
    public void test() throws Exception {
        MailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(10);
        JASocketFactories factory = new JASocketFactories();
        factory.initialize();
        int maxPacketSize = 300;
        SocketManager socketManager = new JidEchoSocketManager();
        socketManager.initialize(mailboxFactory.createMailbox(), factory);
        socketManager.openServerSocket(8884, maxPacketSize);
        DriverProtocol driverApplication = new DriverProtocol();
        driverApplication.initialize(mailboxFactory.createMailbox(), factory);
        driverApplication.openLocal(8884, maxPacketSize, socketManager);

        try {
            DoIt.req.send(new JAFuture(), driverApplication);
        } finally {
            socketManager.close();
            mailboxFactory.close();
        }
    }
}
