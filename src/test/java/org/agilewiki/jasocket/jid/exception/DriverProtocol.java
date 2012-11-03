package org.agilewiki.jasocket.jid.exception;

import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jasocket.jid.JidProtocol;
import org.agilewiki.jasocket.jid.WriteRequest;
import org.agilewiki.jasocket.server.SocketAcceptor;
import org.agilewiki.jid.Jid;
import org.agilewiki.jid.JidFactories;
import org.agilewiki.jid.scalar.vlens.string.StringJid;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class DriverProtocol extends JidProtocol {
    public void doit(final RP rp) throws Exception {
        int maxPacketSize = 300;
        InetAddress inetAddress = InetAddress.getLocalHost();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, 8884);
        socketAcceptor = new ExceptionSocketAcceptor();
        socketAcceptor.initialize(getMailboxFactory().createMailbox(), this);
        socketAcceptor.open(inetSocketAddress, maxPacketSize);
        clientOpen(inetSocketAddress, maxPacketSize);
        StringJid sj = (StringJid) JAFactory.newActor(this, JidFactories.STRING_JID_TYPE);
        sj.setValue("Hello");
        (new WriteRequest(sj)).send(this, this, new RP<Jid>() {
            @Override
            public void processResponse(Jid response) throws Exception {
                StringJid rsp = (StringJid) response;
                System.out.println(rsp.getValue());
            }
        });
    }

    protected void receiveRequest(Jid jid, RP<Jid> rp) throws Exception {
        throw new UnsupportedOperationException();
    }
}
