package org.agilewiki.jasocket.agentChannel;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;
import org.agilewiki.jid.Jid;

public class CloseChannel extends Request<Jid, AgentChannel> {
    public static CloseChannel req = new CloseChannel();

    @Override
    public boolean isTargetType(Actor targetActor) {
        return targetActor instanceof AgentChannel;
    }

    @Override
    public void processRequest(JLPCActor targetActor, RP rp) throws Exception {
        ((AgentChannel) targetActor).closeChannel();
        rp.processResponse(null);
    }
}
