package org.agilewiki.jasocket.echo;

import org.agilewiki.jasocket.BytesApplication;

public class Echo extends BytesApplication {
    @Override
    public void receiveBytes(byte[] bytes) throws Exception {
        writeBytes(bytes);
    }

    @Override
    public void processException(Exception ex) {
        ex.printStackTrace();
    }
}
