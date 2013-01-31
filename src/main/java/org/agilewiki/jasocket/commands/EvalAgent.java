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
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jasocket.jid.PrintJid;
import org.agilewiki.jasocket.jid.agent.StartAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvalAgent extends CommandStringAgent {
    private static Logger logger = LoggerFactory.getLogger(EvalAgent.class);

    private CommandAgent agent;

    @Override
    protected void process(RP<PrintJid> rp) throws Exception {
        if (isLocal())
            logger.info(getOperatorName() + ">" + getArgString());
        else
            logger.info("(" +
                    agentChannel().remoteAddress +
                    ") " +
                    getOperatorName() +
                    ">" +
                    getArgString());
        String in = getArgString().trim();
        int i = in.indexOf(' ');
        String rem = "";
        if (i > -1) {
            rem = in.substring(i + 1);
            in = in.substring(0, i);
        }
        rem = rem.trim();
        Command cmd = getCommand(in);
        if (cmd == null) {
            println("No such command: " + in + ". (Use the help command for a list of commands.)");
            rp.processResponse(out);
            return;
        }
        String type = cmd.type();
        agent = (CommandAgent)
                JAFactory.newActor(this, type, getMailboxFactory().createAsyncMailbox(), getParent());
        agent.configure(getOperatorName(), getId(), rem);
        StartAgent.req.send(this, agent, (RP) rp);
    }

    public void userInterrupt() throws Exception {
        if (isLocal())
            logger.info(getOperatorName() + ">^C");
        else
            logger.info("(" +
                    agentChannel().remoteAddress +
                    ") " +
                    getOperatorName() +
                    ">^C");
        UserInterrupt.req.sendEvent(this, agent);
    }
}
