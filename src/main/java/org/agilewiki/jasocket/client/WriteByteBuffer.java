package org.agilewiki.jasocket.client;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;

import java.nio.ByteBuffer;

public class WriteByteBuffer extends Request<Object, RawWriter> {
    ByteBuffer byteBuffer;

    public WriteByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public boolean isTargetType(Actor targetActor) {
        return targetActor instanceof RawWriter;
    }

    @Override
    public void processRequest(JLPCActor targetActor, RP rp) throws Exception {
        ((RawWriter) targetActor).writeByteBuffer(byteBuffer);
        rp.processResponse(null);
    }
}
