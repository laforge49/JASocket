package org.agilewiki.jasocket.jid.agent.exception;

import org.agilewiki.jactor.RP;
import org.agilewiki.jasocket.jid.agent.AgentJid;
import org.agilewiki.jid.Jid;

public class ExceptionAgent extends AgentJid {
    @Override
    public void start(RP<Jid> rp) throws Exception {
        throw new Exception("test exception");
    }
}
