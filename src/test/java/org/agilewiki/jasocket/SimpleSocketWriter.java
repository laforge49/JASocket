package org.agilewiki.jasocket;

public class SimpleSocketWriter extends SocketWriter {
    @Override
    public void processException(Exception ex) {
        ex.printStackTrace();
    }
}
