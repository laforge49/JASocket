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
package org.agilewiki.jasocket.console;

import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jactor.factory.ActorFactory;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.discovery.Discovery;
import org.agilewiki.jasocket.jid.ShipAgent;
import org.agilewiki.jasocket.jid.agent.AgentChannel;
import org.agilewiki.jasocket.server.*;
import org.agilewiki.jid.Jid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

public class Console {
    protected BufferedReader inbr;
    protected Commands commands;
    protected String[] args;
    protected JASocketFactories factory;
    protected AgentChannelManager agentChannelManager;
    protected JAFuture future = new JAFuture();

    protected int maxThreadCount() {
        return 100;
    }

    protected void process(String[] args) throws Exception {
        JAMailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(maxThreadCount());
        try {
            this.args = args;
            int port = 8880;
            if (args.length > 0) {
                port = Integer.valueOf(args[0]);
            }
            factory = new JASocketFactories();
            factory.initialize();
            commands = new Commands();
            commands.initialize(factory);
            agentChannelManager = new AgentChannelManager();
            agentChannelManager.initialize(mailboxFactory.createMailbox(), factory);
            agentChannelManager.openServerSocket(port);
            agentChannelManager.commands = commands;
            new Discovery(agentChannelManager);
            agentChannelManager.startKeepAlive(5000, 2000);
            System.out.println("\n*** JASocket Test Console " + agentChannelManager.agentChannelManagerAddress() + " ***\n");
            inbr = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.print(">");
                String in = input();
                int i = in.indexOf(' ');
                String rem = "";
                if (i > -1) {
                    rem = in.substring(i + 1);
                    in = in.substring(0, i);
                }
                Command cmd = commands.get(in);
                if (cmd == null) {
                    System.out.println("No such command: " + in + ". (Use the help command for a list of commands.)");
                    continue;
                }
                String type = cmd.type();
                if (type == null) {
                    if (in.equals("exit"))
                        return;
                    else if (in.equals("help"))
                        help();
                    else if (in.equals("channels"))
                        channels();
                    else if (in.equals("registerResource"))
                        registerResource(rem);
                    else if (in.equals("unregisterResource"))
                        unregisterResource(rem);
                    else if (in.equals("resources"))
                        resources();
                    else if (in.equals("halt"))
                        halt(rem);
                }
            }
        } finally {
            mailboxFactory.close();
        }
    }

    protected void resources() throws Exception {
        TreeSet<String> resources = Resources.req.send(future, agentChannelManager);
        Iterator<String> it = resources.iterator();
        while (it.hasNext()) {
            System.out.println(it.next());
        }
    }

    protected void help() {
        Iterator<String> it = commands.iterator();
        while (it.hasNext()) {
            String name = it.next();
            Command c = commands.get(name);
            System.out.println(name + " - " + c.description());
        }
    }

    protected void channels() {
        Iterator<String> it = agentChannelManager.channels().iterator();
        while (it.hasNext()) {
            String address = it.next();
            if (agentChannelManager.isActive(address))
                System.out.println(address);
        }
    }

    protected void registerResource(String rem) throws Exception {
        int p = rem.indexOf(' ');
        if (p > -1)
            rem = rem.substring(0, p);
        if (rem.length() == 0) {
            System.out.println("missing resource name");
            return;
        }
        boolean newResource = (new RegisterResource(rem, new Jid())).send(future, agentChannelManager);
        if (newResource)
            System.out.println("registered resource " + rem);
        else
            System.out.println("a resource named " + rem + " was already registred");
    }

    protected void unregisterResource(String rem) throws Exception {
        int p = rem.indexOf(' ');
        if (p > -1)
            rem = rem.substring(0, p);
        if (rem.length() == 0) {
            System.out.println("missing resource name");
            return;
        }
        JLPCActor oldResource = (new UnregisterResource(rem)).send(future, agentChannelManager);
        if (oldResource != null)
            System.out.println("unregistered resource " + rem);
        else
            System.out.println("a resource named " + rem + " was not registred");
    }

    protected void halt(String rem) throws Exception {
        int p = rem.indexOf(' ');
        if (p > -1)
            rem = rem.substring(0, p);
        if (rem.length() == 0) {
            System.out.println("missing channel name");
            return;
        }
        AgentChannel agentChannel = agentChannelManager.getAgentChannel(rem);
        if (agentChannel == null) {
            System.out.println("not an open channel: " + rem);
            return;
        }
        HaltAgent haltAgent = (HaltAgent) factory.newActor(JASocketFactories.HALT_FACTORY, agentChannel.getMailbox());
        ShipAgent shipAgent = new ShipAgent(haltAgent);
        shipAgent.sendEvent(agentChannel);
    }

    protected String input() throws IOException {
        return inbr.readLine();
    }

    public static void main(String[] args) throws Exception {
        (new Console()).process(args);
    }
}
