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
package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.concurrent.JAThreadFactory;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;
import org.agilewiki.jasocket.concurrent.ConcurrentDupMap;
import org.agilewiki.jasocket.jid.agent.AgentProtocol;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

public class SocketManager extends JLPCActor {
    ServerSocketChannel serverSocketChannel;
    ThreadFactory threadFactory;
    Thread thread;
    public int maxPacketSize = 10000;
    ConcurrentDupMap<String, AgentProtocol> agentProtocols = new ConcurrentDupMap<String, AgentProtocol>();

    public AgentProtocol localAgentProtocol(int port)
            throws Exception {
        InetAddress inetAddress = InetAddress.getLocalHost();
        return agentProtocol(new InetSocketAddress(inetAddress, port));
    }

    public AgentProtocol agentProtocol(InetSocketAddress inetSocketAddress)
            throws Exception {
        return agentProtocol(inetSocketAddress, new JAThreadFactory());
    }

    public AgentProtocol agentProtocol(InetSocketAddress inetSocketAddress, ThreadFactory threadFactory)
            throws Exception {
        InetAddress inetAddress = inetSocketAddress.getAddress();
        String remoteAddress = inetAddress.getHostAddress() + ":" + inetSocketAddress.getPort();
        AgentProtocol agentProtocol = agentProtocols.getAny(remoteAddress);
        if (agentProtocol != null)
            return agentProtocol;
        agentProtocol = new AgentProtocol();
        agentProtocol.initialize(getMailboxFactory().createMailbox(), this);
        agentProtocol.open(inetSocketAddress, maxPacketSize, this, threadFactory);
        agentProtocols.add(agentProtocol.getRemoteAddress(), agentProtocol);
        return agentProtocol;
    }

    public void openServerSocket(int port) throws Exception {
        InetAddress inetAddress = InetAddress.getLocalHost();
        openServerSocket(new InetSocketAddress(inetAddress, port));
    }

    public void openServerSocket(InetSocketAddress inetSocketAddress)
            throws Exception {
        openServerSocket(inetSocketAddress, new JAThreadFactory());
    }

    public void openServerSocket(InetSocketAddress inetSocketAddress, ThreadFactory threadFactory)
            throws Exception {
        this.threadFactory = threadFactory;
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, maxPacketSize);
        serverSocketChannel.bind(inetSocketAddress);
        thread = threadFactory.newThread(new Acceptor());
        thread.start();
    }

    protected AgentProtocol createServerOpened() throws Exception {
        AgentProtocol agentProtocol = new AgentProtocol();
        agentProtocol.initialize(getMailbox(), this);
        return agentProtocol;
    }

    public void acceptSocket(SocketChannel socketChannel) {
        try {
            AgentProtocol agentProtocol = createServerOpened();
            agentProtocol.serverOpen(socketChannel, maxPacketSize, this, threadFactory);
            agentProtocols.add(agentProtocol.getRemoteAddress(), agentProtocol);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void close() {
        thread.interrupt();
        try {
            serverSocketChannel.close();
        } catch (Exception ex) {
        }
    }

    public void closeAll() {
        close();
        Set<String> remoteAddresses = agentProtocols.keySet();
        if (remoteAddresses.isEmpty()) {
            return;
        }
        Iterator<String> sit = remoteAddresses.iterator();
        while (sit.hasNext()) {
            String remoteAddress = sit.next();
            while (true) {
                AgentProtocol agentProtocol = agentProtocols.getAny(remoteAddress);
                if (agentProtocol == null)
                    break;
                agentProtocol.close();
            }
        }
    }

    public void closed(AgentProtocol agentProtocol) {
        agentProtocols.remove(agentProtocol.getRemoteAddress(), agentProtocol);
    }

    class Acceptor implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, maxPacketSize);
                    socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
                    socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
                    (new AcceptSocket(socketChannel)).sendEvent(SocketManager.this);
                }
            } catch (ClosedByInterruptException cbie) {
            } catch (ClosedChannelException cce) {
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(1);
            }
        }
    }
}

class AcceptSocket extends Request<Object, SocketManager> {
    SocketChannel socketChannel;

    public AcceptSocket(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    @Override
    public boolean isTargetType(Actor targetActor) {
        return targetActor instanceof SocketManager;
    }

    @Override
    public void processRequest(JLPCActor targetActor, RP rp) throws Exception {
        ((SocketManager) targetActor).acceptSocket(socketChannel);
        rp.processResponse(null);
    }
}
