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
package org.agilewiki.jasocket.node;

import org.agilewiki.jactor.MailboxFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.commands.Commands;
import org.agilewiki.jasocket.commands.ConsoleCommands;
import org.agilewiki.jasocket.discovery.Discovery;
import org.agilewiki.jasocket.server.AgentChannelManager;
import org.agilewiki.jasocket.sshd.DummyPasswordAuthenticator;
import org.agilewiki.jasocket.sshd.JASMailboxFactory;
import org.agilewiki.jasocket.sshd.JASShellFactory;
import org.apache.sshd.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

import java.net.InetAddress;
import java.net.NetworkInterface;

public class Node {
    private MailboxFactory mailboxFactory;
    private AgentChannelManager agentChannelManager;
    private SshServer sshd;

    public MailboxFactory mailboxFactory() {
        return mailboxFactory;
    }

    public AgentChannelManager agentChannelManager() {
        return agentChannelManager;
    }

    public SshServer sshServer() {
        return sshd;
    }

    public void process(String[] args) throws Exception {
        JASocketFactories factory = factory();
        openAgentChannelManager(clusterPort(args), commands(factory));
        startDiscovery();
        startKeepAlive();
        openSSD(ssdPort(args));
    }

    public Node(int threadCount) throws Exception {
        mailboxFactory = JASMailboxFactory.newMailboxFactory(threadCount, this);
    }

    public static void main(String[] args) throws Exception {
        Node node = new Node(100);
        try {
            node.process(args);
        } catch (Exception ex) {
            node.mailboxFactory.close();
            throw ex;
        }
    }

    protected int clusterPort(String[] args) throws Exception {
        int port = 8880;
        if (args.length > 0) {
            port = Integer.valueOf(args[0]);
        }
        return port;
    }

    protected JASocketFactories factory() throws Exception {
        JASocketFactories factory = new JASocketFactories();
        factory.initialize();
        return factory;
    }

    protected Commands commands(JASocketFactories factory) throws Exception {
        Commands commands = new ConsoleCommands();
        commands.initialize(factory);
        return commands;
    }

    protected void openAgentChannelManager(int clusterPort, Commands commands) throws Exception {
        agentChannelManager = new AgentChannelManager();
        agentChannelManager.maxPacketSize = 64000;
        agentChannelManager.initialize(mailboxFactory.createMailbox(), commands);
        agentChannelManager.openServerSocket(clusterPort);
    }

    protected void startDiscovery() throws Exception {
        new Discovery(
                agentChannelManager,
                NetworkInterface.getByInetAddress(InetAddress.getLocalHost()),
                "225.49.42.13",
                8887,
                2000);
    }

    protected void startKeepAlive() throws Exception {
        agentChannelManager.startKeepAlive(10000, 1000);
    }

    protected int ssdPort(String[] args) throws Exception {
        return clusterPort(args) + 1;
    }

    protected void openSSD(int ssdPort) throws Exception {
        sshd = SshServer.setUpDefaultServer();
        setAuthenticator();
        sshd.setPort(ssdPort);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
        setShellFactory();
        sshd.start();
    }

    protected void setAuthenticator() {
        sshd.setPasswordAuthenticator(new DummyPasswordAuthenticator());
    }

    protected void setShellFactory() {
        sshd.setShellFactory(new JASShellFactory(mailboxFactory));
    }
}
