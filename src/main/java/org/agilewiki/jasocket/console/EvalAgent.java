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
import org.agilewiki.jasocket.jid.agent.AgentJid;
import org.agilewiki.jasocket.jid.agent.StartAgent;
import org.agilewiki.jid.JidFactories;
import org.agilewiki.jid.collection.vlenc.BListJid;
import org.agilewiki.jid.scalar.vlens.string.StringJid;

import java.util.Iterator;

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
        if (!isLocal()) {
            System.out.print("from " + agentChannel().remoteAddress + ">" + commandString() + "\n>");
        }
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
        ConsoleAgent agent = (ConsoleAgent)
                JAFactory.newActor(this, type, getMailboxFactory().createAsyncMailbox(), agentChannelManager());
        agent.setCommandLineString(rem);
        StartAgent.req.send(this, agent, rp);
    }
}
