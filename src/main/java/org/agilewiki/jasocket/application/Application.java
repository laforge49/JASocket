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
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jasocket.DuplicateResourceException;
import org.agilewiki.jasocket.node.Node;
import org.agilewiki.jasocket.server.AgentChannelManager;
import org.agilewiki.jasocket.server.RegisterResource;
import org.agilewiki.jasocket.server.UnregisterResource;
import org.agilewiki.jid.JidFactories;
import org.agilewiki.jid.collection.vlenc.BListJid;
import org.agilewiki.jid.scalar.vlens.string.StringJid;

abstract public class Application extends JLPCActor implements Closable {
    private Node node;

    abstract protected String applicationName();

    protected Node node() {
        return node;
    }

    protected AgentChannelManager agentChannelManager() {
        return node.agentChannelManager();
    }

    public void startUp(Node node, final RP rp) throws Exception {
        this.node = node;
        RegisterResource registerResource = new RegisterResource(applicationName(), this);
        registerResource.send(this, agentChannelManager(), new RP<Boolean>() {
            @Override
            public void processResponse(Boolean response) throws Exception {
                if (response)
                    registered(rp);
                else
                    throw new DuplicateResourceException();
            }
        });
    }

    abstract protected void registered(RP rp) throws Exception;

    public void close() {
        UnregisterResource unregisterResource = new UnregisterResource(applicationName());
        try {
            unregisterResource.sendEvent(agentChannelManager());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void eval(String commandString, BListJid<StringJid> out, RP rp) throws Exception {
        commandString = commandString.trim();
        int i = commandString.indexOf(' ');
        String command = commandString.substring(0, i);
        String args = "";
        if (i > -1)
            args = commandString.substring(i + 1).trim();
        eval(command, args, out, rp);
    }

    protected void println(BListJid<StringJid> out, String v) throws Exception {
        out.iAdd(-1);
        StringJid sj = out.iGet(-1);
        sj.setValue(v);
    }

    protected void eval(String command, String args, BListJid<StringJid> out, RP<BListJid<StringJid>> rp) throws Exception {
        if (command.equals("close")) {
            close();
            println(out, "Closed");
            rp.processResponse(out);
        } else {
            throw new IllegalArgumentException("Unrecognized command: " + command);
        }
    }
}
