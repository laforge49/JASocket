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
package org.agilewiki.jasocket;

import org.agilewiki.jactor.lpc.JLPCActor;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

abstract public class SocketWriter extends JLPCActor implements ExceptionProcessor {
    protected SocketChannel socketChannel;
    protected ByteBuffer writeBuffer;
    protected ExceptionProcessor exceptionProcessor = this;

    public String getRemoteAddress() throws Exception {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
        return getRemoteHostAddress() + ":" + inetSocketAddress.getPort();
    }

    public String getRemoteHostAddress() throws Exception {
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketChannel.getRemoteAddress();
        return inetSocketAddress.getAddress().getHostAddress();
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
        writeBuffer.flip();
        try {
            while (writeBuffer.hasRemaining())
                socketChannel.write(writeBuffer);
        } catch (ClosedChannelException ex) {
        }
        writeBuffer.clear();
    }

    public void close() {
        try {
            socketChannel.close();
        } catch (Exception ex) {
        }
    }
}
