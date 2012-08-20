package org.agilewiki.jasocket.server;

public class NullBytesReceiver extends BytesReceiver {
    @Override
    public void receiveBytes(byte[] bytes) throws Exception {
        System.out.println("received " + bytes.length + " bytes");
    }

    @Override
    public void processException(Exception exception) {
        exception.printStackTrace();
    }
}
