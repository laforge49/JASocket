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
package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.ExceptionHandler;
import org.agilewiki.jactor.RP;
import org.agilewiki.jasocket.jid.PrintJid;

import java.util.HashMap;

abstract public class InterruptableServerCommand<CONTEXT_TYPE> extends ServerCommand {
    final protected HashMap<Long, CONTEXT_TYPE> contextMap = new HashMap<Long, CONTEXT_TYPE>();

    public InterruptableServerCommand(String name, String description) {
        super(name, description);
    }

    @Override
    public void _eval(Server server,
                      String operatorName,
                      String id,
                      String args,
                      PrintJid out,
                      final long requestId,
                      final RP<PrintJid> rp) throws Exception {
        server.setExceptionHandler(new ExceptionHandler() {
            @Override
            public void process(Exception exception) throws Exception {
                contextMap.remove(requestId);
                ((RP) rp).processResponse(exception);
            }
        });
        eval(operatorName, id, args, out, requestId, new RP<PrintJid>() {
            @Override
            public void processResponse(PrintJid response) throws Exception {
                contextMap.remove(requestId);
                rp.processResponse(response);
            }
        });
    }

    @Override
    public void _serverUserInterrupt(Server server,
                                     String args,
                                     PrintJid out,
                                     final long requestId,
                                     final RP<PrintJid> rp) throws Exception {
        server.setExceptionHandler(new ExceptionHandler() {
            @Override
            public void process(Exception exception) throws Exception {
                contextMap.remove(requestId);
                ((RP) rp).processResponse(exception);
            }
        });
        serverUserInterrupt(args, out, requestId);
        contextMap.remove(requestId);
        rp.processResponse(out);
    }

    abstract public void serverUserInterrupt(String args,
                                             PrintJid out,
                                             long requestId) throws Exception;
}
