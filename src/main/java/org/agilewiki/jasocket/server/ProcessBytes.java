package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;

public class ProcessBytes extends Request<Object, BytesProcessor> {
    byte[] bytes;

    public ProcessBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public boolean isTargetType(Actor actor) {
        return actor instanceof BytesProcessor;
    }

    @Override
    public void processRequest(JLPCActor jlpcActor, RP rp) throws Exception {
        ((BytesProcessor) jlpcActor).processBytes(bytes);
        rp.processResponse(null);
    }
}
