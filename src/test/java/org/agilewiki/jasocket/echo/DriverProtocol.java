package org.agilewiki.jasocket.echo;

import org.agilewiki.jasocket.BytesProtocol;

public class DriverProtocol extends BytesProtocol {
    public void doit() throws Exception {
        writeBytes("Hello".getBytes());
        writeBytes("world!".getBytes());
    }

    @Override
    public void receiveBytes(byte[] bytes) throws Exception {
        String s = new String(bytes);
        System.out.println(s);
        if (s.endsWith("!")) {
            socketAcceptor().close();
            getMailboxFactory().close();
        }
    }

    @Override
    public void processException(Exception ex) {
        ex.printStackTrace();
    }

    @Override
    protected void closed() {
    }
}
