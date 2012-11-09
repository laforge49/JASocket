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
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.concurrent.ConcurrentDupMap;
import org.agilewiki.jasocket.jid.ShipAgent;
import org.agilewiki.jasocket.jid.agent.AddResourceNameAgent;
import org.agilewiki.jasocket.jid.agent.AgentChannel;
import org.agilewiki.jid.Jid;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

public class AgentChannelManager extends JLPCActor {
    ServerSocketChannel serverSocketChannel;
    ThreadFactory threadFactory;
    Thread thread;
    public int maxPacketSize = 100000;
    ConcurrentDupMap<String, AgentChannel> agentProtocols = new ConcurrentDupMap<String, AgentChannel>();
    protected HashMap<String, Jid> localResources = new HashMap<String, Jid>();

    public Jid getLocalResource(String name) {
        return localResources.get(name);
    }

    public void removeLocalResource(String name, RP rp) throws Exception {
        rp.processResponse(localResources.remove(name));
    }

    public void putLocalResource(String name, Jid jid, RP rp) throws Exception {
        AddResourceNameAgent agent = (AddResourceNameAgent)
                JAFactory.newActor(this, JASocketFactories.ADD_RESOURCE_NAME_AGENT_FACTORY);
        agent.setResourceName(name);
        ShipAgent shipAgent = new ShipAgent(agent);
        Iterator<String> ksit = agentProtocols.keySet().iterator();
        while (ksit.hasNext()) {
            String remoteAddress = ksit.next();
            Set<AgentChannel> agentChannelSet = agentProtocols.getSet(remoteAddress);
            Iterator<AgentChannel> acit = agentChannelSet.iterator();
            while (acit.hasNext()) {
                AgentChannel agentChannel = acit.next();
                shipAgent.sendEvent(this, agentChannel);
            }
        }
        rp.processResponse(localResources.put(name, jid));
    }

    public AgentChannel localAgentProtocol(int port)
            throws Exception {
        InetAddress inetAddress = InetAddress.getLocalHost();
        return agentProtocol(new InetSocketAddress(inetAddress, port));
    }

    public AgentChannel agentProtocol(InetSocketAddress inetSocketAddress)
            throws Exception {
        return agentProtocol(inetSocketAddress, new JAThreadFactory());
    }

    public AgentChannel agentProtocol(InetSocketAddress inetSocketAddress, ThreadFactory threadFactory)
            throws Exception {
        InetAddress inetAddress = inetSocketAddress.getAddress();
        String remoteAddress = inetAddress.getHostAddress() + ":" + inetSocketAddress.getPort();
        AgentChannel agentChannel = agentProtocols.getAny(remoteAddress);
        if (agentChannel != null)
            return agentChannel;
        agentChannel = new AgentChannel();
        agentChannel.initialize(getMailboxFactory().createMailbox(), this);
        agentChannel.open(inetSocketAddress, maxPacketSize, this, threadFactory);
        agentProtocols.add(agentChannel.getRemoteAddress(), agentChannel);
        return agentChannel;
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

    protected AgentChannel createServerOpened() throws Exception {
        AgentChannel agentChannel = new AgentChannel();
        agentChannel.initialize(getMailbox(), this);
        return agentChannel;
    }

    public void acceptSocket(SocketChannel socketChannel) {
        try {
            AgentChannel agentChannel = createServerOpened();
            agentChannel.serverOpen(socketChannel, maxPacketSize, this, threadFactory);
            agentProtocols.add(agentChannel.getRemoteAddress(), agentChannel);
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
                AgentChannel agentChannel = agentProtocols.getAny(remoteAddress);
                if (agentChannel == null)
                    break;
                agentChannel.close();
            }
        }
    }

    public void closed(AgentChannel agentChannel) {
        agentProtocols.remove(agentChannel.getRemoteAddress(), agentChannel);
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
                    (new AcceptSocket(socketChannel)).sendEvent(AgentChannelManager.this);
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

class AcceptSocket extends Request<Object, AgentChannelManager> {
    SocketChannel socketChannel;

    public AcceptSocket(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    @Override
    public boolean isTargetType(Actor targetActor) {
        return targetActor instanceof AgentChannelManager;
    }

    @Override
    public void processRequest(JLPCActor targetActor, RP rp) throws Exception {
        ((AgentChannelManager) targetActor).acceptSocket(socketChannel);
        rp.processResponse(null);
    }
}
