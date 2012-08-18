package org.agilewiki.jasocket.server;

public class NullBytesProcessor extends BytesProcessor {
    @Override
    public void processBytes(byte[] bytes) throws Exception {
        System.out.println("received " + bytes.length + " bytes");
    }
}
