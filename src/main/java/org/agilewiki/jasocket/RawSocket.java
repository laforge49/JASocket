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

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.concurrent.JAThreadFactory;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ThreadFactory;

abstract public class RawSocket extends SocketWriter {
    int maxPacketSize;
    Thread readerThread;

    @Override
    public void clientOpen(InetSocketAddress inetSocketAddress, int maxPacketSize)
            throws Exception {
        clientOpen(inetSocketAddress, maxPacketSize, new JAThreadFactory());
    }

    public void clientOpen(InetSocketAddress inetSocketAddress, int maxPacketSize, ThreadFactory threadFactory)
            throws Exception {
        super.clientOpen(inetSocketAddress, maxPacketSize);
        this.maxPacketSize = maxPacketSize;
        readerThread = threadFactory.newThread(new Reader());
        readerThread.start();
    }

    public void serverOpen(SocketChannel socketChannel, int maxPacketSize, ThreadFactory threadFactory)
            throws Exception {
        writeBuffer = ByteBuffer.allocateDirect(maxPacketSize);
        this.socketChannel = socketChannel;
        this.maxPacketSize = maxPacketSize;
        readerThread = threadFactory.newThread(new Reader());
        readerThread.start();
    }

    protected abstract void receiveByteBuffer(ByteBuffer byteBuffer) throws Exception;

    class Reader implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(maxPacketSize);
                    int i = socketChannel.read(byteBuffer);
                    if (i == -1)
                        return;
                    byteBuffer.flip();
                    (new ReceiveByteBuffer(byteBuffer)).sendEvent(RawSocket.this);
                }
            } catch (ClosedChannelException cce) {
            } catch (Exception ex) {
                try {
                    (new ProcessException(ex)).sendEvent(exceptionProcessor);
                } catch (Exception x) {
                    x.printStackTrace();
                }
            }
        }
    }
}

class ReceiveByteBuffer extends Request<Object, RawSocket> {
    ByteBuffer byteBuffer;

    public ReceiveByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public boolean isTargetType(Actor targetActor) {
        return targetActor instanceof RawSocket;
    }

    @Override
    public void processRequest(JLPCActor targetActor, RP rp) throws Exception {
        ((RawSocket) targetActor).receiveByteBuffer(byteBuffer);
        rp.processResponse(null);
    }
}
