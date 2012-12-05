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

import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jactor.MailboxFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.commands.Commands;
import org.agilewiki.jasocket.commands.ConsoleCommands;
import org.agilewiki.jasocket.discovery.Discovery;
import org.agilewiki.jasocket.server.AgentChannelManager;

import java.net.InetAddress;
import java.net.NetworkInterface;

public class Node {
    public MailboxFactory mailboxFactory;
    protected int port;
    public String[] args;
    public JASocketFactories factory;
    protected Commands commands;
    public AgentChannelManager agentChannelManager;

    protected void initializePort() throws Exception {
        port = 8880;
        if (args.length > 0) {
            port = Integer.valueOf(args[0]);
        }
    }

    protected void initializeFactory() throws Exception {
        factory = new JASocketFactories();
        factory.initialize();
    }

    protected void initializeCommands() throws Exception {
        commands = new ConsoleCommands();
        commands.initialize(factory);
    }

    protected void initializeAgentChannelManager() throws Exception {
        agentChannelManager = new AgentChannelManager();
        agentChannelManager.initialize(mailboxFactory.createMailbox(), factory);
        agentChannelManager.openServerSocket(port);
        agentChannelManager.commands = commands;
    }

    protected void initializeDiscovery() throws Exception {
        new Discovery(
                agentChannelManager,
                NetworkInterface.getByInetAddress(InetAddress.getLocalHost()),
                "225.49.42.13",
                8887,
                2000);
    }

    protected void initializeKeepAlive() throws Exception {
        agentChannelManager.startKeepAlive(10000, 1000);
    }

    public void process() throws Exception {
        initializePort();
        initializeFactory();
        initializeCommands();
        initializeAgentChannelManager();
        initializeDiscovery();
        initializeKeepAlive();
    }

    public Node(String[] args, int threadCount) throws Exception {
        this.args = args;
        mailboxFactory = JAMailboxFactory.newMailboxFactory(threadCount);
    }

    public static void main(String[] args) throws Exception {
        Node node = new Node(args, 100);
        try {
            node.process();
        } catch (Exception ex) {
            node.mailboxFactory.close();
            throw ex;
        }
    }
}
