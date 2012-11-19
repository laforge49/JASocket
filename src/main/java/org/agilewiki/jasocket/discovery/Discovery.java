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
package org.agilewiki.jasocket.discovery;

import org.agilewiki.jactor.concurrent.ThreadManager;
import org.agilewiki.jasocket.server.AgentChannelManager;
import org.agilewiki.jasocket.server.OpenAgentChannel;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;

public class Discovery {
    protected NetworkInterface networkInterface() throws UnknownHostException, SocketException {
        return NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
    }

    protected InetAddress multicastingGroup() throws UnknownHostException {
        return InetAddress.getByName("225.49.42.13");
    }

    protected int multicastingPort() {
        return 8887;
    }

    protected long delay() {
        return 2000;
    }

    public Discovery(final AgentChannelManager agentChannelManager) throws Exception {
        final InetAddress multicastingGroup = multicastingGroup();
        final NetworkInterface networkInterface = networkInterface();
        final int multicastingPort = multicastingPort();
        final long delay = delay();
        final String agentChannelManagerAddress = agentChannelManager.agentChannelManagerAddress();
        final int agentChannelManagerPort = agentChannelManager.agentChannelManagerPort();

        final DatagramChannel datagramChannel = DatagramChannel.open(StandardProtocolFamily.INET)
                .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                .bind(new InetSocketAddress(multicastingPort))
                .setOption(StandardSocketOptions.IP_MULTICAST_IF, networkInterface);
        datagramChannel.join(multicastingGroup, networkInterface);

        ThreadManager threadManager = agentChannelManager.getMailboxFactory().getThreadManager();

        threadManager.process(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(4);
                    try {
                        InetSocketAddress inetSocketAddress = (InetSocketAddress) datagramChannel.receive(byteBuffer);
                        InetAddress inetAddress = inetSocketAddress.getAddress();
                        byteBuffer.flip();
                        int port = byteBuffer.getInt();
                        InetSocketAddress remoteInetSocketAddress = new InetSocketAddress(inetAddress, port);
                        if (!("/"+agentChannelManagerAddress).equals(remoteInetSocketAddress.toString()))
                            (new OpenAgentChannel(remoteInetSocketAddress)).sendEvent(agentChannelManager);
                    } catch (ClosedByInterruptException cbie) {
                        return;
                    } catch (ClosedChannelException cce) {
                        return;
                    } catch (Exception e) {
                        agentChannelManager.getMailboxFactory().logException(false, "Discovery receiver threw unexpected exception", e);
                        return;
                    }
                }
            }
        });

        threadManager.process(new Runnable() {
            @Override
            public void run() {
                InetSocketAddress inetSocketAddress = new InetSocketAddress(multicastingGroup, multicastingPort);
                while (true) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(4);
                    byteBuffer.putInt(agentChannelManagerPort);
                    byteBuffer.flip();
                    try {
                        datagramChannel.send(byteBuffer, inetSocketAddress);
                    } catch (IOException e) {
                        agentChannelManager.getMailboxFactory().logException(false, "Discovery sender threw unexpected exception", e);
                        agentChannelManager.getMailboxFactory().close();
                        return;
                    }
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        });
    }
}
