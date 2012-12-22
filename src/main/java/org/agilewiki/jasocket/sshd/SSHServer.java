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

import org.agilewiki.jactor.RP;
import org.agilewiki.jasocket.jid.PrintJid;
import org.agilewiki.jasocket.node.Node;
import org.agilewiki.jasocket.server.Server;
import org.apache.sshd.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

public class SSHServer extends Server {
    private int sshPort;
    private SshServer sshd;

    @Override
    protected String serviceName() {
        return "sshServer";
    }

    @Override
    protected void startService(PrintJid out, RP rp) throws Exception {
        sshPort = sshPort();
        out.println("ssh port: " + sshPort);
        sshd = SshServer.setUpDefaultServer();
        setAuthenticator();
        sshd.setPort(sshPort);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
        setShellFactory();
        sshd.start();
        super.startService(out, rp);
    }

    @Override
    public void close() {
        try {
            if (sshd != null)
                sshd.stop(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        super.close();
    }

    protected int sshPort() throws Exception {
        return node().clusterPort() + 1;
    }

    protected void setAuthenticator() {
        sshd.setPasswordAuthenticator(new DummyPasswordAuthenticator());
    }

    protected void setShellFactory() {
        sshd.setShellFactory(new JASShellFactory(node()));
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
