package org.agilewiki.jasocket;

public interface SocketApplication extends ExceptionProcessor {
    public void receiveBytes(byte[] bytes) throws Exception;
}
