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
import org.agilewiki.jasocket.jid.agent.AgentChannel;
import org.agilewiki.jasocket.jid.agent.AgentJid;
import org.agilewiki.jasocket.resourceListener.ResourceListener;
import org.agilewiki.jid.Jid;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.StandardSocketOptions;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

public class AgentChannelManager extends JLPCActor {
    ServerSocketChannel serverSocketChannel;
    final ThreadFactory threadFactory = threadFactory();
    Thread thread;
    public int maxPacketSize = 100000;
    ConcurrentDupMap<String, AgentChannel> agentChannels = new ConcurrentDupMap<String, AgentChannel>();
    protected HashMap<String, Jid> localResources = new HashMap<String, Jid>();
    String agentChannelManagerAddress;
    private HashSet<String> resourceNames = new HashSet<String>();
    private HashSet<ResourceListener> resourceListeners = new HashSet<ResourceListener>();

    public boolean subscribeResourceNotifications(ResourceListener resourceListener) {
        return resourceListeners.add(resourceListener);
    }

    public boolean unsubscribeResourceNotifications(ResourceListener resourceListener) {
        return resourceListeners.remove(resourceListener);
    }

    public String agentChannelManagerAddress() throws Exception {
        if (agentChannelManagerAddress == null) {
            ServerSocket serverSocket = serverSocketChannel.socket();
            String host = serverSocket.getInetAddress().getHostAddress();
            int port = serverSocket.getLocalPort();
            agentChannelManagerAddress = host + ":" + port;
        }
        return agentChannelManagerAddress;
    }

    public boolean isAgentChannelManagerAddress(String address) throws Exception {
        return agentChannelManagerAddress().equals(address);
    }

    public Jid getLocalResource(String name) {
        return localResources.get(name);
    }

    protected void shipAgentEventToAll(AgentJid agent) throws Exception {
        ShipAgent shipAgent = new ShipAgent(agent);
        Iterator<String> ksit = agentChannels.keySet().iterator();
        while (ksit.hasNext()) {
            String remoteAddress = ksit.next();
            Set<AgentChannel> agentChannelSet = agentChannels.getSet(remoteAddress);
            Iterator<AgentChannel> acit = agentChannelSet.iterator();
            while (acit.hasNext()) {
                AgentChannel agentChannel = acit.next();
                shipAgent.sendEvent(this, agentChannel);
            }
        }
    }

    public Jid removeLocalResource(String name) throws Exception {
        Jid removed = localResources.remove(name);
        RemoveResourceNameAgent agent = (RemoveResourceNameAgent)
                JAFactory.newActor(this, JASocketFactories.REMOVE_RESOURCE_NAME_AGENT_FACTORY, getMailbox());
        agent.setResourceName(name);
        shipAgentEventToAll(agent);
        removeResourceName(agentChannelManagerAddress(), name);
        return removed;
    }

    public Jid putLocalResource(String name, Jid jid) throws Exception {
        Jid added = localResources.put(name, jid);
        AddResourceNameAgent agent = (AddResourceNameAgent)
                JAFactory.newActor(this, JASocketFactories.ADD_RESOURCE_NAME_AGENT_FACTORY, getMailbox());
        agent.setResourceName(name);
        shipAgentEventToAll(agent);
        addResourceName(agentChannelManagerAddress(), name);
        return added;
    }

    public void addResourceName(String address, String name) {
        resourceNames.add(address + " " + name);
    }

    public void removeResourceName(String address, String name) {
        resourceNames.remove(address + " " + name);
    }

    private void shareResourceNames(AgentChannel agentChannel) throws Exception {
        Iterator<String> it = localResources.keySet().iterator();
        while (it.hasNext()) {
            String name = it.next();
            AddResourceNameAgent agent = (AddResourceNameAgent)
                    JAFactory.newActor(this, JASocketFactories.ADD_RESOURCE_NAME_AGENT_FACTORY, getMailbox());
            agent.setResourceName(name);
            ShipAgent shipAgent = new ShipAgent(agent);
            shipAgent.sendEvent(this, agentChannel);
        }
    }

    protected ThreadFactory threadFactory() {
        return new JAThreadFactory();
    }

    public AgentChannel agentChannel(InetSocketAddress inetSocketAddress)
            throws Exception {
        InetAddress inetAddress = inetSocketAddress.getAddress();
        String remoteAddress = inetAddress.getHostAddress() + ":" + inetSocketAddress.getPort();
        AgentChannel agentChannel = agentChannels.getAny(remoteAddress);
        if (agentChannel != null)
            return agentChannel;
        agentChannel = new AgentChannel();
        agentChannel.initialize(getMailboxFactory().createMailbox(), this);
        agentChannel.open(inetSocketAddress, maxPacketSize, threadFactory);
        remoteAddress = agentChannel.remoteAddress();
        SetClientAddressAgent agent = (SetClientAddressAgent)
                JAFactory.newActor(this, JASocketFactories.SET_CLIENT_ADDRESS_AGENT_FACTORY, getMailbox());
        agent.setRemoteAddress(remoteAddress);
        (new ShipAgent(agent)).sendEvent(this, agentChannel);
        agentChannels.add(remoteAddress, agentChannel);
        shareResourceNames(agentChannel);
        return agentChannel;
    }

    public void openServerSocket(int port) throws Exception {
        InetAddress inetAddress = InetAddress.getLocalHost();
        openServerSocket(new InetSocketAddress(inetAddress, port));
    }

    public void openServerSocket(InetSocketAddress inetSocketAddress)
            throws Exception {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, maxPacketSize);
        serverSocketChannel.bind(inetSocketAddress);
        thread = threadFactory.newThread(new Acceptor());
        thread.start();
    }

    public void acceptSocket(SocketChannel socketChannel) throws Exception {
        AgentChannel agentChannel = new AgentChannel();
        agentChannel.initialize(getMailbox(), this);
        agentChannel.serverOpen(socketChannel, maxPacketSize, threadFactory);
    }

    public void setClientAddress(AgentChannel agentChannel, String remoteAddress) throws Exception {
        agentChannel.setClientAddress(remoteAddress);
        agentChannels.add(remoteAddress, agentChannel);
        shareResourceNames(agentChannel);
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
        Set<String> remoteAddresses = agentChannels.keySet();
        if (remoteAddresses.isEmpty()) {
            return;
        }
        Iterator<String> sit = remoteAddresses.iterator();
        while (sit.hasNext()) {
            String remoteAddress = sit.next();
            while (true) {
                AgentChannel agentChannel = agentChannels.getAny(remoteAddress);
                if (agentChannel == null)
                    break;
                agentChannel.close();
            }
        }
    }

    public void agentChannelClosed(AgentChannel agentChannel, RP rp) throws Exception {
        String remoteAddress = agentChannel.remoteAddress();
        if (remoteAddress != null) {
            agentChannels.remove(remoteAddress, agentChannel);
            HashSet<String> channelResources = new HashSet<String>();
            Iterator<String> it = resourceNames.iterator();
            String prefix = remoteAddress + " ";
            int offset = prefix.length();
            while (it.hasNext()) {
                String name = it.next();
                if (name.startsWith(prefix)) {
                    channelResources.add(name.substring(offset));
                }
            }
            it = channelResources.iterator();
            while (it.hasNext()) {
                String name = it.next();
                removeResourceName(remoteAddress, name);
            }
        }
        rp.processResponse(null);
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
