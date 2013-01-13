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

import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jasocket.cluster.GetLocalServer;
import org.agilewiki.jasocket.jid.agent.AgentJid;
import org.agilewiki.jid.Jid;
import org.agilewiki.jid.scalar.vlens.string.StringJid;
import org.apache.mina.util.ConcurrentHashSet;

import java.util.Iterator;

public class BroadcastAgent extends AgentJid {
    private StringJid getOperatorNameJid() throws Exception {
        return (StringJid) _iGet(0);
    }

    private StringJid getMessageJid() throws Exception {
        return (StringJid) _iGet(1);
    }

    public void configure(String operatorName, String message) throws Exception {
        getOperatorNameJid().setValue(operatorName);
        getMessageJid().setValue(message);
    }

    @Override
    public void start(final RP<Jid> rp) throws Exception {
        (new GetLocalServer("sshServer")).send(this, agentChannelManager(), new RP<JLPCActor>() {
            @Override
            public void processResponse(JLPCActor response) throws Exception {
                if (response == null) {
                    rp.processResponse(null);
                    return;
                }
                SSHServer sshServer = (SSHServer) response;
                ConcurrentHashSet<JASShell> shells = sshServer.shells;
                Iterator<JASShell> it = shells.iterator();
                String notice =
                        "[broadcast] " + getOperatorNameJid().getValue() + ": " + getMessageJid().getValue();
                while (it.hasNext()) {
                    JASShell shell = it.next();
                    shell.notice(notice);
                }
                rp.processResponse(null);
            }
        });

    }
}
