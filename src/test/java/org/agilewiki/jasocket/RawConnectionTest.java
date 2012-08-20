package org.agilewiki.jasocket;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jactor.MailboxFactory;
import org.agilewiki.jasocket.SocketWriter;
import org.agilewiki.jasocket.WriteRawBytes;
import org.agilewiki.jasocket.server.NullRawSocketAcceptor;
import org.agilewiki.jasocket.server.SocketAcceptor;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class RawConnectionTest extends TestCase {
    public void test() throws Exception {
        int maxPacketSize = 3;
        MailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(10);
        SocketAcceptor socketAcceptor = new NullRawSocketAcceptor();
        try {
            socketAcceptor.initialize(mailboxFactory.createMailbox());
            InetAddress inetAddress = InetAddress.getLocalHost();
            InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, 8889);
            socketAcceptor.open(inetSocketAddress, maxPacketSize);
            SocketWriter rawWriter = new SocketWriter();
            rawWriter.initialize(mailboxFactory.createAsyncMailbox());
            rawWriter.clientOpen(inetSocketAddress, maxPacketSize);
            (new WriteRawBytes("Hello".getBytes())).sendEvent(rawWriter);
            (new WriteRawBytes(" ".getBytes())).sendEvent(rawWriter);
            (new WriteRawBytes("world!".getBytes())).send(new JAFuture(), rawWriter);
            rawWriter.close();
            Thread.sleep(200);
        } finally {
            socketAcceptor.close();
            mailboxFactory.close();
        }
    }
}
