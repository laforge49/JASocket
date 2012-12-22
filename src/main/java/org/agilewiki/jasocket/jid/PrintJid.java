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
package org.agilewiki.jasocket.jid;

import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jid.collection.vlenc.BListJid;
import org.agilewiki.jid.scalar.vlens.string.StringJid;

public class PrintJid extends BListJid<StringJid> {
    public static PrintJid newPrintJid(JLPCActor actor) throws Exception {
        return (PrintJid) JAFactory.newActor(
                actor, JASocketFactories.PRINT_JID_FACTORY, actor.getMailboxFactory().createMailbox());
    }

    public void println(String v) throws Exception {
        iAdd(-1);
        StringJid sj = iGet(-1);
        sj.setValue(v);
    }

    public void appendto(StringBuilder sb) throws Exception {
        int s = size();
        int i = 0;
        while (i < s) {
            sb.append(iGet(i).getValue());
            sb.append("\n");
            i += 1;
        }
    }
}
