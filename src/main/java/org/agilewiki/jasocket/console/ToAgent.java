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

import org.agilewiki.jactor.ExceptionHandler;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.jid.ShipAgent;
import org.agilewiki.jasocket.jid.agent.AgentChannel;
import org.agilewiki.jasocket.jid.agent.StartAgent;
import org.agilewiki.jid.Jid;

public class ToAgent extends ConsoleStringAgent {
    @Override
    protected void process(final RP rp) throws Exception {
        String address = getCommandLineString();
        String argsString = "";
        int p = address.indexOf(' ');
        if (p > -1) {
            argsString = address.substring(p + 1).trim();
            address = address.substring(0, p).trim();
        }
        if (address.length() == 0) {
            println("missing channel name");
            rp.processResponse(out);
            return;
        }
        EvalAgent evalAgent = (EvalAgent)
                JAFactory.newActor(this, JASocketFactories.EVAL_FACTORY, getMailbox(), agentChannelManager());
        evalAgent.setEvalString(argsString);
        if (address.equals(agentChannelManager().agentChannelManagerAddress())) {
            StartAgent.req.send(this, evalAgent, rp);
            return;
        }
        AgentChannel agentChannel = agentChannelManager().getAgentChannel(address);
        if (agentChannel == null) {
            println("not an open channel: " + address);
            rp.processResponse(out);
            return;
        }
        ShipAgent shipAgent = new ShipAgent(evalAgent);
        setExceptionHandler(new ExceptionHandler() {
            @Override
            public void process(Exception exception) throws Exception {
                System.err.println("ToAgent: exception caught");
                println("an exception occurred");
                rp.processResponse(out);
            }
        });
        shipAgent.send(this, agentChannel, new RP<Jid>() {
            @Override
            public void processResponse(Jid response) throws Exception {
                rp.processResponse(response);
            }
        });
    }
}
