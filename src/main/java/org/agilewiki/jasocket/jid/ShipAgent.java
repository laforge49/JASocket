package org.agilewiki.jasocket.jid;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;
import org.agilewiki.jasocket.jid.agent.AgentJid;
import org.agilewiki.jasocket.jid.agent.AgentChannel;
import org.agilewiki.jid.Jid;

public class ShipAgent extends Request<Jid, AgentChannel> {
    public final AgentJid request;

    public ShipAgent(AgentJid request) {
        this.request = request;
    }

    @Override
    public boolean isTargetType(Actor targetActor) {
        return targetActor instanceof AgentChannel;
    }

    @Override
    public void processRequest(JLPCActor targetActor, RP rp) throws Exception {
        ((AgentChannel) targetActor).shipAgent(request, rp);
    }
}
