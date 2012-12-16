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
import org.agilewiki.jasocket.Closable;
import org.agilewiki.jasocket.JASMailboxFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.commands.Commands;
import org.agilewiki.jasocket.commands.ConsoleCommands;
import org.agilewiki.jasocket.discovery.Discovery;
import org.agilewiki.jasocket.server.AgentChannelManager;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Node {
    private String[] args;
    private MailboxFactory mailboxFactory;
    private AgentChannelManager agentChannelManager;
    private File nodeDirectory;
    private List<Closable> closables = new ArrayList<Closable>();
    private JASocketFactories factory;

    public String[] args() {
        return args;
    }

    public void addClosable(Closable closable) {
        closables.add(closable);
    }

    public MailboxFactory mailboxFactory() {
        return mailboxFactory;
    }

    public AgentChannelManager agentChannelManager() {
        return agentChannelManager;
    }

    public File nodeDirectory() {
        return nodeDirectory;
    }

    public JASocketFactories factory() throws Exception {
        if (factory == null) {
            factory = new JASocketFactories();
            factory.initialize();
        }
        return factory;
    }

    public void process() throws Exception {
        factory();
        setNodeDirectory(args);
        openAgentChannelManager(clusterPort(args), commands(factory));
        startDiscovery();
        startKeepAlive();
    }

    public Node(String[] args, int threadCount) throws Exception {
        mailboxFactory = JASMailboxFactory.newMailboxFactory(threadCount, this);
        this.args = args;
    }

    public void close() {
        Iterator<Closable> it = closables.iterator();
        while (it.hasNext())
            it.next().close();
    }

    public static void main(String[] args) throws Exception {
        Node node = new Node(args, 100);
        try {
            node.process();
        } catch (Exception ex) {
            node.mailboxFactory().close();
            throw ex;
        }
    }

    protected void setNodeDirectory(String[] args) throws Exception {
        int port = clusterPort(args);
        nodeDirectory = FileSystems.getDefault().getPath("node" + port).toFile();
        if (nodeDirectory.exists())
            return;
        if (!nodeDirectory.mkdir())
            throw new IOException("unable to create directory " + nodeDirectory.getPath());
    }

    protected int clusterPort(String[] args) throws Exception {
        int port = 8880;
        if (args.length > 0) {
            port = Integer.valueOf(args[0]);
        }
        return port;
    }

    protected Commands commands(JASocketFactories factory) throws Exception {
        Commands commands = new ConsoleCommands();
        commands.initialize(factory);
        return commands;
    }

    protected void openAgentChannelManager(int clusterPort, Commands commands) throws Exception {
        agentChannelManager = new AgentChannelManager();
        agentChannelManager.maxPacketSize = 64000;
        agentChannelManager.initialize(mailboxFactory.createAsyncMailbox(), commands);
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
}
