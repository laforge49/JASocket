package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.lpc.TargetActor;

import java.nio.ByteBuffer;

public interface ByteBufferProcessor extends TargetActor {
    public void processByteBuffer(ByteBuffer byteBuffer) throws Exception;
}
