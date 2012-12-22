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
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.agentChannel.AgentChannelClosedException;
import org.agilewiki.jasocket.cluster.AgentChannelManager;
import org.agilewiki.jasocket.jid.PrintJid;
import org.agilewiki.jasocket.jid.agent.EvalAgent;
import org.agilewiki.jasocket.jid.agent.StartAgent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleApp {
    private Node node;
    protected BufferedReader inbr;

    protected String input() throws IOException {
        return inbr.readLine();
    }

    public void create(Node node) throws Exception {
        this.node = node;
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
                PrintJid out = (PrintJid) StartAgent.req.send(future, evalAgent);
                StringBuilder sb = new StringBuilder();
                out.appendto(sb);
                System.out.print(sb.toString());
            } catch (InterruptedException ex) {
            } catch (AgentChannelClosedException x) {
                System.out.println("Channel closed: " + x.getMessage());
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Node node = new Node(args, 100);
        try {
            node.process();
            (new ConsoleApp()).create(node);
        } catch (Exception ex) {
            node.mailboxFactory().close();
            throw ex;
        }
    }
}
