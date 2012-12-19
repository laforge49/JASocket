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
package org.agilewiki.jasocket.application;

import org.agilewiki.jactor.Closable;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jasocket.node.Node;
import org.agilewiki.jasocket.server.AgentChannelManager;
import org.agilewiki.jasocket.server.RegisterResource;
import org.agilewiki.jasocket.server.UnregisterResource;
import org.agilewiki.jid.collection.vlenc.BListJid;
import org.agilewiki.jid.scalar.vlens.string.StringJid;

import java.util.Iterator;
import java.util.TreeMap;

abstract public class Application extends JLPCActor implements Closable {
    private Node node;
    protected TreeMap<String, ApplicationCommand> applicationCommands = new TreeMap<String, ApplicationCommand>();
    protected String startupArgs;

    protected String applicationName() {
        return this.getClass().getName();
    }

    protected Node node() {
        return node;
    }

    protected AgentChannelManager agentChannelManager() {
        return node.agentChannelManager();
    }

    protected void registerApplicationCommand(ApplicationCommand applicationCommand) {
        applicationCommands.put(applicationCommand.name, applicationCommand);
    }

    public void startUp(Node node, final String args, final BListJid<StringJid> out, final RP rp) throws Exception {
        this.node = node;
        this.startupArgs = args;
        RegisterResource registerResource = new RegisterResource(applicationName(), this);
        registerResource.send(this, agentChannelManager(), new RP<Boolean>() {
            @Override
            public void processResponse(Boolean response) throws Exception {
                if (response)
                    startApplication(out, rp);
                else
                    println(out, "Application already registered: " + applicationName());
                rp.processResponse(out);
            }
        });
    }

    protected void startApplication(BListJid<StringJid> out, RP rp) throws Exception {
        registerCloseCommand();
        registerHelpCommand();
        println(out, applicationName() + " started");
    }

    public void close() {
        UnregisterResource unregisterResource = new UnregisterResource(applicationName());
        try {
            unregisterResource.sendEvent(agentChannelManager());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void evalApplicationCommand(String commandString, BListJid<StringJid> out, RP rp) throws Exception {
        commandString = commandString.trim();
        int i = commandString.indexOf(' ');
        String command = commandString;
        String args = "";
        if (i > -1) {
            command = commandString.substring(0, i);
            args = commandString.substring(i + 1).trim();
        }
        ApplicationCommand applicationCommand = applicationCommands.get(command);
        if (applicationCommand == null) {
            println(out, "Unknown command for " + applicationName() + ": " + command);
            rp.processResponse(out);
            return;
        }
        applicationCommand.eval(args, out, rp);
    }

    protected void println(BListJid<StringJid> out, String v) throws Exception {
        out.iAdd(-1);
        StringJid sj = out.iGet(-1);
        sj.setValue(v);
    }

    protected void registerCloseCommand() {
        registerApplicationCommand(new ApplicationCommand("close", "Closes the application") {
            @Override
            public void eval(String args, BListJid<StringJid> out, RP rp) throws Exception {
                close();
                println(out, "Closed " + applicationName());
                rp.processResponse(out);
            }
        });
    }

    protected void registerHelpCommand() {
        registerApplicationCommand(new ApplicationCommand("help", "List the commands supported by the application") {
            @Override
            public void eval(String args, BListJid<StringJid> out, RP rp) throws Exception {
                Iterator<String> it = applicationCommands.keySet().iterator();
                while (it.hasNext()) {
                    ApplicationCommand ac = applicationCommands.get(it.next());
                    println(out, ac.name + " - " + ac.description);
                }
                rp.processResponse(out);
            }
        });
    }
}
