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
package org.agilewiki.jasocket.commands;

import org.agilewiki.jactor.RP;
import org.agilewiki.jasocket.console.Interpreter;
import org.agilewiki.jasocket.jid.PrintJid;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class WriteAgent extends CommandStringAgent {
    @Override
    public void process(RP<PrintJid> rp) throws Exception {
        ConcurrentHashMap<String, Interpreter> interpreters = agentChannelManager().interpreters;
        String args = getArgString();
        if (interpreters.size() == 0) {
            out.println("no operators present");
        } else {
            int i = args.indexOf(' ');
            if (i == -1) {
                out.println("no message is present, only operator name " + args);
            } else {
                String name = args.substring(0, i);
                String msg = args.substring(i + 1);
                Iterator<String> it = interpreters.keySet().iterator();
                boolean found = false;
                while (it.hasNext()) {
                    String id = it.next();
                    Interpreter interpreter = interpreters.get(id);
                    if (interpreter.getOperatorName().equals(name)) {
                        found = true;
                        interpreter.notice(getOperatorName() + ": " + msg);
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
}
