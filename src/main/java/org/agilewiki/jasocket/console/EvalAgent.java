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

import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.jid.ShipAgent;
import org.agilewiki.jasocket.jid.agent.AgentChannel;
import org.agilewiki.jasocket.jid.agent.AgentJid;
import org.agilewiki.jasocket.jid.agent.StartAgent;
import org.agilewiki.jasocket.server.RegisterResource;
import org.agilewiki.jasocket.server.Resources;
import org.agilewiki.jasocket.server.UnregisterResource;
import org.agilewiki.jid.Jid;
import org.agilewiki.jid.JidFactories;
import org.agilewiki.jid.collection.vlenc.BListJid;
import org.agilewiki.jid.scalar.vlens.string.StringJid;

import java.util.Iterator;
import java.util.TreeSet;

public class EvalAgent extends AgentJid {
    BListJid<StringJid> out;

    private Commands commands() {
        return agentChannelManager().commands;
    }

    private Command getCommand(String name) {
        return commands().get(name);
    }

    private Iterator<String> commandIterator() {
        return commands().iterator();
    }

    private StringJid getStringJid() throws Exception {
        return (StringJid) _iGet(0);
    }

    public void setEvalString(String name) throws Exception {
        getStringJid().setValue(name);
    }

    private String commandString() throws Exception {
        return getStringJid().getValue();
    }

    private void println(String v) throws Exception {
        out.iAdd(-1);
        StringJid sj = out.iGet(-1);
        sj.setValue(v);
    }

    @Override
    public void start(RP rp) throws Exception {
        out = (BListJid<StringJid>) JAFactory.newActor(this, JidFactories.STRING_BLIST_JID_TYPE, getMailbox());
        String in = commandString().trim();
        int i = in.indexOf(' ');
        String rem = "";
        if (i > -1) {
            rem = in.substring(i + 1);
            in = in.substring(0, i);
        }
        rem = rem.trim();
        Command cmd = getCommand(in);
        if (cmd == null) {
            println("No such command: " + in + ". (Use the help command for a list of commands.)");
            rp.processResponse(out);
            return;
        }
        String type = cmd.type();
        if (type == null) {
            if (in.equals("channels"))
                channels(rem, rp);
            else if (in.equals("registerResource"))
                registerResource(rem, rp);
            else if (in.equals("unregisterResource"))
                unregisterResource(rem, rp);
            else if (in.equals("resources"))
                resources(rem, rp);
            else if (in.equals("halt"))
                halt(rem, rp);
        } else {
            ConsoleAgent agent = (ConsoleAgent) JAFactory.newActor(this, type, getMailbox(), agentChannelManager());
            agent.setCommandLine(rem);
            StartAgent.req.send(this, agent, rp);
        }
    }

    protected void resources(String rem, final RP rp) throws Exception {
        Resources.req.send(this, agentChannelManager(), new RP<TreeSet<String>>() {
            @Override
            public void processResponse(TreeSet<String> response) throws Exception {
                Iterator<String> it = response.iterator();
                while (it.hasNext()) {
                    println(it.next());
                }
                rp.processResponse(out);
            }
        });
    }

    protected void channels(String rem, RP rp) throws Exception {
        Iterator<String> it = agentChannelManager().channels().iterator();
        while (it.hasNext()) {
            String address = it.next();
            if (agentChannelManager().isActive(address))
                println(address);
        }
        rp.processResponse(out);
    }

    protected void registerResource(String rem, final RP rp) throws Exception {
        int p = rem.indexOf(' ');
        if (p > -1)
            rem = rem.substring(0, p).trim();
        if (rem.length() == 0) {
            println("missing resource name");
            rp.processResponse(out);
            return;
        }
        final String r = rem;
        (new RegisterResource(r, new Jid())).send(this, agentChannelManager(), new RP<Boolean>() {
            @Override
            public void processResponse(Boolean response) throws Exception {
                if (response)
                    println("registered resource " + r);
                else
                    println("a resource named " + r + " was already registred");
                rp.processResponse(out);
            }
        });
    }

    protected void unregisterResource(String rem, final RP rp) throws Exception {
        int p = rem.indexOf(' ');
        if (p > -1)
            rem = rem.substring(0, p).trim();
        if (rem.length() == 0) {
            println("missing resource name");
            rp.processResponse(out);
            return;
        }
        final String r = rem;
        (new UnregisterResource(r)).send(this, agentChannelManager(), new RP<JLPCActor>() {
            @Override
            public void processResponse(JLPCActor response) throws Exception {
                if (response != null)
                    println("unregistered resource " + r);
                else
                    println("a resource named " + r + " was not registred");
                rp.processResponse(out);
            }
        });
    }

    protected void halt(String rem, RP rp) throws Exception {
        int p = rem.indexOf(' ');
        if (p > -1)
            rem = rem.substring(0, p);
        if (rem.length() == 0) {
            println("missing channel name");
            rp.processResponse(out);
            return;
        }
        AgentChannel agentChannel = agentChannelManager().getAgentChannel(rem);
        if (agentChannel == null) {
            println("not an open channel: " + rem);
            return;
        }
        HaltAgent haltAgent = (HaltAgent) JAFactory.newActor(this, JASocketFactories.HALT_FACTORY, getMailbox());
        ShipAgent shipAgent = new ShipAgent(haltAgent);
        shipAgent.sendEvent(agentChannel);
        rp.processResponse(out);
    }
}
