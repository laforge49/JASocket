package org.agilewiki.jasocket;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jactor.MailboxFactory;
import org.agilewiki.jasocket.server.NullRawSocketAcceptor;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class RawConnectionTest extends TestCase {
    public void test() throws Exception {
        int maxPacketSize = 3;
        MailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(10);
        NullRawSocketAcceptor socketAcceptor = new NullRawSocketAcceptor();
        socketAcceptor.initialize(mailboxFactory.createMailbox());
        socketAcceptor.deadIn = 12; //total bytes sent before close
        InetAddress inetAddress = InetAddress.getLocalHost();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, 8889);
        socketAcceptor.open(inetSocketAddress, maxPacketSize);
        SocketWriter rawWriter = new SimpleSocketWriter();
        rawWriter.initialize(mailboxFactory.createAsyncMailbox());
        rawWriter.clientOpen(inetSocketAddress, maxPacketSize);
        (new WriteRawBytes("Hello".getBytes())).sendEvent(rawWriter);
        (new WriteRawBytes(" ".getBytes())).sendEvent(rawWriter);
        (new WriteRawBytes("world!".getBytes())).send(new JAFuture(), rawWriter);
        rawWriter.close();
    }
}
