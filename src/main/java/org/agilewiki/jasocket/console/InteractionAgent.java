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
import org.agilewiki.jactor.JAIterator;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.jid.agent.AgentJid;
import org.agilewiki.jasocket.jid.agent.StartAgent;
import org.agilewiki.jid.Jid;
import org.agilewiki.jid.collection.vlenc.BListJid;
import org.agilewiki.jid.scalar.vlens.string.StringJid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class InteractionAgent extends AgentJid {
    protected BufferedReader inbr;

    @Override
    public void start(RP rp) throws Exception {
        System.out.println("\n*** JASocket Test Console " + agentChannelManager().agentChannelManagerAddress() + " ***\n");
        inbr = new BufferedReader(new InputStreamReader(System.in));
        (new JAIterator() {
            @Override
            protected void process(final RP responseProcessor) throws Exception {
                System.out.print(">");
                String in = input();
                EvalAgent evalAgent = (EvalAgent)
                        JAFactory.newActor(InteractionAgent.this, JASocketFactories.EVAL_FACTORY, getMailbox(), agentChannelManager());
                evalAgent.setEvalString(in);
                setExceptionHandler(new ExceptionHandler() {
                    @Override
                    public void process(Exception exception) throws Exception {
                        exception.printStackTrace();
                        responseProcessor.processResponse(null);
                    }
                });
                StartAgent.req.send(InteractionAgent.this, evalAgent, new RP<Jid>() {
                    @Override
                    public void processResponse(Jid response) throws Exception {
                        BListJid<StringJid> out = (BListJid) response;
                        int s = out.size();
                        int i = 0;
                        while (i < s) {
                            System.out.println(out.iGet(i).getValue());
                            i += 1;
                        }
                        responseProcessor.processResponse(null);
                    }
                });
            }
        }).iterate(rp);
    }

    protected String input() throws IOException {
        return inbr.readLine();
    }
}
