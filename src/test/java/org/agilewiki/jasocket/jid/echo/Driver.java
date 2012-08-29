package org.agilewiki.jasocket.jid.echo;

import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jasocket.jid.JidApplication;
import org.agilewiki.jasocket.server.SocketAcceptor;
import org.agilewiki.jid.Jid;
import org.agilewiki.jid.JidFactories;
import org.agilewiki.jid.ReadableBytes;
import org.agilewiki.jid.scalar.vlens.string.StringJid;
import org.agilewiki.jid.scalar.vlens.string.StringJidFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Driver extends JidApplication {
    SocketAcceptor socketAcceptor;

    public void doit(final RP rp) throws Exception {
        int maxPacketSize = 300;
        InetAddress inetAddress = InetAddress.getLocalHost();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, 8884);
        socketAcceptor = new JidEchoSocketAcceptor();
        socketAcceptor.initialize(getMailboxFactory().createMailbox(), this);
        socketAcceptor.open(inetSocketAddress, maxPacketSize);
        clientOpen(inetSocketAddress, maxPacketSize);
        StringJid sj = (StringJid) JAFactory.newActor(this, JidFactories.STRING_JID_TYPE);
        sj.setValue("Hello");
        writeRequest(sj, new RP<Jid>() {
            @Override
            public void processResponse(Jid response) throws Exception {
                StringJid rsp = (StringJid) response;
                System.out.println(rsp.getValue());
            }
        });
        StringJid sj2 = (StringJid) JAFactory.newActor(this, JidFactories.STRING_JID_TYPE);
        sj2.setValue("world!");
        writeRequest(sj2, new RP<Jid>() {
            @Override
            public void processResponse(Jid response) throws Exception {
                StringJid rsp = (StringJid) response;
                System.out.println(rsp.getValue());
                rp.processResponse(null);
            }
        });
    }

    @Override
    public void close() {
        socketAcceptor.close();
        super.close();
    }
}