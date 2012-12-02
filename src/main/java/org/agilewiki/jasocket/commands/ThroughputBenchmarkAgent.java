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
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.agentChannel.AgentChannel;
import org.agilewiki.jasocket.agentChannel.ShipAgent;
import org.agilewiki.jasocket.jid.agent.EvalAgent;
import org.agilewiki.jasocket.jid.agent.StartAgent;
import org.agilewiki.jasocket.server.KeepAliveAgent;
import org.agilewiki.jasocket.server.KeepAliveAgentFactory;
import org.agilewiki.jid.Jid;

/**
 * >latencyTest 10.0.0.2:8880
 * elapsed time (ms): 2246
 * message count: 10000
 * latency (ns): 224600
 * >latencyTest 10.0.0.2:8880 100000
 * elapsed time (ms): 8411
 * message count: 100000
 * latency (ns): 84110
 */
public class ThroughputBenchmarkAgent extends ConsoleStringAgent {
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
        String rem = "";
        int p1 = argsString.indexOf(' ');
        if (p1 > -1) {
            rem = argsString.substring(p1 + 1).trim();
            argsString = argsString.substring(0, p1).trim();
        }
        int count = 10;
        if (argsString.length() > 0)
            count = Integer.valueOf(argsString);
        final int c = count;
        int batch = 10;
        if (rem.length() > 0)
            batch = Integer.valueOf(rem);
        final int b = batch;
        final KeepAliveAgent keepAliveAgent = (KeepAliveAgent)
                JAFactory.newActor(this, KeepAliveAgentFactory.fac.actorType, getMailbox(), agentChannelManager());

        if (address.equals(agentChannelManager().agentChannelManagerAddress())) {
            final long t0 = System.currentTimeMillis();
            (new JAIterator() {
                int i = 0;

                @Override
                protected void process(final RP responseProcessor) throws Exception {
                    if (i == c) {
                        long t1 = System.currentTimeMillis();
                        long d = t1 - t0;
                        println("elapsed time (ms): " + d);
                        println("message count: " + c * b);
                        println("latency (ns): " + (d*1000000/c/b));
                        responseProcessor.processResponse(out);
                    } else {
                        i += 1;
                        RP brp = new RP() {
                            int j = 0;
                            @Override
                            public void processResponse(Object response) throws Exception {
                                j += 1;
                                if (j == b)
                                    responseProcessor.processResponse(null);
                            }
                        };
                        int k = 0;
                        while (k < b) {
                            k += 1;
                            StartAgent.req.send(ThroughputBenchmarkAgent.this, keepAliveAgent, brp);
                        }
                    }
                }
            }).iterate(rp);
            StartAgent.req.send(this, keepAliveAgent, rp);
            return;
        }

        final AgentChannel agentChannel = agentChannelManager().getAgentChannel(address);
        if (agentChannel == null) {
            println("not an open channel: " + address);
            rp.processResponse(out);
            return;
        }
        final long t0 = System.currentTimeMillis();
        (new JAIterator() {
            int i = 0;

            @Override
            protected void process(final RP responseProcessor) throws Exception {
                if (i == c) {
                    long t1 = System.currentTimeMillis();
                    long d = t1 - t0;
                    println("elapsed time (ms): " + d);
                    println("message count: " + c * b);
                    println("latency (ns): " + (d*1000000/c/b));
                    responseProcessor.processResponse(out);
                } else {
                    i += 1;
                    RP brp = new RP() {
                        int j = 0;
                        @Override
                        public void processResponse(Object response) throws Exception {
                            j += 1;
                            if (j == b)
                                responseProcessor.processResponse(null);
                        }
                    };
                    int k = 0;
                    while (k < b) {
                        k += 1;
                        ShipAgent shipAgent = new ShipAgent(keepAliveAgent);
                        shipAgent.send(ThroughputBenchmarkAgent.this, agentChannel, brp);
                    }

                }
            }
        }).iterate(rp);
    }
}
