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

import org.agilewiki.jactor.ExceptionHandler;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.agentChannel.AgentChannel;
import org.agilewiki.jasocket.agentChannel.ShipAgent;
import org.agilewiki.jasocket.cluster.Channels;
import org.agilewiki.jasocket.cluster.GetAgentChannel;
import org.agilewiki.jasocket.console.Interpreter;
import org.agilewiki.jasocket.jid.PrintJid;
import org.agilewiki.jasocket.node.Node;
import org.agilewiki.jasocket.server.InterruptableServerCommand;
import org.agilewiki.jasocket.server.Server;
import org.apache.sshd.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

public class SSHServerImpl extends Server implements SSHServer {
    private SshServer sshd;
    public int idCounter;

    @Override
    protected String serverName() {
        return "sshServer";
    }

    @Override
    public int incId() {
        idCounter += 1;
        return idCounter;
    }

    @Override
    protected void startServer(PrintJid out, RP rp) throws Exception {
        registerSshServers();
        out.println("ssh port: " + sshPort());
        sshd = SshServer.setUpDefaultServer();
        sshd.setPasswordAuthenticator(node().passwordAuthenticator());
        sshd.setPort(sshPort());
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
        setShellFactory();
        sshd.start();
        super.startServer(out, rp);
    }

    @Override
    public void close() {
        HashMap<String, Interpreter> interpreters = new HashMap<String, Interpreter>(agentChannelManager().interpreters);
        Iterator<String> it = interpreters.keySet().iterator();
        while (it.hasNext()) {
            String id = it.next();
            Interpreter interpreter = interpreters.get(id);
            try {
                interpreter.close();
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

    @Override
    public int sshPort() throws Exception {
        if (startupArgs.length() == 0)
            return node().clusterPort() + 1;
        else
            return Integer.valueOf(startupArgs);
    }

    protected void setShellFactory() {
        sshd.setShellFactory(new JASShellFactory(this, node()));
    }

    public void registerSshServers() {
        registerServerCommand(new InterruptableServerCommand<MungeRP>(
                "all",
                "Lists all sshServer's address/runTime/runtime/userCount") {

            @Override
            public void eval(String operatorName,
                             String id,
                             AgentChannel agentChannel,
                             String args,
                             final PrintJid out,
                             long requestId,
                             RP<PrintJid> rp) throws Exception {
                final MungeRP mungeRP = new MungeRP(out, rp);
                contextMap.put(requestId, mungeRP);
                final SSHAgent sshAgent = (SSHAgent) JAFactory.newActor(
                        SSHServerImpl.this,
                        JASocketFactories.SSH_AGENT_FACTORY,
                        getMailbox(),
                        agentChannelManager());
                sshAgent.configure(serverName());
                final ShipAgent shipAgent = new ShipAgent(sshAgent);
                Channels.req.send(SSHServerImpl.this, agentChannelManager(), new RP<TreeSet<String>>() {
                    @Override
                    public void processResponse(TreeSet<String> addresses) throws Exception {
                        mungeRP.expecting = addresses.size() + 1;
                        setExceptionHandler(new ExceptionHandler() {
                            @Override
                            public void process(Exception exception) throws Exception {
                                mungeRP.processResponse(null);
                            }
                        });
                        Iterator<String> ita = addresses.iterator();
                        while (ita.hasNext()) {
                            String address = ita.next();
                            (new GetAgentChannel(address)).
                                    send(SSHServerImpl.this, agentChannelManager(), new RP<AgentChannel>() {
                                        @Override
                                        public void processResponse(AgentChannel response) throws Exception {
                                            if (response == null)
                                                mungeRP.processResponse(null);
                                            else
                                                shipAgent.send(SSHServerImpl.this, response, mungeRP);
                                        }
                                    });
                        }
                        sshAgent._start(mungeRP);
                    }
                });
            }

            @Override
            public void serverUserInterrupt(String args,
                                            PrintJid out,
                                            long requestId) throws Exception {
                MungeRP mungeRP = contextMap.get(requestId);
                Iterator<String> it = mungeRP.ts.iterator();
                while (it.hasNext()) {
                    out.println(it.next());
                }
                out.println("*** sshPorts Interrupted ***");
                out.println("No response from " + mungeRP.expecting + " nodes.");
            }
        });
    }

    public static void main(String[] args) throws Exception {
        Node node = new Node(args, 100);
        try {
            node.process();
            node.startup(SSHServerImpl.class, "");
        } catch (Exception ex) {
            node.mailboxFactory().close();
            throw ex;
        }
    }
}
