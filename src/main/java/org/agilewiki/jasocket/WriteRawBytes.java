package org.agilewiki.jasocket;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;

public class WriteRawBytes extends Request<Object, SocketWriter> {
    byte[] bytes;

    public WriteRawBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public boolean isTargetType(Actor targetActor) {
        return targetActor instanceof SocketWriter;
    }

    @Override
    public void processRequest(JLPCActor targetActor, RP rp) throws Exception {
        ((SocketWriter) targetActor).writeBytes(bytes);
        rp.processResponse(null);
    }
}
