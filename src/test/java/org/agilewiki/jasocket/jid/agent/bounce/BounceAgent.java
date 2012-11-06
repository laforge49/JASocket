package org.agilewiki.jasocket.jid.agent.bounce;

import org.agilewiki.jactor.RP;
import org.agilewiki.jasocket.jid.WriteRequest;
import org.agilewiki.jasocket.jid.agent.AgentJid;
import org.agilewiki.jid.Jid;
import org.agilewiki.jid.scalar.flens.integer.IntegerJid;

public class BounceAgent extends AgentJid {
    private IntegerJid getIntegerJid() throws Exception {
        return (IntegerJid) _iGet(0);
    }

    public int getCounter() throws Exception {
        return getIntegerJid().getValue();
    }

    public void setCounter(int value) throws Exception {
        getIntegerJid().setValue(value);
    }

    @Override
    public void start(RP<Jid> rp) throws Exception {
        int counter = getCounter() - 1;
        System.out.println(counter);
        if (counter < 1) {
            rp.processResponse(null);
            return;
        }
        setCounter(counter);
        (new WriteRequest(this)).send(this, this, rp);
    }
}
