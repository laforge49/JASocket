package org.agilewiki.jasocket.client;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;

import java.nio.ByteBuffer;

public class WriteBytes extends Request<Object, SocketWriter> {
    byte[] bytes;

    public WriteBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public boolean isTargetType(Actor targetActor) {
        return targetActor instanceof SocketWriter;
    }

    @Override
    public void processRequest(JLPCActor targetActor, RP rp) throws Exception {
        byte[] lengthBytes = new byte[4];
        ByteBuffer lengthBuffer = ByteBuffer.wrap(lengthBytes);
        lengthBuffer.putInt(bytes.length);
        ((SocketWriter) targetActor).writeBytes(lengthBytes);
        ((SocketWriter) targetActor).writeBytes(bytes);
        rp.processResponse(null);
    }
}
