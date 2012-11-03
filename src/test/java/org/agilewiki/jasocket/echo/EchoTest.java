package org.agilewiki.jasocket.echo;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jactor.MailboxFactory;
import org.agilewiki.jasocket.server.SocketAcceptor;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class EchoTest extends TestCase {
    public void test() throws Exception {
        MailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(10);
        int maxPacketSize = 30;
        InetAddress inetAddress = InetAddress.getLocalHost();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, 8885);
        SocketAcceptor socketAcceptor = new EchoSocketAcceptor();
        socketAcceptor.initialize(mailboxFactory.createMailbox());
        socketAcceptor.open(inetSocketAddress, maxPacketSize);
        DriverProtocol driverProtocol = new DriverProtocol();
        driverProtocol.initialize(mailboxFactory.createMailbox());
        driverProtocol.clientOpen(inetSocketAddress, maxPacketSize, socketAcceptor);
        DoIt.req.send(new JAFuture(), driverProtocol);
    }
}
