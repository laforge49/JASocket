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
import java.util.Timer;
import java.util.TimerTask;

public class Discovery {
    public Discovery(
            final AgentChannelManager agentChannelManager,
            NetworkInterface networkInterface,
            String group,
            int multicastingPort,
            long delay) throws Exception {
        final Timer timer = agentChannelManager.getMailboxFactory().timer();
        final InetAddress multicastingGroup = InetAddress.getByName(group);
        final String agentChannelManagerAddress = agentChannelManager.agentChannelManagerAddress();
        final int agentChannelManagerPort = agentChannelManager.agentChannelManagerPort();
        final InetSocketAddress inetSocketAddress = new InetSocketAddress(multicastingGroup, multicastingPort);

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
                        if (!("/" + agentChannelManagerAddress).equals(remoteInetSocketAddress.toString()))
                            (new OpenAgentChannel(remoteInetSocketAddress)).sendEvent(agentChannelManager);
                    } catch (ClosedByInterruptException cbie) {
                        return;
                    } catch (ClosedChannelException cce) {
                        System.out.println("Discovery exception");
                        cce.printStackTrace();
                        return;
                    } catch (Exception e) {
                        agentChannelManager.getMailboxFactory().logException(false, "Discovery receiver threw unexpected exception", e);
                        return;
                    }
                }
            }
        });

        TimerTask tt1 = new TimerTask() {
            @Override
            public void run() {
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
            }
        };
        timer.scheduleAtFixedRate(tt1, 0, delay);
    }
}
