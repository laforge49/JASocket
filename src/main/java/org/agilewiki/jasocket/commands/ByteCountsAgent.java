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
package org.agilewiki.jasocket.commands;

import org.agilewiki.jactor.ExceptionHandler;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.agentChannel.AgentChannel;
import org.agilewiki.jasocket.agentChannel.ShipAgent;
import org.agilewiki.jasocket.cluster.ByteCountserAgent;
import org.agilewiki.jasocket.cluster.Channels;
import org.agilewiki.jasocket.cluster.GetAgentChannel;
import org.agilewiki.jasocket.jid.PrintJid;

import java.util.Iterator;
import java.util.TreeSet;

public class ByteCountsAgent extends CommandAgent {
    private final TreeSet<String> ts = new TreeSet<String>();
    private int expecting;

    @Override
    public void process(final RP<PrintJid> rp) throws Exception {
        final ByteCountserAgent byteCountserAgent = (ByteCountserAgent) JAFactory.newActor(
                this,
                JASocketFactories.BYTE_COUNTSER_AGENT_FACTORY,
                getMailbox(),
                agentChannelManager());
        final ShipAgent shipAgent = new ShipAgent(byteCountserAgent);
        Channels.req.send(this, agentChannelManager(), new RP<TreeSet<String>>() {
            @Override
            public void processResponse(TreeSet<String> addresses) throws Exception {
                expecting = addresses.size() + 1;
                final RP byteCountsRP = new RP() {
                    @Override
                    public void processResponse(Object response) throws Exception {
                        if (response != null) {
                            PrintJid o = (PrintJid) response;
                            int s = o.size();
                            int i = 0;
                            while (i < s) {
                                ts.add(o.iGet(i).getValue());
                                i += 1;
                            }
                        }
                        expecting -= 1;
                        if (expecting > 0)
                            return;
                        respond();
                        rp.processResponse(out);
                    }
                };
                setExceptionHandler(new ExceptionHandler() {
                    @Override
                    public void process(Exception exception) throws Exception {
                        byteCountsRP.processResponse(null);
                    }
                });
                Iterator<String> ita = addresses.iterator();
                while (ita.hasNext()) {
                    String address = ita.next();
                    (new GetAgentChannel(address)).
                            send(ByteCountsAgent.this, agentChannelManager(), new RP<AgentChannel>() {
                                @Override
                                public void processResponse(AgentChannel response) throws Exception {
                                    if (response == null)
                                        byteCountsRP.processResponse(null);
                                    else
                                        shipAgent.send(ByteCountsAgent.this, response, byteCountsRP);
                                }
                            });
                }
                byteCountserAgent._start(byteCountsRP);
            }
        });
    }

    private void respond() throws Exception {
        Iterator<String> it = ts.iterator();
        while (it.hasNext()) {
            out.println(it.next());
        }
    }

    public void userInterrupt() throws Exception {
        respond();
        out.println("*** ByteCounts Interrupted ***");
        out.println("No response from " + expecting + " nodes.");
        startRP.processResponse(out);
    }
}
