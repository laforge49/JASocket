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

import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jasocket.BytesApplication;
import org.agilewiki.jid.Jid;
import org.agilewiki.jid.collection.flenc.TupleJid;
import org.agilewiki.jid.scalar.flens.bool.BooleanJid;
import org.agilewiki.jid.scalar.flens.lng.LongJid;
import org.agilewiki.jid.scalar.vlens.actor.ActorJid;
import org.agilewiki.jid.scalar.vlens.actor.RootJid;
import org.agilewiki.jid.scalar.vlens.actor.RootJidFactory;

import java.util.HashMap;

public class JidApplication extends BytesApplication {
    HashMap<Long, RP> rps = new HashMap<Long, RP>();
    long requestId = 0;

    @Override
    public void receiveBytes(byte[] bytes) throws Exception {
        RootJid root = new RootJid();
        root.initialize(getMailbox(), this);
        root.load(bytes);
        try {
        TupleJid transport = (TupleJid) root.getValue();
        BooleanJid requestFlag = (BooleanJid) transport.iGet(0);
        LongJid idj = (LongJid) transport.iGet(1);
        Long id = idj.getValue();
        ActorJid envelope = (ActorJid) transport.iGet(2);
        Jid jid = (Jid) envelope.getValue();
        if (requestFlag.getValue())
            gotReq(id, jid);
        else
            gotRsp(id, jid);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    private void gotReq(final Long id, Jid jid) throws Exception {
        receiveRequest(jid, new RP<Jid>() {
            @Override
            public void processResponse(Jid response) throws Exception {
                write(false, id, response);
            }
        });
    }

    protected void receiveRequest(Jid jid, RP<Jid> rp) throws Exception {
    }

    private void gotRsp(Long id, Jid jid) throws Exception {
        RP rp = rps.remove(id);
        if (rp != null)
            rp.processResponse(jid);
    }

    @Override
    public void processException(Exception ex) {
        ex.printStackTrace();
        close();
    }

    @Override
    protected void closed() {}

    public void writeRequest(final Jid jid, final RP<Jid> rp) throws Exception {
        requestId += 1;
        requestId %= 1000000000000000000L;
        rps.put(requestId, rp);
        write(true, requestId, jid);
    }

    private void write(boolean requestFlag, Long id, Jid jid) throws Exception {
        RootJid root = new RootJid();
        root.initialize(getMailbox(), this);
        root.setValue(TransportFactory.TRANSPORT_FACTORY);
        TupleJid transport = (TupleJid) root.getValue();
        BooleanJid requestFlagJid = (BooleanJid) transport.iGet(0);
        requestFlagJid.setValue(requestFlag);
        LongJid idj = (LongJid) transport.iGet(1);
        idj.setValue(id);
        ActorJid envelope = (ActorJid) transport.iGet(2);
        envelope.setBytes(jid.getFactory(), jid.getSerializedBytes());
        byte[] bytes = root.getSerializedBytes();
        writeBytes(bytes);
    }
}
