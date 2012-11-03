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

import org.agilewiki.jactor.ExceptionHandler;
import org.agilewiki.jactor.JANoResponse;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.Request;
import org.agilewiki.jasocket.BytesProtocol;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jid.Jid;
import org.agilewiki.jid.scalar.vlens.actor.RootJid;

import java.util.HashMap;

abstract public class JidProtocol extends BytesProtocol {
    HashMap<Long, RP> rps = new HashMap<Long, RP>();
    long requestId = 0;

    @Override
    public void receiveBytes(byte[] bytes) throws Exception {
        RootJid root = new RootJid();
        root.initialize(getMailboxFactory().createMailbox(), this);
        root.load(bytes, 0, bytes.length);
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

    protected void gotEvent(Jid jid) throws Exception {
        final Request request = getMailbox().getCurrentRequest().getUnwrappedRequest();
        setExceptionHandler(new ExceptionHandler() {
            @Override
            public void process(Exception exception) throws Exception {
                getMailboxFactory().eventException(request, exception);
            }
        });
        receiveRequest(jid, JANoResponse.nrp);
    }

    protected void gotReq(final Long id, Jid jid) throws Exception {
        setExceptionHandler(new ExceptionHandler() {
            @Override
            public void process(Exception exception) throws Exception {
                RemoteException re = new RemoteException(exception);
                ExceptionJid bj = (ExceptionJid) ExceptionJidFactory.fac.newActor(getMailbox(), null);
                bj.setObject(re);
                write(false, id, bj);
            }
        });
        receiveRequest(jid, new RP<Jid>() {
            @Override
            public void processResponse(Jid response) throws Exception {
                write(false, id, response);
            }
        });
    }

    abstract protected void receiveRequest(Jid jid, RP<Jid> rp) throws Exception;

    protected void gotRsp(Long id, Jid jid) throws Exception {
        RP rp = rps.remove(id);
        if (rp != null) {
            if (jid instanceof ExceptionJid) {
                ExceptionJid ej = (ExceptionJid) jid;
                Exception ex = (Exception) ej.getObject();
                rp.processResponse(ex);
            } else
                rp.processResponse(jid);
        }
    }

    @Override
    public void processException(Exception ex) {
        ex.printStackTrace();
        close();
    }

    void writeRequest(final Jid jid, final RP rp) throws Exception {
        if (rp.isEvent()) {
            write(true, -1, jid);
        } else {
            requestId += 1;
            requestId %= 1000000000000000000L;
            rps.put(requestId, rp);
            write(true, requestId, jid);
        }
    }

    protected void write(boolean requestFlag, long id, Jid jid) throws Exception {
        RootJid root = new RootJid();
        root.initialize(this);
        root.setValue(JASocketFactories.TRANSPORT_FACTORY);
        TransportJid transport = (TransportJid) root.getValue();
        transport.setRequest(requestFlag);
        transport.setId(id);
        transport.setContent(jid);
        byte[] bytes = new byte[root.getSerializedLength()];
        root.save(bytes, 0);
        writeBytes(bytes);
    }
}
