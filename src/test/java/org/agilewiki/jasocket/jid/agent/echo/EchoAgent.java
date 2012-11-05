package org.agilewiki.jasocket.jid.agent.echo;

import org.agilewiki.jactor.RP;
import org.agilewiki.jasocket.jid.agent.AgentJid;
import org.agilewiki.jid.Jid;
import org.agilewiki.jid.scalar.vlens.string.StringJid;

public class EchoAgent extends AgentJid {
    private StringJid getStringJid() throws Exception {
        return (StringJid) _iGet(0);
    }

    public void setValue(String value) throws Exception {
        getStringJid().setValue(value);
    }

    @Override
    public void start(RP<Jid> rp) throws Exception {
        rp.processResponse(getStringJid());
    }
}
