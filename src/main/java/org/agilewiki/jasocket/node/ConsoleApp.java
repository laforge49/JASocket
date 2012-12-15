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
package org.agilewiki.jasocket.node;

import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jasocket.JASApplication;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.agentChannel.AgentChannelClosedException;
import org.agilewiki.jasocket.jid.agent.EvalAgent;
import org.agilewiki.jasocket.jid.agent.StartAgent;
import org.agilewiki.jasocket.server.AgentChannelManager;
import org.agilewiki.jid.collection.vlenc.BListJid;
import org.agilewiki.jid.scalar.vlens.string.StringJid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleApp implements JASApplication {
    private Node node;
    protected BufferedReader inbr;

    @Override
    public void create(Node node, String[] args) throws Exception {
        this.node = node;
    }

    protected String input() throws IOException {
        return inbr.readLine();
    }

    @Override
    public void open() throws Exception {
        AgentChannelManager agentChannelManager = node.agentChannelManager();
        System.out.println(
                "\n*** JASocket ConsoleApp " + agentChannelManager.agentChannelManagerAddress() + " ***\n");
        inbr = new BufferedReader(new InputStreamReader(System.in));
        JAFuture future = new JAFuture();
        while (true) {
            System.out.print(">");
            String in = input();
            EvalAgent evalAgent = (EvalAgent) JAFactory.newActor(
                    agentChannelManager,
                    JASocketFactories.EVAL_FACTORY,
                    node.mailboxFactory().createAsyncMailbox(),
                    agentChannelManager);
            evalAgent.setArgString(in);
            try {
                BListJid<StringJid> out = (BListJid) StartAgent.req.send(future, evalAgent);
                int s = out.size();
                int i = 0;
                while (i < s) {
                    System.out.println(out.iGet(i).getValue());
                    i += 1;
                }
            } catch (InterruptedException ex) {
            } catch (AgentChannelClosedException x) {
                System.out.println("Channel closed: " + x.getMessage());
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
    }
    @Override
    public void close() {}

    public static void main(String[] args) throws Exception {
        Node node = new Node(100);
        try {
            ConsoleApp consoleApp = new ConsoleApp();
            node.addApplication(consoleApp);
            node.process(args);
        } catch (Exception ex) {
            node.mailboxFactory().close();
            throw ex;
        }
    }
}
