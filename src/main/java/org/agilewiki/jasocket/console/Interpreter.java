/*
 * Copyright 2013 Bill La Forge
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

import org.agilewiki.jactor.ExceptionHandler;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.agentChannel.AgentChannelClosedException;
import org.agilewiki.jasocket.cluster.AgentChannelManager;
import org.agilewiki.jasocket.jid.PrintJid;
import org.agilewiki.jasocket.jid.agent.EvalAgent;
import org.agilewiki.jasocket.jid.agent.StartAgent;
import org.agilewiki.jasocket.node.Node;
import org.agilewiki.jid.Jid;

import java.io.PrintStream;

public class Interpreter extends JLPCActor {
    private String operatorName;
    private Node node;
    private PrintStream ps;
    private AgentChannelManager agentChannelManager;

    private int commandCount;
    private long startTime;
    private long lastTime;
    private RP _rp;

    public String getOperatorName() {
        return operatorName;
    }

    public int getCommandCount() {
        return commandCount;
    }

    public long getLogonTime() {
        return System.currentTimeMillis() - startTime;
    }

    public long getIdleTime() {
        return System.currentTimeMillis() - lastTime;
    }

    public void configure(String operatorName, Node node, PrintStream out) throws Exception {
        this.operatorName = operatorName;
        this.node = node;
        this.ps = out;
        agentChannelManager = node.agentChannelManager();
        startTime = System.currentTimeMillis();
        lastTime = startTime;
    }

    public void interpret(String commandLine, RP rp) throws Exception {
        _rp = rp;
        lastTime = System.currentTimeMillis();
        EvalAgent evalAgent = (EvalAgent) JAFactory.newActor(
                agentChannelManager,
                JASocketFactories.EVAL_FACTORY,
                getMailboxFactory().createAsyncMailbox(),
                agentChannelManager);
        evalAgent.configure(operatorName, commandLine);
        setExceptionHandler(new ExceptionHandler() {
            @Override
            public void process(Exception exception) throws Exception {
                if (_rp == null)
                    return;
                if (exception instanceof InterruptedException) {
                } else if (exception instanceof AgentChannelClosedException) {
                    ps.println("Channel closed: " + exception.getMessage());
                } else {
                    exception.printStackTrace(ps);
                }
                _rp.processResponse(null);

            }
        });
        commandCount += 1;
        StartAgent.req.send(this, evalAgent, new RP<Jid>() {
            @Override
            public void processResponse(Jid response) throws Exception {
                if (_rp == null)
                    return;
                PrintJid out = (PrintJid) response;
                ps.println(out.toString());
                _rp.processResponse(null);
                _rp = null;
            }
        });
    }

    public void interrupt() throws Exception {
        if (_rp != null) {
            ps.println("*** Interrupted ***");
            _rp.processResponse(null);
            _rp = null;
        }
    }

    public void prompt() {
        ps.print((commandCount + 1) + ">");
    }
}
