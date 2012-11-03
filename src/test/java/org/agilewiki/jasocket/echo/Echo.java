package org.agilewiki.jasocket.echo;

import org.agilewiki.jasocket.BytesProtocol;

public class Echo extends BytesProtocol {
    @Override
    public void receiveBytes(byte[] bytes) throws Exception {
        writeBytes(bytes);
    }

    @Override
    public void processException(Exception ex) {
        ex.printStackTrace();
    }

    @Override
    protected void closed() {
    }
}
