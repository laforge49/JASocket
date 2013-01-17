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
import org.agilewiki.jasocket.cluster.AgentChannelManager;
import org.agilewiki.jasocket.console.Interpret;
import org.agilewiki.jasocket.console.Interpreter;
import org.agilewiki.jasocket.console.Interrupter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleApp {
    private Node node;
    protected BufferedReader inbr;

    protected String input() throws IOException {
        return inbr.readLine();
    }

    public void create(Node node, Interrupter interrupter) throws Exception {
        Interpreter interpreter = new Interpreter();
        interpreter.initialize(node.mailboxFactory().createAsyncMailbox());
        interpreter.configure("*console*", node, System.out);
        if (interrupter != null)
            interrupter.activate(interpreter);
        this.node = node;
        AgentChannelManager agentChannelManager = node.agentChannelManager();
        System.out.println(
                "\n*** JASocket ConsoleApp " + agentChannelManager.agentChannelManagerAddress() + " ***\n");
        inbr = new BufferedReader(new InputStreamReader(System.in));
        JAFuture future = new JAFuture();
        boolean first = true;
        while (true) {
            String commandLine = null;
            while (commandLine == null) {
                if (first)
                    System.out.print(">");
                else
                    System.out.print("\n>");
                commandLine = input();
                first = false;
            }
            (new Interpret(commandLine)).send(future, interpreter);
        }
    }

    public static void main(String[] args) throws Exception {
        Node node = new Node(args, 100);
        try {
            node.process();
            (new ConsoleApp()).create(node, null);
        } catch (Exception ex) {
            node.mailboxFactory().close();
            throw ex;
        }
    }
}
