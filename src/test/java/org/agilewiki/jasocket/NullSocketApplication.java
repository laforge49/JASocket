package org.agilewiki.jasocket;

public class NullSocketApplication extends SocketApplication {
    @Override
    public void receiveBytes(byte[] bytes) throws Exception {
        System.out.println("received " + bytes.length + " bytes");
    }

    @Override
    public void processException(Exception exception) {
        exception.printStackTrace();
    }
}
