package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.lpc.JLPCActor;

public class NullBytesProcessor extends JLPCActor implements BytesProcessor {
    @Override
    public void processBytes(byte[] bytes) throws Exception {
        System.out.println("received " + bytes.length + " bytes");
    }
}
