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
package org.agilewiki.jasocket.commands;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.Mailbox;
import org.agilewiki.jactor.factory.ActorFactory;
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jactor.lpc.JLPCActor;

import java.util.Iterator;
import java.util.TreeMap;

public class Commands extends JLPCActor {
    private TreeMap<String, Command> commands = new TreeMap<String, Command>();
    private JAFactory jaFactory;

    public Command get(String name) {
        return commands.get(name);
    }

    public Iterator<String> iterator() {
        return commands.keySet().iterator();
    }

    @Override
    public void initialize(final Mailbox mailbox, Actor parent, ActorFactory factory) throws Exception {
        super.initialize(mailbox, parent, factory);
        jaFactory = (JAFactory) getAncestor(JAFactory.class);
        configure();
    }

    protected void cmd(String name, String description) {
        commands.put(name, new Command(description, null));
    }

    protected void cmd(String name, String description, String type) {
        commands.put(name, new Command(description, type));
    }

    protected void cmd(String name, String description, ActorFactory actorFactory) throws Exception {
        jaFactory.registerActorFactory(actorFactory);
        cmd(name, description, actorFactory.actorType);
    }

    protected void configure() throws Exception {
        cmd("help", "Displays this list of commands", HelpAgentFactory.fac);
        cmd("to", "Send a command to another node", ToAgentFactory.fac);
        cmd("channels", "List all the open channels to other nodes in the cluster", ChannelsAgentFactory.fac);
        cmd("registerResource", "Register a resource with the given name", RegisterResourceAgentFactory.fac);
        cmd("unregisterResource", "Unregister a resource with the given name", UnregisterResourceAgentFactory.fac);
        cmd("resources", "list all resources in the cluster", ResourcesAgentFactory.fac);
        cmd("halt", "Exit (only) the local node", HaltAgentFactory.fac);
        cmd("exception", "Throws an exception", ExceptionAgentFactory.fac);
    }
}
