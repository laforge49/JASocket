package org.agilewiki.jasocket;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jactor.MailboxFactory;
import org.agilewiki.jasocket.server.NullBytesSocketAcceptor;
import org.agilewiki.jasocket.server.SocketAcceptor;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class BytesConnectionTest extends TestCase {
    public void test() throws Exception {
        int maxPacketSize = 30;
        MailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(10);
        SocketAcceptor socketAcceptor = new NullBytesSocketAcceptor();
        try {
            socketAcceptor.initialize(mailboxFactory.createMailbox());
            InetAddress inetAddress = InetAddress.getLocalHost();
            InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, 8886);
            socketAcceptor.open(inetSocketAddress, maxPacketSize);
            SocketWriter rawWriter = new SimpleSocketWriter();
            rawWriter.initialize(mailboxFactory.createAsyncMailbox());
            rawWriter.clientOpen(inetSocketAddress, maxPacketSize);
            (new WriteBytes("Hello".getBytes())).sendEvent(rawWriter);
            (new WriteBytes(" ".getBytes())).sendEvent(rawWriter);
            (new WriteBytes("world!".getBytes())).send(new JAFuture(), rawWriter);
            rawWriter.close();
            Thread.sleep(200);
        } finally {
            socketAcceptor.close();
            mailboxFactory.close();
        }
    }
}
