package org.agilewiki.jasocket.echo;

import org.agilewiki.jasocket.BytesApplication;
import org.agilewiki.jasocket.server.SocketAcceptor;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Driver extends BytesApplication {
    SocketAcceptor socketAcceptor;

    public void doit() throws Exception {
        int maxPacketSize = 30;
        InetAddress inetAddress = InetAddress.getLocalHost();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, 8885);
        socketAcceptor = new EchoSocketAcceptor();
        socketAcceptor.initialize(getMailboxFactory().createMailbox());
        socketAcceptor.open(inetSocketAddress, maxPacketSize);
        clientOpen(inetSocketAddress, maxPacketSize);
        writeBytes("Hello".getBytes());
        writeBytes("world!".getBytes());
    }

    @Override
    public void receiveBytes(byte[] bytes) throws Exception {
        System.out.println(new String(bytes));
    }

    @Override
    public void processException(Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void close() {
        socketAcceptor.close();
        super.close();
    }

    @Override
    protected void closed() {
    }
}
