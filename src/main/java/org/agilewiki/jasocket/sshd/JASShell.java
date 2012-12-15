/*
 * Copyright 2011 Bill La Forge
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
package org.agilewiki.jasocket.sshd;

import jline.console.ConsoleReader;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.MailboxFactory;
import org.agilewiki.jactor.concurrent.ThreadManager;
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.agentChannel.AgentChannelClosedException;
import org.agilewiki.jasocket.jid.agent.EvalAgent;
import org.agilewiki.jasocket.jid.agent.StartAgent;
import org.agilewiki.jasocket.node.Node;
import org.agilewiki.jasocket.server.AgentChannelManager;
import org.agilewiki.jid.collection.vlenc.BListJid;
import org.agilewiki.jid.scalar.vlens.string.StringJid;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class JASShell implements Command {
    protected Node node;
    protected AgentChannelManager agentChannelManager;
    protected MailboxFactory mailboxFactory;
    protected ThreadManager threadManager;
    protected InputStream in;
    protected ConsoleReader consoleReader;
    protected PrintStream out;
    protected OutputStream outputStream;
    protected PrintStream err;
    protected ExitCallback exitCallback;
    protected Thread thread;
    protected Environment env;

    public JASShell(Node node) {
        this.node = node;
        agentChannelManager = node.agentChannelManager();
        mailboxFactory = node.mailboxFactory();
        threadManager = mailboxFactory.getThreadManager();
    }

    @Override
    public void setInputStream(InputStream in) {
        this.in = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        outputStream = out;
        this.out = new PrintStream(out, true);
    }

    @Override
    public void setErrorStream(OutputStream err) {
        this.err = new PrintStream(err, true);
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        this.exitCallback = callback;
    }

    @Override
    public void start(Environment env) throws IOException {
        this.env = env;
        threadManager.process(new Runnable() {
            @Override
            public void run() {
                try {
                    consoleReader = new ConsoleReader(in, outputStream);
                    thread = Thread.currentThread();
                    out.println(
                            "\n*** JASocket ConsoleApp " + agentChannelManager.agentChannelManagerAddress() + " ***\n");
                    JAFuture future = new JAFuture();
                    while (true) {
                        out.print(">");
                        String in = consoleReader.readLine();
                        EvalAgent evalAgent = (EvalAgent) JAFactory.newActor(
                                agentChannelManager,
                                JASocketFactories.EVAL_FACTORY,
                                node.mailboxFactory().createAsyncMailbox(),
                                agentChannelManager);
                        evalAgent.setArgString(in);
                        try {
                            BListJid<StringJid> outs = (BListJid) StartAgent.req.send(future, evalAgent);
                            int s = outs.size();
                            int i = 0;
                            while (i < s) {
                                out.println(outs.iGet(i).getValue());
                                i += 1;
                            }
                        } catch (InterruptedException ex) {
                        } catch (AgentChannelClosedException x) {
                            out.println("Channel closed: " + x.getMessage());
                        } catch (Exception x) {
                            x.printStackTrace();
                        }
                    }
                } catch (InterruptedException ex) {
                } catch (Exception ex) {
                    ex.printStackTrace(err);
                }
                exitCallback.onExit(0);
            }
        });
    }

    @Override
    public void destroy() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        thread.interrupt();
    }
}
