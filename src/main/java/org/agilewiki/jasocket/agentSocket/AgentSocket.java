/*
 * Copyright 2012 Bill La Forge
 *
 * This file is part of AgileWiki and is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License (LGPL) as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 * or navigate to the following url http://www.gnu.org/licenses/lgpl-2.1.txt
 *
 * Note however that only Scala, Java and JavaScript files are being covered by LGPL.
 * All other files are covered by the Common Public License (CPL).
 * A copy of this license is also included and can be
 * found as well at http://www.opensource.org/licenses/cpl1.0.txt
 */
package org.agilewiki.jasocket.agentSocket;

import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jasocket.ExceptionProcessor;
import org.agilewiki.jasocket.ProcessException;
import org.agilewiki.jasocket.agentChannel.AgentChannel;
import org.agilewiki.jasocket.agentChannel.CloseChannel;
import org.agilewiki.jasocket.agentChannel.ReceiveBytes;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

public class AgentSocket extends JLPCActor implements ExceptionProcessor {
    byte[] lengthBytes = new byte[4];
    int lengthIndex = 0;
    int length;
    byte[] bytes = null;
    int bytesIndex;
    int maxPacketSize;
    AgentChannel agentChannel;
    SocketChannel socketChannel;
    ByteBuffer writeBuffer;
    ExceptionProcessor exceptionProcessor = this;

    public void setAgentChannel(AgentChannel agentChannel) {
        this.agentChannel = agentChannel;
        exceptionProcessor = agentChannel;
    }

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
        (new ReceiveBytes(b)).sendEvent(this, agentChannel);
    }

    @Override
    public void processException(Exception exception) {
        getMailboxFactory().logException(false, "AgentSocket threw unhandled exception", exception);
    }

    public void clientOpen(InetSocketAddress inetSocketAddress, int maxPacketSize)
            throws Exception {
        writeBuffer = ByteBuffer.allocateDirect(maxPacketSize);
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(true);
        socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, maxPacketSize);
        socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, maxPacketSize);
        socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        socketChannel.connect(inetSocketAddress);
        this.maxPacketSize = maxPacketSize;
        getMailboxFactory().getThreadManager().process(new Reader());
    }

    public void serverOpen(SocketChannel socketChannel, int maxPacketSize)
            throws Exception {
        writeBuffer = ByteBuffer.allocateDirect(maxPacketSize);
        this.socketChannel = socketChannel;
        this.maxPacketSize = maxPacketSize;
        getMailboxFactory().getThreadManager().process(new Reader());
    }

    class Reader implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(maxPacketSize);
                    int i = socketChannel.read(byteBuffer);
                    if (i == -1) {
                        CloseChannel.req.sendEvent(agentChannel);
                        return;
                    }
                    agentChannel.received();
                    byteBuffer.flip();
                    (new ReceiveByteBuffer(byteBuffer)).sendEvent(AgentSocket.this);
                }
            } catch (ClosedChannelException cce) {
            } catch (Exception ex) {
                try {
                    (new ProcessException(ex)).sendEvent(exceptionProcessor);
                } catch (Exception x) {
                    getMailboxFactory().logException(false, "AgentSocket threw unhandled exception", x);
                }
            }
        }
    }

    public String getRemoteAddress() throws Exception {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
        return getRemoteHostAddress() + ":" + inetSocketAddress.getPort();
    }

    public String getRemoteHostAddress() throws Exception {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
        return inetSocketAddress.getAddress().getHostAddress();
    }

    public void writeBytes(byte[] bytes) throws Exception {
        try {
            int i = 0;
            while (i < bytes.length) {
                int l = bytes.length - i;
                int r = writeBuffer.remaining();
                if (l > r)
                    l = r;
                writeBuffer.put(bytes, i, l);
                i += l;
                if (!writeBuffer.hasRemaining())
                    write();
            }
            if (writeBuffer.position() > 0 && getMailbox().isEmpty())
                write();
        } catch (Exception ex) {
            try {
                (new ProcessException(ex)).sendEvent(exceptionProcessor);
            } catch (Exception x) {
                getMailboxFactory().logException(false, "sendEvent threw unhandled exception", x);
            }
        }
    }

    void write() throws Exception {
        agentChannel.sent();
        writeBuffer.flip();
        while (writeBuffer.hasRemaining())
            socketChannel.write(writeBuffer);
        writeBuffer.clear();
    }

    public void close() {
        try {
            socketChannel.close();
        } catch (Exception ex) {
        }
    }
}
