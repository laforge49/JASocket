package org.agilewiki.jasocket;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;

public class ReceiveBytes extends Request<Object, SocketApplication> {
    byte[] bytes;

    public ReceiveBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public boolean isTargetType(Actor actor) {
        return actor instanceof SocketApplication;
    }

    @Override
    public void processRequest(JLPCActor jlpcActor, RP rp) throws Exception {
        ((SocketApplication) jlpcActor).receiveBytes(bytes);
        rp.processResponse(null);
    }
}
