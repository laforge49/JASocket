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
package org.agilewiki.jasocket.commands;

import org.agilewiki.jactor.RP;
import org.agilewiki.jasocket.cluster.GetLocalServer;
import org.agilewiki.jasocket.jid.PrintJid;
import org.agilewiki.jasocket.server.EvalServerCommand;
import org.agilewiki.jasocket.server.Server;

public class ServerEvalAgent extends CommandStringAgent {
    @Override
    protected void process(final RP<PrintJid> rp) throws Exception {
        String args = getArgString().trim();
        int i = args.indexOf(' ');
        if (i == -1) {
            println("Server command name is missing");
            rp.processResponse(out);
            return;
        }
        final String serverName = args.substring(0, i);
        final String commandLine = args.substring(i + 1).trim();
        (new GetLocalServer(serverName)).send(this, agentChannelManager(), new RP<Server>() {
            @Override
            public void processResponse(Server response) throws Exception {
                if (response == null) {
                    println("Unable to locate server " + serverName);
                    rp.processResponse(out);
                    return;
                }
                (new EvalServerCommand(getOperatorName(), commandLine, out)).
                        send(ServerEvalAgent.this, response, rp);
            }
        });
    }
}
