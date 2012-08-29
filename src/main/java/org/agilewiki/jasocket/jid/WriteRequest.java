package org.agilewiki.jasocket.jid;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;
import org.agilewiki.jid.Jid;

public class WriteRequest extends Request<Jid, JidApplication> {
    public final Jid request;

    public WriteRequest(Jid request) {
        this.request = request;
    }

    @Override
    public boolean isTargetType(Actor targetActor) {
        return targetActor instanceof JidApplication;
    }

    @Override
    public void processRequest(JLPCActor targetActor, RP rp) throws Exception {
        ((JidApplication) targetActor).writeRequest(request, rp);
    }
}
