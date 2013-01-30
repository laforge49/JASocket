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
package org.agilewiki.jasocket.sshd;

import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.MailboxFactory;
import org.agilewiki.jactor.concurrent.ThreadManager;
import org.agilewiki.jasocket.cluster.AgentChannelManager;
import org.agilewiki.jasocket.console.*;
import org.agilewiki.jasocket.node.Node;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class JASShell implements Command, ConsoleIO {
    protected Node node;
    protected AgentChannelManager agentChannelManager;
    protected MailboxFactory mailboxFactory;
    protected ThreadManager threadManager;
    protected InputStream in;
    protected PrintStream ps;
    protected OutputStream outputStream;
    protected PrintStream err;
    public ExitCallback exitCallback;
    protected Thread thread;
    protected Environment env;
    protected SSHServer sshServer;
    protected String operatorName;
    private Interpreter interpreter;
    private LineReader lineReader;
    private String id;

    public String getOperatorName() {
        return operatorName;
    }

    public int getCommandCount() {
        return interpreter.getCommandCount();
    }

    public long getLogonTime() {
        return interpreter.getLogonTime();
    }

    public long getIdleTime() {
        return interpreter.getIdleTime();
    }

    @Override
    public boolean hasInput() {
        return lineReader.hasInput();
    }

    public void notice(String n) {
        interpreter.notice(n);
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
        this.ps = new PrintStream(out, true);
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
        threadManager.process(new Runnable() {
            @Override
            public void run() {
                try {
                    JAFuture future = new JAFuture();
                    id = AssignSSHId.req.send(future, sshServer);
                    interpreter = new Interpreter();
                    interpreter.initialize(node.mailboxFactory().createAsyncMailbox());
                    interpreter.configure(operatorName, id, node, JASShell.this, ps);
                    agentChannelManager.interpreters.put(id, interpreter);
                    lineReader = new LineReader();
                    lineReader.initialize(node.mailboxFactory().createAsyncMailbox());
                    (new StartLineReader(in, outputStream, interpreter)).
                            sendEvent(lineReader);
                    thread = Thread.currentThread();
                    ps.println(
                            "\n*** JASShell " + agentChannelManager.agentChannelManagerAddress() + " ***\n");
                    while (true) {
                        interpreter.prompt();
                        String commandLine = ReadLine.req.send(future, lineReader);
                        (new Interpret(commandLine)).send(future, interpreter);
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
        lineReader.close();
        agentChannelManager.interpreters.remove(this);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        thread.interrupt();
    }

    @Override
    public void close() {
        exitCallback.onExit(0);
    }

    @Override
    public void print(String s) {
        ps.print(s);
        ps.flush();
    }

    @Override
    public void println(String s) {
        ps.println(s);
        ps.flush();
    }

    @Override
    public String readLine() throws Exception {
        return ReadLine.req.send(new JAFuture(), lineReader);
    }

    @Override
    public String readPassword() throws Exception {
        return ReadPassword.req.send(new JAFuture(), lineReader);
    }
}
