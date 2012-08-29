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

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ThreadFactory;

abstract public class BytesApplication extends JLPCActor implements SocketApplication {
    public SocketApplication socketApplication = this;
    BytesSocket bytesSocket;

    abstract public void receiveBytes(byte[] bytes) throws Exception;

    public void writeBytes(byte[] bytes) throws Exception {
        (new WriteBytes(bytes)).sendEvent(this, bytesSocket);
    }

    public void clientOpen(InetSocketAddress inetSocketAddress, int maxPacketSize)
            throws Exception {
        clientOpen(inetSocketAddress, maxPacketSize, new JAThreadFactory());
    }

    public void clientOpen(InetSocketAddress inetSocketAddress, int maxPacketSize, ThreadFactory threadFactory)
            throws Exception {
        bytesSocket = new BytesSocket();
        bytesSocket.setSocketApplication(socketApplication);
        bytesSocket.initialize(getMailboxFactory().createAsyncMailbox());
        bytesSocket.clientOpen(inetSocketAddress, maxPacketSize, threadFactory);
    }

    @Override
    public void serverOpen(SocketChannel socketChannel, int maxPacketSize, ThreadFactory threadFactory)
            throws Exception {
        bytesSocket = new BytesSocket();
        bytesSocket.setSocketApplication(socketApplication);
        bytesSocket.initialize(getMailboxFactory().createAsyncMailbox());
        bytesSocket.serverOpen(socketChannel, maxPacketSize, threadFactory);
    }

    public void close() {
        bytesSocket.close();
        closed();
    }

    abstract protected void closed();
}
