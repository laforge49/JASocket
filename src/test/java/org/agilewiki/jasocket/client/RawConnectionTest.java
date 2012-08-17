package org.agilewiki.jasocket.client;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jactor.MailboxFactory;
import org.agilewiki.jasocket.server.NullSocketAcceptor;
import org.agilewiki.jasocket.server.SocketAcceptor;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class RawConnectionTest extends TestCase {
    public void test() throws Exception {
        MailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(10);
        SocketAcceptor socketAcceptor = new NullSocketAcceptor();
        try {
            socketAcceptor.initialize(mailboxFactory.createMailbox());
            InetAddress inetAddress = InetAddress.getLocalHost();
            InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, 8889);
            socketAcceptor.open(inetSocketAddress, 100000);
            RawWriter rawWriter = new RawWriter();
            rawWriter.initialize(mailboxFactory.createAsyncMailbox());
            rawWriter.open(inetSocketAddress, 100000);
            ByteBuffer byteBuffer = ByteBuffer.allocate(100000);
            byteBuffer.putInt(42);
            (new WriteByteBuffer(byteBuffer)).send(new JAFuture(), rawWriter);
            byteBuffer = ByteBuffer.allocate(100000);
            byteBuffer.putInt(42);
            (new WriteByteBuffer(byteBuffer)).send(new JAFuture(), rawWriter);
            rawWriter.close();
            Thread.sleep(200);
        } finally {
            socketAcceptor.close();
            mailboxFactory.close();
        }
    }
}
