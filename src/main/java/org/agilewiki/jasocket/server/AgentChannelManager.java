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
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.agentChannel.AgentChannel;
import org.agilewiki.jasocket.agentChannel.CloseChannel;
import org.agilewiki.jasocket.agentChannel.ShipAgent;
import org.agilewiki.jasocket.jid.agent.AgentJid;
import org.agilewiki.jasocket.node.Node;
import org.agilewiki.jasocket.applicationListener.ApplicationNameAdded;
import org.agilewiki.jasocket.applicationListener.ApplicationNameListener;
import org.agilewiki.jasocket.applicationListener.ApplicationRemoved;
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
    public Node node;
    ServerSocketChannel serverSocketChannel;
    public int maxPacketSize;
    HashMap<String, List<AgentChannel>> agentChannels = new HashMap<String, List<AgentChannel>>();
    protected HashMap<String, JLPCActor> localApplications = new HashMap<String, JLPCActor>();
    String agentChannelManagerAddress;
    private HashSet<String> applicationNames = new HashSet<String>();
    private HashSet<ApplicationNameListener> applicationNameListeners = new HashSet<ApplicationNameListener>();
    private Set<AgentChannel> inactiveReceivers = Collections.newSetFromMap(new ConcurrentHashMap<AgentChannel, Boolean>());
    private Set<AgentChannel> activeReceivers = Collections.newSetFromMap(new ConcurrentHashMap<AgentChannel, Boolean>());
    private Set<String> inactiveSenders = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    public void received(AgentChannel agentChannel) {
        inactiveReceivers.remove(agentChannel);
        activeReceivers.add(agentChannel);
    }

    public void sent(String address) {
        inactiveSenders.remove(address);
    }

    public TreeSet<String> channels() {
        TreeSet<String> channels = new TreeSet<String>();
        Iterator<String> it = agentChannels.keySet().iterator();
        while (it.hasNext()) {
            String address = it.next();
            if (agentChannels.containsKey(address))
                channels.add(address);
        }
        return channels;
    }

    public AgentChannel getAgentChannel(String address) throws Exception {
        List<String> locations = locateApplication(address);
        if (locations.size() > 0)
            address = locations.get(0);
        List<AgentChannel> dups = agentChannels.get(address);
        if (dups == null)
            return null;
        return dups.get(0);
    }

    public void readTimeout() throws Exception {
        Iterator<AgentChannel> it = inactiveReceivers.iterator();
        while (it.hasNext()) {
            AgentChannel agentChannel = it.next();
            CloseChannel.req.sendEvent(agentChannel);
        }
        inactiveReceivers.clear();
        Iterator<String> it2 = agentChannels.keySet().iterator();
        while (it2.hasNext()) {
            List<AgentChannel> dup = agentChannels.get(it2.next());
            inactiveReceivers.addAll(dup);
        }
    }

    public void writeTimeout() throws Exception {
        Iterator<AgentChannel> ait = activeReceivers.iterator();
        while (ait.hasNext()) {
            AgentChannel agentChannel = ait.next();
            String remoteAddress = agentChannel.remoteAddress;
            List<AgentChannel> dup = agentChannels.get(remoteAddress);
            if (dup == null)
                continue;
            int i = dup.indexOf(agentChannel);
            if (i > 0) {
                dup.remove(i);
                dup.add(0, agentChannel);
            }
        }
        KeepAliveAgent keepAliveAgent = (KeepAliveAgent)
                JAFactory.newActor(this, JASocketFactories.KEEP_ALIVE_FACTORY, getMailbox());
        ShipAgent shipKeepAlive = new ShipAgent(keepAliveAgent);
        Iterator<String> it = inactiveSenders.iterator();
        while (it.hasNext()) {
            String address = it.next();
            AgentChannel agentChannel = getAgentChannel(address);
            if (agentChannel != null) {
                shipKeepAlive.sendEvent(agentChannel);
            }
        }
        inactiveSenders.clear();
        Iterator<String> it2 = channels().iterator();
        while (it2.hasNext()) {
            String address = it2.next();
            inactiveSenders.add(address);
        }
    }

    public void startKeepAlive(final long readTimeout, final long keepaliveTimeout) throws Exception {
        Timer timer = getMailboxFactory().timer();

        TimerTask rtt = new TimerTask() {
            @Override
            public void run() {
                try {
                    ReadTimeout.req.sendEvent(AgentChannelManager.this);
                } catch (Exception x) {
                }
            }
        };
        timer.scheduleAtFixedRate(rtt, readTimeout, readTimeout);

        TimerTask ktt = new TimerTask() {
            @Override
            public void run() {
                try {
                    WriteTimeout.req.sendEvent(AgentChannelManager.this);
                } catch (Exception x) {
                }
            }
        };
        timer.scheduleAtFixedRate(ktt, keepaliveTimeout, keepaliveTimeout);
    }

    public TreeSet<String> applicationNames() {
        return new TreeSet<String>(applicationNames);
    }

    public List<String> locateApplication(String name) {
        Iterator<String> it = applicationNames.iterator();
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

    public boolean subscribeApplicationNameNotifications(ApplicationNameListener applicationNameListener) throws Exception {
        boolean subscribed = applicationNameListeners.add(applicationNameListener);
        if (subscribed) {
            Iterator<String> it = applicationNames.iterator();
            while (it.hasNext()) {
                String applicationName = it.next();
                int p = applicationName.indexOf(" ");
                String address = applicationName.substring(0, p);
                String name = applicationName.substring(p + 1);
                ApplicationNameAdded applicationNameAdded = new ApplicationNameAdded(address, name);
                applicationNameAdded.sendEvent(this, applicationNameListener);
            }
        }
        return subscribed;
    }

    public boolean unsubscribeApplicationNameNotifications(ApplicationNameListener applicationNameListener) {
        return applicationNameListeners.remove(applicationNameListener);
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
        return getLocalApplication(address) != null;
    }

    public JLPCActor getLocalApplication(String name) {
        return localApplications.get(name);
    }

    public void copyApplication(String address, String name, final RP rp) throws Exception {
        if (agentChannelManagerAddress().equals(address)) {
            Jid application = (Jid) getLocalApplication(name);
            if (application == null) {
                rp.processResponse(null);
                return;
            }
            Mailbox mailbox = null;
            if (application instanceof AgentJid) {
                AgentJid agent = (AgentJid) application;
                if (agent.async())
                    mailbox = getMailboxFactory().createAsyncMailbox();
                else
                    mailbox = getMailboxFactory().createMailbox();
            } else {
                mailbox = getMailboxFactory().createMailbox();
            }
            (new CopyJID(mailbox)).send(this, application, rp);
            return;
        }
        final GetLocalApplicationAgent agent = (GetLocalApplicationAgent)
                JAFactory.newActor(this, JASocketFactories.GET_LOCAL_APPLICATION_AGENT_FACTORY, getMailbox());
        agent.setApplicationName(name);
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
            AgentChannel agentChannel = agentChannels.get(remoteAddress).get(0);
            shipAgent.sendEvent(this, agentChannel);
        }
    }

    public JLPCActor unregisterApplication(String name) throws Exception {
        JLPCActor removed = localApplications.remove(name);
        if (removed == null)
            return null;
        RemoveApplicationNameAgent agent = (RemoveApplicationNameAgent)
                JAFactory.newActor(this, JASocketFactories.REMOVE_REMOTE_APPLICATION_NAME_AGENT_FACTORY, getMailbox());
        agent.setApplicationName(name);
        shipAgentEventToAll(agent);
        removeApplicationName(agentChannelManagerAddress(), name);
        return removed;
    }

    public boolean registerApplication(String name, JLPCActor application) throws Exception {
        JLPCActor added = localApplications.get(name);
        if (added != null)
            return false;
        localApplications.put(name, application);
        AddRemoteApplicationNameAgent agent = (AddRemoteApplicationNameAgent)
                JAFactory.newActor(this, JASocketFactories.ADD_REMOTE_APPLICATION_NAME_AGENT_FACTORY, getMailbox());
        agent.setApplicationName(name);
        shipAgentEventToAll(agent);
        addRemoteApplicationName(agentChannelManagerAddress(), name);
        return true;
    }

    public void addRemoteApplicationName(String address, String name) throws Exception {
        String rn = address + " " + name;
        if (!applicationNames.add(rn))
            return;
        applicationNames.add(rn);
        ApplicationNameAdded applicationNameAdded = new ApplicationNameAdded(address, name);
        Iterator<ApplicationNameListener> it = applicationNameListeners.iterator();
        while (it.hasNext()) {
            applicationNameAdded.sendEvent(this, it.next());
        }
    }

    public void removeApplicationName(String address, String name) throws Exception {
        if (!applicationNames.remove(address + " " + name))
            return;
        ApplicationRemoved applicationRemoved = new ApplicationRemoved(address, name);
        Iterator<ApplicationNameListener> it = applicationNameListeners.iterator();
        while (it.hasNext()) {
            applicationRemoved.sendEvent(this, it.next());
        }
    }

    private void shareApplicationNames(AgentChannel agentChannel) throws Exception {
        Iterator<String> it = localApplications.keySet().iterator();
        while (it.hasNext()) {
            String name = it.next();
            AddRemoteApplicationNameAgent agent = (AddRemoteApplicationNameAgent)
                    JAFactory.newActor(this, JASocketFactories.ADD_REMOTE_APPLICATION_NAME_AGENT_FACTORY, getMailbox());
            agent.setApplicationName(name);
            ShipAgent shipAgent = new ShipAgent(agent);
            shipAgent.sendEvent(this, agentChannel);
        }
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
        List<AgentChannel> dups = agentChannels.get(remoteAddress);
        if (dups != null) {
            rp.processResponse(dups.get(0));
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
                List<AgentChannel> dups = agentChannels.get(remoteAddress);
                if (dups == null) {
                    dups = new ArrayList<AgentChannel>();
                    agentChannels.put(remoteAddress, dups);
                }
                dups.add(0, agentChannel);
                AgentChannel someAgentChannel = dups.get(0);
                shareApplicationNames(someAgentChannel);
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
        List<AgentChannel> dups = agentChannels.get(remoteAddress);
        if (dups == null) {
            dups = new ArrayList<AgentChannel>();
            agentChannels.put(remoteAddress, dups);
        }
        dups.add(0, agentChannel);
        shareApplicationNames(agentChannel);
    }

    public void close() {
        try {
            serverSocketChannel.close();
        } catch (Exception ex) {
        }
    }

    public void agentChannelClosed(AgentChannel agentChannel, RP rp) throws Exception {
        String remoteAddress = agentChannel.remoteAddress();
        if (remoteAddress == null)
            rp.processResponse(null);
        List<AgentChannel> dups = agentChannels.get(remoteAddress);
        if (dups != null) {
            dups.remove(agentChannel);
            if (dups.isEmpty())
                agentChannels.remove(remoteAddress);
        }
        inactiveSenders.remove(remoteAddress);
        HashSet<String> channelAppliations = new HashSet<String>();
        Iterator<String> it = applicationNames.iterator();
        String prefix = remoteAddress + " ";
        int offset = prefix.length();
        while (it.hasNext()) {
            String name = it.next();
            if (name.startsWith(prefix)) {
                channelAppliations.add(name.substring(offset));
            }
        }
        it = channelAppliations.iterator();
        while (it.hasNext()) {
            String name = it.next();
            removeApplicationName(remoteAddress, name);
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
