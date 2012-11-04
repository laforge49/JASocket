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

import org.agilewiki.jactor.concurrent.JAThreadFactory;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jasocket.server.SocketManager;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ThreadFactory;

abstract public class BytesProtocol extends JLPCActor implements SocketProtocol {
    private BytesSocket bytesSocket;
    private SocketManager socketManager;
    private boolean client;

    public boolean isClient() {
        return client;
    }

    public String getRemoteAddress() throws Exception {
        return bytesSocket.getRemoteAddress();
    }

    public void writeBytes(byte[] bytes) throws Exception {
        (new WriteBytes(bytes)).sendEvent(this, bytesSocket);
    }

    public void clientOpenLocal(int port, int maxPacketSize, SocketManager socketManager)
            throws Exception {
        InetAddress inetAddress = InetAddress.getLocalHost();
        clientOpen(new InetSocketAddress(inetAddress, port), maxPacketSize, socketManager);
    }

    public void clientOpen(InetSocketAddress inetSocketAddress, int maxPacketSize, SocketManager socketManager)
            throws Exception {
        clientOpen(inetSocketAddress, maxPacketSize, socketManager, new JAThreadFactory());
    }

    public void clientOpen(InetSocketAddress inetSocketAddress, int maxPacketSize, SocketManager socketManager, ThreadFactory threadFactory)
            throws Exception {
        this.socketManager = socketManager;
        bytesSocket = new BytesSocket();
        bytesSocket.setBytesProtocol(this);
        bytesSocket.initialize(getMailboxFactory().createAsyncMailbox());
        bytesSocket.clientOpen(inetSocketAddress, maxPacketSize, threadFactory);
        client = true;
    }

    public void serverOpen(SocketChannel socketChannel, int maxPacketSize, SocketManager socketManager, ThreadFactory threadFactory)
            throws Exception {
        this.socketManager = socketManager;
        bytesSocket = new BytesSocket();
        bytesSocket.setBytesProtocol(this);
        bytesSocket.initialize(getMailboxFactory().createAsyncMailbox());
        bytesSocket.serverOpen(socketChannel, maxPacketSize, threadFactory);
    }

    public void close() {
        bytesSocket.close();
    }

    @Override
    public void processException(Exception exception) {
        exception.printStackTrace();
        close();
    }
}
