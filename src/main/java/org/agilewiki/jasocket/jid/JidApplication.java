package org.agilewiki.jasocket.jid;

import org.agilewiki.jactor.RP;
import org.agilewiki.jasocket.BytesApplication;
import org.agilewiki.jid.Jid;
import org.agilewiki.jid.collection.flenc.TupleJid;
import org.agilewiki.jid.scalar.flens.bool.BooleanJid;
import org.agilewiki.jid.scalar.flens.lng.LongJid;
import org.agilewiki.jid.scalar.vlens.actor.ActorJid;
import org.agilewiki.jid.scalar.vlens.actor.RootJid;
import org.agilewiki.jid.scalar.vlens.actor.RootJidFactory;

import java.util.HashMap;

public class JidApplication extends BytesApplication {
    HashMap<Long, RP> rps = new HashMap<Long, RP>();
    long requestId = 0;

    @Override
    public void receiveBytes(byte[] bytes) throws Exception {
        RootJid root = (RootJid) RootJidFactory.fac.newActor(getMailbox(), null);
        root.setBytes(TransportFactory.fac, bytes);
        TupleJid transport = (TupleJid) root.getValue();
        BooleanJid requestFlag = (BooleanJid) transport.iGet(0);
        LongJid idj = (LongJid) transport.iGet(1);
        Long id = idj.getValue();
        ActorJid envelope = (ActorJid) transport.iGet(2);
        Jid jid = (Jid) envelope.getValue();
        if (requestFlag.getValue())
            gotReq(id, jid);
        else
            gotRsp(id, jid);
    }

    private void gotReq(final Long id, Jid jid) throws Exception {
        receiveRequest(jid, new RP<Jid>() {
            @Override
            public void processResponse(Jid response) throws Exception {
                write(false, id, response);
            }
        });
    }

    protected void receiveRequest(Jid jid, RP<Jid> rp) throws Exception {
    }

    private void gotRsp(Long id, Jid jid) throws Exception {
        RP rp = rps.remove(id);
        if (rp != null)
            rp.processResponse(jid);
    }

    @Override
    public void processException(Exception ex) {
        ex.printStackTrace();
        close();
    }

    public void writeRequest(final Jid jid, final RP rp) throws Exception {
        requestId += 1;
        requestId %= 1000000000000000000L;
        rps.put(requestId, rp);
        write(true, requestId, jid);
    }

    private void write(boolean requestFlag, Long id, Jid jid) throws Exception {
        TupleJid transport = (TupleJid) TransportFactory.fac.newActor(getMailbox(), null);
        BooleanJid requestFlagJid = (BooleanJid) transport.iGet(0);
        requestFlagJid.setValue(requestFlag);
        LongJid idj = (LongJid) transport.iGet(1);
        idj.setValue(id);
        ActorJid envelope = (ActorJid) transport.iGet(2);
        envelope.setBytes(jid.getFactory(), jid.getSerializedBytes());
        RootJid root = (RootJid) RootJidFactory.fac.newActor(getMailbox(), null);
        root.setBytes(TransportFactory.fac, transport.getSerializedBytes());
        byte[] bytes = root.getSerializedBytes();
        writeBytes(bytes);
    }
}
