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
import org.agilewiki.jactor.continuation.Continuation;
import org.agilewiki.jasocket.jid.PrintJid;

import java.util.Timer;
import java.util.TimerTask;

public class PauseAgent extends CommandStringAgent {
    private TimerTask timerTask;

    @Override
    public void process(final RP<PrintJid> rp) throws Exception {
        String arg = getArgString();
        int sec = 5;
        if (arg.length() > 0) {
            sec = Integer.valueOf(arg);
        }
        Timer timer = getMailboxFactory().timer();
        final Continuation<PrintJid> c = new Continuation<PrintJid>(this, rp);
        timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    c.processResponse(out);
                } catch (Exception ex) {
                }
            }
        };
        timer.schedule(timerTask, sec * 1000);
    }

    @Override
    public void userInterrupt() throws Exception {
        timerTask.cancel();
        out.println("*** Pause Interrupted ***");
        startRP.processResponse(out);
    }
}
