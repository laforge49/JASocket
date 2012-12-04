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
import org.agilewiki.jactor.Mailbox;
import org.agilewiki.jactor.MailboxFactory;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.concurrent.ThreadManager;
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.agentChannel.AgentChannel;
import org.agilewiki.jasocket.agentChannel.CloseChannel;
import org.agilewiki.jasocket.agentChannel.ShipAgent;
import org.agilewiki.jasocket.commands.Commands;
import org.agilewiki.jasocket.concurrent.ConcurrentDupMap;
import org.agilewiki.jasocket.jid.agent.AgentJid;
import org.agilewiki.jasocket.resourceListener.ResourceAdded;
import org.agilewiki.jasocket.resourceListener.ResourceListener;
import org.agilewiki.jasocket.resourceListener.ResourceRemoved;
import org.agilewiki.jid.CopyJID;
import org.agilewiki.jid.Jid;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.StandardSocketOptions;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AgentChannelManager extends JLPCActor {
    public Commands commands;
    ServerSocketChannel serverSocketChannel;
    public int maxPacketSize = 64000;
    ConcurrentDupMap<String, AgentChannel> agentChannels = new ConcurrentDupMap<String, AgentChannel>();
    protected HashMap<String, JLPCActor> localResources = new HashMap<String, JLPCActor>();
    String agentChannelManagerAddress;
    private HashSet<String> resourceNames = new HashSet<String>();
    private HashSet<ResourceListener> resourceListeners = new HashSet<ResourceListener>();
    private Set<String> inactiveReceivers = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private Set<String> inactiveSenders = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    public void received(String address) {
        inactiveReceivers.remove(address);
    }

    public void sent(String address) {
        inactiveSenders.remove(address);
    }

    public Set<String> channels() {
        return agentChannels.keySet();
    }

    public boolean isActive(String address) {
        return agentChannels.getAny(address) != null;
    }

    public void resetActive(Set<String> set) {
        set.clear();
        Iterator<String> it = agentChannels.keySet().iterator();
        while (it.hasNext()) {
            String address = it.next();
            if (isActive(address))
                set.add(address);
        }
    }

    public void startKeepAlive(final long readTimeout, final long keepaliveTimeout) throws Exception {
        final ThreadManager threadManager = getMailboxFactory().getThreadManager();

        threadManager.process(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    resetActive(inactiveReceivers);
                    try {
                        Thread.sleep(readTimeout);
                    } catch (InterruptedException e) {
                        return;
                    }
                    Iterator<String> it = inactiveReceivers.iterator();
                    while (it.hasNext()) {
                        String address = it.next();
                        AgentChannel agentChannel = agentChannels.getAny(address);
                        if (agentChannel != null) {
                            try {
                                CloseChannel.req.sendEvent(agentChannel);
                            } catch (Exception x) {
                            }
                        }
                    }
                }
            }
        });

        KeepAliveAgent keepAliveAgent = (KeepAliveAgent)
                JAFactory.newActor(this, JASocketFactories.KEEP_ALIVE_FACTORY, getMailbox());
        final ShipAgent shipKeepAlive = new ShipAgent(keepAliveAgent);
        threadManager.process(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    resetActive(inactiveSenders);
                    try {
                        Thread.sleep(keepaliveTimeout);
                    } catch (InterruptedException e) {
                        return;
                    }
                    Iterator<String> it = inactiveSenders.iterator();
                    while (it.hasNext()) {
                        String address = it.next();
                        AgentChannel agentChannel = agentChannels.getAny(address);
                        if (agentChannel != null) {
                            try {
                                shipKeepAlive.sendEvent(agentChannel);
                            } catch (Exception e) {
                                threadManager.logException(false, "shipping keepalive", e);
                            }
                        }
                    }
                }
            }
        });
    }

    public TreeSet<String> resources() {
        return new TreeSet<String>(resourceNames);
    }

    public List<String> locateResource(String name) {
        Iterator<String> it = resourceNames.iterator();
        List<String> addresses = new ArrayList<String>();
        String postfix = " " + name;
        while (it.hasNext()) {
            String rn = it.next();
            if (rn.endsWith(postfix)) {
                int pos = rn.indexOf(" ");
                if (pos + postfix.length() == rn.length()) {
                    addresses.add(rn.substring(0, pos));
                }
            }
        }
        return addresses;
    }

    public boolean subscribeResourceNotifications(ResourceListener resourceListener) throws Exception {
        boolean subscribed = resourceListeners.add(resourceListener);
        if (subscribed) {
            Iterator<String> it = resourceNames.iterator();
            while (it.hasNext()) {
                String resourceName = it.next();
                int p = resourceName.indexOf(" ");
                String address = resourceName.substring(0, p);
                String name = resourceName.substring(p + 1);
                ResourceAdded resourceAdded = new ResourceAdded(address, name);
                resourceAdded.sendEvent(this, resourceListener);
            }
        }
        return subscribed;
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

    public int agentChannelManagerPort() throws Exception {
        String address = agentChannelManagerAddress();
        int pos = address.indexOf(':');
        return Integer.valueOf(address.substring(pos + 1));
    }

    public boolean isLocalAddress(String address) throws Exception {
        if (agentChannelManagerAddress().equals(address))
            return true;
        return getLocalResource(address) != null;
    }

    public JLPCActor getLocalResource(String name) {
        return localResources.get(name);
    }

    public void copyResource(String address, String name, final RP rp) throws Exception {
        if (agentChannelManagerAddress().equals(address)) {
            Jid resource = (Jid) getLocalResource(name);
            if (resource == null) {
                rp.processResponse(null);
                return;
            }
            Mailbox mailbox = null;
            if (resource instanceof AgentJid) {
                AgentJid agent = (AgentJid) resource;
                if (agent.async())
                    mailbox = getMailboxFactory().createAsyncMailbox();
                else
                    mailbox = getMailboxFactory().createMailbox();
            } else {
                mailbox = getMailboxFactory().createMailbox();
            }
            (new CopyJID(mailbox)).send(this, resource, rp);
            return;
        }
        final GetLocalResourceAgent agent = (GetLocalResourceAgent)
                JAFactory.newActor(this, JASocketFactories.GET_LOCAL_RESOURCE_AGENT_FACTORY, getMailbox());
        agent.setResourceName(name);
        agentChannel(address, new RP() {
            @Override
            public void processResponse(Object response) throws Exception {
                (new ShipAgent(agent)).send(AgentChannelManager.this, (AgentChannel) response, rp);

            }
        });
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

    public JLPCActor unregisterResource(String name) throws Exception {
        JLPCActor removed = localResources.remove(name);
        if (removed == null)
            return null;
        RemoveResourceNameAgent agent = (RemoveResourceNameAgent)
                JAFactory.newActor(this, JASocketFactories.REMOVE_RESOURCE_NAME_AGENT_FACTORY, getMailbox());
        agent.setResourceName(name);
        shipAgentEventToAll(agent);
        removeResourceName(agentChannelManagerAddress(), name);
        return removed;
    }

    public boolean registerResource(String name, JLPCActor resource) throws Exception {
        JLPCActor added = localResources.get(name);
        if (added != null)
            return false;
        localResources.put(name, resource);
        AddResourceNameAgent agent = (AddResourceNameAgent)
                JAFactory.newActor(this, JASocketFactories.ADD_RESOURCE_NAME_AGENT_FACTORY, getMailbox());
        agent.setResourceName(name);
        shipAgentEventToAll(agent);
        addResourceName(agentChannelManagerAddress(), name);
        return true;
    }

    public void addResourceName(String address, String name) throws Exception {
        String rn = address + " " + name;
        if (!resourceNames.add(rn))
            return;
        resourceNames.add(rn);
        ResourceAdded resourceAdded = new ResourceAdded(address, name);
        Iterator<ResourceListener> it = resourceListeners.iterator();
        while (it.hasNext()) {
            resourceAdded.sendEvent(this, it.next());
        }
    }

    public void removeResourceName(String address, String name) throws Exception {
        if (!resourceNames.remove(address + " " + name))
            return;
        ResourceRemoved resourceRemoved = new ResourceRemoved(address, name);
        Iterator<ResourceListener> it = resourceListeners.iterator();
        while (it.hasNext()) {
            resourceRemoved.sendEvent(this, it.next());
        }
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

    public AgentChannel getAgentChannel(String address) throws Exception {
        List<String> locations = locateResource(address);
        if (locations.size() > 0)
            address = locations.get(0);
        return agentChannels.getAny(address);
    }

    public void agentChannel(String address, RP rp) throws Exception {
        int i = address.indexOf(":");
        String host = address.substring(0, i);
        int port = Integer.valueOf(address.substring(i + 1));
        InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
        agentChannel(inetSocketAddress, rp);
    }

    public void agentChannel(InetSocketAddress inetSocketAddress, final RP rp)
            throws Exception {
        InetAddress inetAddress = inetSocketAddress.getAddress();
        final String remoteAddress = inetAddress.getHostAddress() + ":" + inetSocketAddress.getPort();
        AgentChannel oldAgentChannel = agentChannels.getAny(remoteAddress);
        if (oldAgentChannel != null) {
            rp.processResponse(oldAgentChannel);
            return;
        }
        final AgentChannel agentChannel = new AgentChannel();
        agentChannel.initialize(getMailboxFactory().createAsyncMailbox(), this);
        agentChannel.open(inetSocketAddress, maxPacketSize);
        SetClientPortAgent agent = (SetClientPortAgent)
                JAFactory.newActor(this, JASocketFactories.SET_CLIENT_PORT_AGENT_FACTORY, getMailbox());
        agent.setRemotePort(agentChannelManagerPort());
        (new ShipAgent(agent)).send(this, agentChannel, new RP<Jid>() {
            @Override
            public void processResponse(Jid response) throws Exception {
                agentChannels.add(agentChannel.remoteAddress(), agentChannel);
                AgentChannel someAgentChannel = agentChannels.getAny(remoteAddress);
                shareResourceNames(someAgentChannel);
                rp.processResponse(someAgentChannel);
            }
        });
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
        getMailboxFactory().getThreadManager().process(new Acceptor());
    }

    public void acceptSocket(SocketChannel socketChannel) throws Exception {
        AgentChannel agentChannel = new AgentChannel();
        agentChannel.initialize(getMailboxFactory().createAsyncMailbox(), this);
        agentChannel.serverOpen(socketChannel, maxPacketSize);
    }

    public void setClientPort(AgentChannel agentChannel, int port) throws Exception {
        agentChannel.setClientPort(port);
        String remoteAddress = agentChannel.remoteAddress();
        agentChannels.add(remoteAddress, agentChannel);
        shareResourceNames(agentChannel);
    }

    public void close() {
        try {
            serverSocketChannel.close();
        } catch (Exception ex) {
        }
    }

    public void agentChannelClosed(AgentChannel agentChannel, RP rp) throws Exception {
        String remoteAddress = agentChannel.remoteAddress();
        if (remoteAddress != null) {
            agentChannels.remove(remoteAddress, agentChannel);
            inactiveSenders.remove(remoteAddress);
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
                MailboxFactory mailboxFactory = getMailboxFactory();
                mailboxFactory.logException(true, "Server socket accept process threw unexpected exception", ex);
                mailboxFactory.close();
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
