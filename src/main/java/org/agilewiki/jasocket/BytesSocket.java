package org.agilewiki.jasocket;

import java.nio.ByteBuffer;

public class BytesSocket extends RawSocket {
    private SocketApplication socketApplication;
    byte[] lengthBytes = new byte[4];
    int lengthIndex = 0;
    int length;
    byte[] bytes = null;
    int bytesIndex;

    public void setSocketApplication(SocketApplication socketApplication) {
        this.socketApplication = socketApplication;
        exceptionProcessor = socketApplication;
    }

    @Override
    protected void receiveByteBuffer(ByteBuffer byteBuffer) throws Exception {
        while (byteBuffer.remaining() > 0) {
            if (bytes == null)
                buildLength(byteBuffer);
            else
                buildBytes(byteBuffer);
        }
    }

    void buildLength(ByteBuffer byteBuffer) {
        int r = byteBuffer.remaining();
        int l = 4 - lengthIndex;
        if (l > r)
            l = r;
        byteBuffer.get(lengthBytes, lengthIndex, l);
        lengthIndex += l;
        if (lengthIndex < 4)
            return;
        ByteBuffer lengthBuffer = ByteBuffer.wrap(lengthBytes);
        length = lengthBuffer.getInt();
        bytes = new byte[length];
        bytesIndex = 0;
        lengthIndex = 0;
    }

    void buildBytes(ByteBuffer byteBuffer) throws Exception {
        int r = byteBuffer.remaining();
        int l = length - bytesIndex;
        if (l > r)
            l = r;
        byteBuffer.get(bytes, bytesIndex, l);
        bytesIndex += l;
        if (bytesIndex < length)
            return;
        byte[] b = bytes;
        bytes = null;
        (new ReceiveBytes(b)).sendEvent(this, socketApplication);
    }

    @Override
    public void processException(Exception exception) {
        exception.printStackTrace();
    }
}
