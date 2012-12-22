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

import org.agilewiki.jactor.JAIterator;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jasocket.agentChannel.AgentChannel;
import org.agilewiki.jasocket.agentChannel.ShipAgent;
import org.agilewiki.jasocket.cluster.GetAgentChannel;
import org.agilewiki.jasocket.cluster.KeepAliveAgent;
import org.agilewiki.jasocket.cluster.KeepAliveAgentFactory;
import org.agilewiki.jasocket.jid.PrintJid;
import org.agilewiki.jasocket.jid.agent.StartAgent;

/**
 * >latencyTest 10.0.0.2:8880
 * elapsed time (ms): 2246
 * message count: 10000
 * latency (ns): 224600
 * >latencyTest 10.0.0.2:8880 100000
 * elapsed time (ms): 8411
 * message count: 100000
 * latency (ns): 84110      <- 84 microsecond latency
 */
public class LatencyBenchmarkAgent extends CommandStringAgent {
    @Override
    protected void process(final RP<PrintJid> rp) throws Exception {
        String address = getArgString();
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
        int count = 100000;
        if (argsString.length() > 0)
            count = Integer.valueOf(argsString);
        final int c = count;
        final KeepAliveAgent keepAliveAgent = (KeepAliveAgent)
                JAFactory.newActor(this, KeepAliveAgentFactory.fac.actorType, getMailbox(), agentChannelManager());

        if (isLocalAddress(address)) {
            final long t0 = System.currentTimeMillis();
            (new JAIterator() {
                int i = 0;

                @Override
                protected void process(RP responseProcessor) throws Exception {
                    if (i == c) {
                        long t1 = System.currentTimeMillis();
                        long d = t1 - t0;
                        println("elapsed time (ms): " + d);
                        println("message count: " + c);
                        println("latency (ns): " + (d * 1000000 / c));
                        responseProcessor.processResponse(out);
                    } else {
                        i += 1;
                        StartAgent.req.send(LatencyBenchmarkAgent.this, keepAliveAgent, responseProcessor);
                    }
                }
            }).iterate(rp);
            StartAgent.req.send(this, keepAliveAgent, (RP) rp);
            return;
        }

        final String a = address;
        (new GetAgentChannel(address)).send(this, agentChannelManager(), new RP<AgentChannel>() {
            @Override
            public void processResponse(final AgentChannel agentChannel) throws Exception {
                if (agentChannel == null) {
                    println("not an open channel: " + a);
                    rp.processResponse(out);
                    return;
                }
                final long t0 = System.currentTimeMillis();
                (new JAIterator() {
                    int i = 0;

                    @Override
                    protected void process(RP responseProcessor) throws Exception {
                        if (i == c) {
                            long t1 = System.currentTimeMillis();
                            long d = t1 - t0;
                            println("elapsed time (ms): " + d);
                            println("message count: " + c);
                            println("latency (ns): " + (d * 1000000 / c));
                            responseProcessor.processResponse(out);
                        } else {
                            i += 1;
                            ShipAgent shipAgent = new ShipAgent(keepAliveAgent);
                            shipAgent.send(LatencyBenchmarkAgent.this, agentChannel, responseProcessor);

                        }
                    }
                }).iterate(rp);
            }
        });
    }
}
