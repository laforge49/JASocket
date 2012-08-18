package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.lpc.TargetActor;

import java.nio.ByteBuffer;

public interface BytesProcessor extends TargetActor {
    public void processBytes(byte[] bytes) throws Exception;
}
