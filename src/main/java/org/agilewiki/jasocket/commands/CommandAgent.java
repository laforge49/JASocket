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

import org.agilewiki.jactor.ExceptionHandler;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.agentChannel.ShipAgent;
import org.agilewiki.jasocket.console.PrintlnAgent;
import org.agilewiki.jasocket.console.ReadLineAgent;
import org.agilewiki.jasocket.console.ReadPasswordAgent;
import org.agilewiki.jasocket.jid.PrintJid;
import org.agilewiki.jasocket.jid.agent.AgentJid;
import org.agilewiki.jasocket.jid.agent.StartAgent;
import org.agilewiki.jid.Jid;
import org.agilewiki.jid.scalar.vlens.string.StringJid;

import java.util.Iterator;

abstract public class CommandAgent extends AgentJid {
    protected PrintJid out;
    protected RP commandRP;
    private long requestId = -1L;

    private StringJid getOperatorJid() throws Exception {
        return (StringJid) _iGet(0);
    }

    private StringJid getIdJid() throws Exception {
        return (StringJid) _iGet(1);
    }

    public void setOperatorName(String operatorName) throws Exception {
        getOperatorJid().setValue(operatorName);
    }

    protected String getId() throws Exception {
        return getIdJid().getValue();
    }

    public void setId(String operatorName) throws Exception {
        getIdJid().setValue(operatorName);
    }

    protected String getOperatorName() throws Exception {
        return getOperatorJid().getValue();
    }

    public void configure(String operatorName, String id, String commandLine) throws Exception {
        setOperatorName(operatorName);
        setId(id);
    }

    protected Commands commands() {
        return (Commands) getAncestor(Commands.class);
    }

    protected Command getCommand(String name) {
        return commands().get(name);
    }

    protected Iterator<String> commandIterator() {
        return commands().iterator();
    }

    protected void println(String v) throws Exception {
        out.println(v);
    }

    abstract protected void process(RP<PrintJid> rp) throws Exception;

    @Override
    public void start(RP rp) throws Exception {
        if (getOperatorName() == null)
            throw new IllegalArgumentException("missing operator name");
        if (requestId > -1) {
            agentChannel().agents.put(requestId, this);
        }
        out = PrintJid.newPrintJid(this);
        commandRP = rp;
        process(rp);
    }

    public void _userInterrupt() throws Exception {
        setExceptionHandler(new ExceptionHandler() {
            @Override
            public void process(Exception exception) throws Exception {
                commandRP.processResponse(exception);
            }
        });
        userInterrupt();
    }

    public void userInterrupt() throws Exception {
        out.println("*** Command Interrupted ***");
        commandRP.processResponse(out);
    }

    @Override
    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public long getRequestId() {
        return requestId;
    }

    public void consolePrintln(String value,
                               final RP<String> rp) throws Exception {
        PrintlnAgent printlnAgent = (PrintlnAgent) JAFactory.newActor(
                this,
                JASocketFactories.PRINTLN_AGENT_FACTORY,
                getMailboxFactory().createAsyncMailbox(),
                agentChannelManager());
        printlnAgent.configure(getId(), value);
        RP<Jid> _rp = new RP<Jid>() {
            @Override
            public void processResponse(Jid response) throws Exception {
                rp.processResponse(null);
            }
        };
        if (agentChannel() == null) {
            StartAgent.req.send(this, printlnAgent, _rp);
            return;
        }
        ShipAgent shipAgent = new ShipAgent(printlnAgent);
        shipAgent.send(this, agentChannel(), _rp);
    }

    public void consoleReadLine(String prompt,
                                final RP<String> rp) throws Exception {
        ReadLineAgent readLineAgent = (ReadLineAgent) JAFactory.newActor(
                this,
                JASocketFactories.READ_LINE_AGENT_FACTORY,
                getMailboxFactory().createAsyncMailbox(),
                agentChannelManager());
        readLineAgent.configure(getId(), prompt);
        RP<Jid> _rp = new RP<Jid>() {
            @Override
            public void processResponse(Jid response) throws Exception {
                rp.processResponse(((StringJid) response).getValue());
            }
        };
        if (agentChannel() == null) {
            StartAgent.req.send(this, readLineAgent, _rp);
            return;
        }
        ShipAgent shipAgent = new ShipAgent(readLineAgent);
        shipAgent.send(this, agentChannel(), _rp);
    }

    public void consoleReadPassword(String prompt,
                                    final RP<String> rp) throws Exception {
        ReadPasswordAgent readPasswordAgent = (ReadPasswordAgent) JAFactory.newActor(
                this,
                JASocketFactories.READ_PASSWORD_AGENT_FACTORY,
                getMailboxFactory().createAsyncMailbox(),
                agentChannelManager());
        readPasswordAgent.configure(getId(), prompt);
        RP<Jid> _rp = new RP<Jid>() {
            @Override
            public void processResponse(Jid response) throws Exception {
                rp.processResponse(((StringJid) response).getValue());
            }
        };
        if (agentChannel() == null) {
            StartAgent.req.send(this, readPasswordAgent, _rp);
            return;
        }
        ShipAgent shipAgent = new ShipAgent(readPasswordAgent);
        shipAgent.send(this, agentChannel(), _rp);
    }
}
