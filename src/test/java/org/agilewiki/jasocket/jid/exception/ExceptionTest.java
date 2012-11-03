package org.agilewiki.jasocket.jid.exception;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jactor.Mailbox;
import org.agilewiki.jactor.MailboxFactory;
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jasocket.jid.ExceptionJidFactory;
import org.agilewiki.jasocket.jid.TransportJidFactory;
import org.agilewiki.jasocket.server.SocketAcceptor;
import org.agilewiki.jid.JidFactories;

public class ExceptionTest extends TestCase {
    public void test() throws Exception {
        MailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(10);
        Mailbox mailbox = mailboxFactory.createMailbox();
        JAFactory factory = new JAFactory();
        factory.initialize(mailbox);
        JidFactories factories = new JidFactories();
        factories.initialize(mailbox, factory);
        factory.registerActorFactory(TransportJidFactory.fac);
        factory.registerActorFactory(ExceptionJidFactory.fac);
        int maxPacketSize = 300;
        SocketAcceptor socketAcceptor = new ExceptionSocketAcceptor();
        socketAcceptor.initialize(mailboxFactory.createMailbox(), factory);
        socketAcceptor.open(8884, maxPacketSize);
        DriverProtocol driverProtocol = new DriverProtocol();
        driverProtocol.initialize(mailbox, factory);
        driverProtocol.clientOpenLocal(8884, maxPacketSize, socketAcceptor);
        try {
            DoIt.req.send(new JAFuture(), driverProtocol);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            socketAcceptor.close();
            mailboxFactory.close();
        }
    }
}
