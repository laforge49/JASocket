package org.agilewiki.jasocket.client;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;

public class WriteBytes extends Request<Object, RawWriter> {
    byte[] bytes;

    public WriteBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public boolean isTargetType(Actor targetActor) {
        return targetActor instanceof RawWriter;
    }

    @Override
    public void processRequest(JLPCActor targetActor, RP rp) throws Exception {
        ((RawWriter) targetActor).writeBytes(bytes);
        rp.processResponse(null);
    }
}
