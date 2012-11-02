package org.agilewiki.jasocket.jid.echo;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jactor.Mailbox;
import org.agilewiki.jactor.MailboxFactory;
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jasocket.jid.TransportJidFactory;
import org.agilewiki.jasocket.server.SocketAcceptor;
import org.agilewiki.jid.JidFactories;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class JidEchoTest extends TestCase {
    public void test() throws Exception {
        MailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(10);
        Mailbox mailbox = mailboxFactory.createMailbox();
        JAFactory factory = new JAFactory();
        factory.initialize(mailbox);
        JidFactories factories = new JidFactories();
        factories.initialize(mailbox, factory);
        factory.registerActorFactory(TransportJidFactory.fac);
        DriverApplication driverApplication = new DriverApplication();
        driverApplication.initialize(mailbox, factory);
        int maxPacketSize = 300;
        InetAddress inetAddress = InetAddress.getLocalHost();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, 8884);
        SocketAcceptor socketAcceptor = new JidEchoSocketAcceptor();
        socketAcceptor.initialize(mailboxFactory.createMailbox(), factory);
        socketAcceptor.open(inetSocketAddress, maxPacketSize);
        driverApplication.socketAcceptor = socketAcceptor;
        driverApplication.clientOpen(inetSocketAddress, maxPacketSize);

        try {
            DoIt.req.send(new JAFuture(), driverApplication);
        } finally {
            socketAcceptor.close();
            mailboxFactory.close();
        }
    }
}
