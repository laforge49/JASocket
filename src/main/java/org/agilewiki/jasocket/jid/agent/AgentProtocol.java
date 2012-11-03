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
package org.agilewiki.jasocket.jid.agent;

import org.agilewiki.jactor.RP;
import org.agilewiki.jasocket.jid.JidProtocol;
import org.agilewiki.jasocket.jid.TransportJid;
import org.agilewiki.jasocket.jid.TransportJidFactory;
import org.agilewiki.jid.Jid;
import org.agilewiki.jid.scalar.vlens.actor.RootJid;

public class AgentProtocol extends JidProtocol {
    @Override
    protected void receiveRequest(Jid jid, RP<Jid> rp) throws Exception {
        AgentJid agentJid = (AgentJid) jid;
        agentJid.agentApplication = this;
        StartAgent.req.send(this, agentJid, rp);
    }

    @Override
    public void receiveBytes(byte[] bytes) throws Exception {
        RootJid root = new RootJid();
        if (bytes[0] == 1)
            root.initialize(getMailboxFactory().createAsyncMailbox(), this);
        else
            root.initialize(getMailboxFactory().createMailbox(), this);
        root.load(bytes, 1, bytes.length - 1);
        TransportJid transport = (TransportJid) root.getValue();
        boolean requestFlag = transport.isRequest();
        Long id = transport.getId();
        Jid jid = transport.getContent();
        if (requestFlag)
            if (id == -1)
                gotEvent(jid);
            else
                gotReq(id, jid);
        else
            gotRsp(id, jid);
    }

    @Override
    protected void write(boolean requestFlag, long id, Jid jid) throws Exception {
        RootJid root = new RootJid();
        root.initialize(this);
        root.setValue(TransportJidFactory.TRANSPORT_FACTORY);
        TransportJid transport = (TransportJid) root.getValue();
        transport.setRequest(requestFlag);
        transport.setId(id);
        transport.setContent(jid);
        byte[] bytes = new byte[root.getSerializedLength() + 1];
        if (requestFlag && ((AgentJid) jid).async())
            bytes[0] = 1;
        else
            bytes[0] = 0;
        root.save(bytes, 1);
        writeBytes(bytes);
    }
}
