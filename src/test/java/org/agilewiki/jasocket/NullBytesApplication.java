package org.agilewiki.jasocket;

public class NullBytesApplication extends BytesApplication {
    @Override
    public void receiveBytes(byte[] bytes) throws Exception {
        System.out.println("received " + bytes.length + " bytes");
        if (bytes[bytes.length - 1] == '!') {
            socketAcceptor.close();
            getMailboxFactory().close();
        }
    }

    @Override
    public void processException(Exception exception) {
        exception.printStackTrace();
    }

    @Override
    protected void closed() {
    }
}
