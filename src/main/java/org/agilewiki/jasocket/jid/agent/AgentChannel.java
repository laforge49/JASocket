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

import org.agilewiki.jactor.ExceptionHandler;
import org.agilewiki.jactor.JANoResponse;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;
import org.agilewiki.jasocket.AgentSocket;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.SocketProtocol;
import org.agilewiki.jasocket.WriteBytes;
import org.agilewiki.jasocket.jid.ExceptionJid;
import org.agilewiki.jasocket.jid.ExceptionJidFactory;
import org.agilewiki.jasocket.jid.RemoteException;
import org.agilewiki.jasocket.jid.TransportJid;
import org.agilewiki.jasocket.server.AgentChannelManager;
import org.agilewiki.jid.Jid;
import org.agilewiki.jid.scalar.vlens.actor.RootJid;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ThreadFactory;

public class AgentChannel extends JLPCActor implements SocketProtocol {
    HashMap<Long, RP> rps = new HashMap<Long, RP>();
    long requestId = 0;
    private AgentSocket agentSocket;
    private AgentChannelManager agentChannelManager;
    private boolean client;
    String remoteAddress;
    protected HashSet<String> remoteResourceNames = new HashSet<String>();

    public void addRemoteResourceName(String name) {
        remoteResourceNames.add(name);
    }

    public void removeRemoteResourceName(String name) {
        remoteResourceNames.remove(name);
    }

    public boolean isClient() {
        return client;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void writeBytes(byte[] bytes) throws Exception {
        (new WriteBytes(bytes)).sendEvent(this, agentSocket);
    }

    @Override
    public void processException(Exception exception) {
        exception.printStackTrace();
        close();
    }

    public void open(InetSocketAddress inetSocketAddress, int maxPacketSize, AgentChannelManager agentChannelManager, ThreadFactory threadFactory)
            throws Exception {
        this.agentChannelManager = agentChannelManager;
        agentSocket = new AgentSocket();
        agentSocket.setAgentChannel(this);
        agentSocket.initialize(getMailboxFactory().createAsyncMailbox());
        agentSocket.clientOpen(inetSocketAddress, maxPacketSize, threadFactory);
        client = true;
        remoteAddress = agentSocket.getRemoteAddress();
    }

    public void serverOpen(SocketChannel socketChannel, int maxPacketSize, AgentChannelManager agentChannelManager, ThreadFactory threadFactory)
            throws Exception {
        this.agentChannelManager = agentChannelManager;
        agentSocket = new AgentSocket();
        agentSocket.setAgentChannel(this);
        agentSocket.initialize(getMailboxFactory().createAsyncMailbox());
        agentSocket.serverOpen(socketChannel, maxPacketSize, threadFactory);
        remoteAddress = agentSocket.getRemoteAddress();
    }

    public void close() {
        agentChannelManager.closed(this);
        agentSocket.close();
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

    protected void receiveRequest(Jid jid, RP<Jid> rp) throws Exception {
        AgentJid agentJid = (AgentJid) jid;
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

    public void shipAgentEvent(AgentJid jid) throws Exception {
        shipAgent(jid, JANoResponse.nrp);
    }

    public void shipAgent(final AgentJid jid, final RP rp) throws Exception {
        if (rp.isEvent()) {
            write(true, -1, jid);
        } else {
            requestId += 1;
            requestId %= 1000000000000000000L;
            rps.put(requestId, rp);
            write(true, requestId, jid);
        }
    }

    private void write(boolean requestFlag, long id, Jid jid) throws Exception {
        RootJid root = new RootJid();
        root.initialize(this);
        root.setValue(JASocketFactories.TRANSPORT_FACTORY);
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
