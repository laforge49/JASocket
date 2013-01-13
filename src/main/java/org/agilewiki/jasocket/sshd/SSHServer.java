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
package org.agilewiki.jasocket.sshd;

import org.agilewiki.jactor.ExceptionHandler;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.agentChannel.AgentChannel;
import org.agilewiki.jasocket.agentChannel.ShipAgent;
import org.agilewiki.jasocket.cluster.GetAgentChannel;
import org.agilewiki.jasocket.cluster.ServerNames;
import org.agilewiki.jasocket.cluster.ShipAgentEventToAll;
import org.agilewiki.jasocket.jid.PrintJid;
import org.agilewiki.jasocket.node.Node;
import org.agilewiki.jasocket.server.Server;
import org.agilewiki.jasocket.server.ServerCommand;
import org.agilewiki.jid.Jid;
import org.apache.mina.util.ConcurrentHashSet;
import org.apache.sshd.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

import java.util.Iterator;
import java.util.TreeSet;

public class SSHServer extends Server {
    private int sshPort;
    private SshServer sshd;
    public ConcurrentHashSet<JASShell> shells = new ConcurrentHashSet<JASShell>();

    @Override
    protected String serverName() {
        return "sshServer";
    }

    @Override
    protected void startServer(PrintJid out, RP rp) throws Exception {
        registerWriteCommand();
        registerBroadcastCommand();
        registerWhoCommand();
        sshPort = sshPort();
        out.println("ssh port: " + sshPort);
        sshd = SshServer.setUpDefaultServer();
        setAuthenticator();
        sshd.setPort(sshPort);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
        setShellFactory();
        sshd.start();
        super.startServer(out, rp);
    }

    @Override
    public void close() {
        Iterator<JASShell> it = shells.iterator();
        while (it.hasNext()) {
            JASShell shell = it.next();
            try {
                shell.exitCallback.onExit(0);
            } catch (Exception ex) {
            }
        }
        try {
            if (sshd != null) {
                //            sshd.stop();    Hangs!
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        super.close();
    }

    protected int sshPort() throws Exception {
        return node().clusterPort() + 1;
    }

    protected void setAuthenticator() {
        sshd.setPasswordAuthenticator(new DummyPasswordAuthenticator(this));
    }

    protected void setShellFactory() {
        sshd.setShellFactory(new JASShellFactory(this, node()));
    }

    protected void registerWriteCommand() {
        registerServerCommand(new ServerCommand("write", "Send a message to a user's ssh client") {
            @Override
            public void eval(String operatorName, String args, PrintJid out, RP<PrintJid> rp) throws Exception {
                if (shells.size() == 0) {
                    out.println("no operators present");
                } else {
                    int i = args.indexOf(' ');
                    if (i == -1) {
                        out.println("no message is present, only operator name " + args);
                    } else {
                        String name = args.substring(0, i);
                        String msg = args.substring(i + 1);
                        Iterator<JASShell> it = shells.iterator();
                        boolean found = false;
                        while (it.hasNext()) {
                            JASShell sh = it.next();
                            if (sh.getOperatorName().equals(name)) {
                                found = true;
                                sh.notice(operatorName + ": " + msg);
                            }
                        }
                        if (found)
                            out.println("wrote");
                        else
                            out.println("no such operator: " + name);
                    }
                }
                rp.processResponse(out);
            }
        });
    }

    protected void registerBroadcastCommand() {
        registerServerCommand(new ServerCommand("broadcast", "Send a message to all ssh clients") {
            @Override
            public void eval(String operatorName, String args, final PrintJid out, final RP<PrintJid> rp) throws Exception {
                BroadcastAgent broadcastAgent = (BroadcastAgent) JAFactory.newActor(
                        SSHServer.this,
                        JASocketFactories.BROADCAST_AGENT_FACTORY,
                        getMailbox(),
                        agentChannelManager());
                broadcastAgent.configure(operatorName, args);
                (new ShipAgentEventToAll(broadcastAgent)).sendEvent(SSHServer.this, agentChannelManager());
                broadcastAgent.start(new RP<Jid>() {
                    @Override
                    public void processResponse(Jid response) throws Exception {
                        rp.processResponse(out);
                    }
                });
            }
        });
    }

    protected void registerWhoCommand() {
        registerServerCommand(new ServerCommand("who", "Lists operators/node/logonTime/commandCount/idleTime") {
            @Override
            public void eval(String operatorName, String args, final PrintJid out, final RP<PrintJid> rp) throws Exception {
                final WhoAgent whoAgent = (WhoAgent) JAFactory.newActor(
                        SSHServer.this,
                        JASocketFactories.WHO_AGENT_FACTORY,
                        getMailbox(),
                        agentChannelManager());
                final ShipAgent shipAgent = new ShipAgent(whoAgent);
                ServerNames.req.send(SSHServer.this, agentChannelManager(), new RP<TreeSet<String>>() {
                    @Override
                    public void processResponse(TreeSet<String> response) throws Exception {
                        Iterator<String> itan = response.iterator();
                        TreeSet<String> addresses = new TreeSet<String>();
                        while (itan.hasNext()) {
                            String an = itan.next();
                            if (an.endsWith(" sshServer")) {
                                int l = an.length() - " sshServer".length();
                                addresses.add(an.substring(0, l));
                            }
                        }
                        final WhoRP whoRP = new WhoRP(rp, addresses.size(), out);
                        setExceptionHandler(new ExceptionHandler() {
                            @Override
                            public void process(Exception exception) throws Exception {
                                whoRP.processResponse(PrintJid.newPrintJid(SSHServer.this));
                            }
                        });
                        Iterator<String> ita = addresses.iterator();
                        while (ita.hasNext()) {
                            String address = ita.next();
                            if (agentChannelManager().isLocalAddress(address)) {
                                whoAgent.start(whoRP);
                            } else
                                (new GetAgentChannel(address)).send(SSHServer.this, agentChannelManager(), new RP<AgentChannel>() {
                                    @Override
                                    public void processResponse(AgentChannel response) throws Exception {
                                        shipAgent.send(SSHServer.this, response, whoRP);
                                    }
                                });
                        }
                    }
                });
            }
        });
    }

    class WhoRP extends RP {
        private int expecting;
        private final RP rp;
        private final PrintJid out;
        private final TreeSet<String> ts = new TreeSet<String>();

        public WhoRP(RP rp, int expecting, PrintJid out) {
            this.rp = rp;
            this.expecting = expecting;
            this.out = out;
        }

        @Override
        public void processResponse(Object response) throws Exception {
            PrintJid o = (PrintJid) response;
            int s = o.size();
            int i = 0;
            while (i < s) {
                ts.add(o.iGet(i).getValue());
                i += 1;
            }
            expecting -= 1;
            if (expecting > 0)
                return;
            s = ts.size();
            Iterator<String> it = ts.iterator();
            while (it.hasNext()) {
                out.println(it.next());
            }
            rp.processResponse(out);
        }
    }

    public static void main(String[] args) throws Exception {
        Node node = new Node(args, 100);
        try {
            node.process();
            node.startup(SSHServer.class, "");
        } catch (Exception ex) {
            node.mailboxFactory().close();
            throw ex;
        }
    }
}
