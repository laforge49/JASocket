package org.agilewiki.jasocket.jid.echo;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;

public class DoIt extends Request<Object, DriverApplication> {
    public static DoIt req = new DoIt();

    @Override
    public boolean isTargetType(Actor targetActor) {
        return targetActor instanceof DriverApplication;
    }

    @Override
    public void processRequest(JLPCActor targetActor, RP rp) throws Exception {
        ((DriverApplication) targetActor).doit(rp);
    }
}
