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
import org.agilewiki.jasocket.cluster.AgentChannelManager;
import org.agilewiki.jasocket.jid.PrintJid;
import org.agilewiki.jasocket.jid.agent.EvalAgent;
import org.agilewiki.jasocket.jid.agent.StartAgent;
import org.agilewiki.jasocket.node.Node;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    protected SSHServer sshServer;
    protected String operatorName;
    protected int commandCount;
    protected long startTime;
    protected long lastTime;
    protected ConcurrentLinkedQueue<String> notices = new ConcurrentLinkedQueue<String>();

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

    public void notice(String n) {
        notices.add(n);
        while (notices.size() > 0 && consoleReader.getCursorBuffer().length() == 0) {
            n = notices.poll();
            if (n != null)
                out.println(n);
        }
    }

    public JASShell(SSHServer sshServer, Node node) {
        this.sshServer = sshServer;
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
        operatorName = env.getEnv().get(env.ENV_USER);
        startTime = System.currentTimeMillis();
        lastTime = startTime;
        sshServer.shells.add(this);
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
                        while (notices.size() > 0) {
                            String n = notices.poll();
                            if (n != null)
                                out.println(n);
                        }
                        out.print((commandCount + 1) + ">");
                        String in = consoleReader.readLine();
                        lastTime = System.currentTimeMillis();
                        EvalAgent evalAgent = (EvalAgent) JAFactory.newActor(
                                agentChannelManager,
                                JASocketFactories.EVAL_FACTORY,
                                node.mailboxFactory().createAsyncMailbox(),
                                agentChannelManager);
                        evalAgent.configure(operatorName, in);
                        try {
                            commandCount += 1;
                            PrintJid outs = (PrintJid) StartAgent.req.send(future, evalAgent);
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
        sshServer.shells.remove(this);
        consoleReader.shutdown();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        thread.interrupt();
    }
}
