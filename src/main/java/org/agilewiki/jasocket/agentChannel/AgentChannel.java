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
package org.agilewiki.jasocket.agentChannel;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.ExceptionHandler;
import org.agilewiki.jactor.JANoResponse;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.SocketProtocol;
import org.agilewiki.jasocket.agentSocket.AgentSocket;
import org.agilewiki.jasocket.agentSocket.WriteBytes;
import org.agilewiki.jasocket.cluster.AgentChannelClosed;
import org.agilewiki.jasocket.cluster.AgentChannelManager;
import org.agilewiki.jasocket.jid.ExceptionJid;
import org.agilewiki.jasocket.jid.ExceptionJidFactory;
import org.agilewiki.jasocket.jid.RemoteException;
import org.agilewiki.jasocket.jid.TransportJid;
import org.agilewiki.jasocket.jid.agent.AgentJid;
import org.agilewiki.jasocket.jid.agent.StartAgent;
import org.agilewiki.jid.CopyJID;
import org.agilewiki.jid.Jid;
import org.agilewiki.jid.scalar.vlens.actor.RootJid;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class AgentChannel extends JLPCActor implements SocketProtocol {
    HashMap<Long, RP> rps = new HashMap<Long, RP>();
    long requestId = 0;
    private AgentSocket agentSocket;
    private boolean client;
    public String remoteAddress;
    public ConcurrentHashMap<Long, AgentJid> agents = new ConcurrentHashMap<Long, AgentJid>();

    public AgentChannelManager agentChannelManager() {
        return (AgentChannelManager) getParent();
    }

    public void received() {
        if (remoteAddress != null)
            agentChannelManager().received(this);
    }

    public void sent() {
        if (remoteAddress != null)
            agentChannelManager().sent(remoteAddress);
    }

    public boolean isClient() {
        return client;
    }

    public String remoteAddress() {
        return remoteAddress;
    }

    public int remotePort() {
        int i = remoteAddress.indexOf(':');
        return Integer.valueOf(remoteAddress.substring(i + 1));
    }

    public void writeBytes(byte[] bytes) throws Exception {
        (new WriteBytes(bytes)).sendEvent(this, agentSocket);
    }

    @Override
    public void processException(Exception exception) throws Exception {
        CloseChannel.req.sendEvent(this);
    }

    public void open(InetSocketAddress inetSocketAddress, int maxPacketSize)
            throws Exception {
        agentSocket = new AgentSocket();
        agentSocket.setAgentChannel(this);
        agentSocket.initialize(getMailboxFactory().createAsyncMailbox());
        agentSocket.clientOpen(inetSocketAddress, maxPacketSize);
        client = true;
        remoteAddress = agentSocket.getRemoteAddress();
    }

    public void serverOpen(SocketChannel socketChannel, int maxPacketSize)
            throws Exception {
        agentSocket = new AgentSocket();
        agentSocket.setAgentChannel(this);
        agentSocket.initialize(getMailboxFactory().createAsyncMailbox());
        agentSocket.serverOpen(socketChannel, maxPacketSize);
    }

    public void setClientPort(int port) throws Exception {
        this.remoteAddress = agentSocket.getRemoteHostAddress() + ":" + port;
    }

    public void closeChannel() {
        agentSocket.close();
        Iterator<RP> it = rps.values().iterator();
        while (it.hasNext()) {
            RP rp = it.next();
            try {
                rp.processResponse(new AgentChannelClosedException(remoteAddress));
            } catch (Exception x) {
                getMailboxFactory().logException(false, "unhandled exception while aborting unsatisfied request", x);
            }
        }
        try {
            (new AgentChannelClosed(this)).sendEvent(agentChannelManager());
        } catch (Exception ex) {
            getMailboxFactory().logException(false, "exception when sending event AgentChannelClosed", ex);
        }
    }

    protected void gotEvent(AgentJid agentJid) throws Exception {
        final Request request = getMailbox().getCurrentRequest().getUnwrappedRequest();
        setExceptionHandler(new ExceptionHandler() {
            @Override
            public void process(Exception exception) throws Exception {
                getMailboxFactory().eventException(request, exception);
            }
        });
        StartAgent.req.send(this, agentJid, JANoResponse.nrp);
    }

    private void gotReq(final Long requestId, AgentJid agentJid) throws Exception {
        setExceptionHandler(new ExceptionHandler() {
            @Override
            public void process(Exception exception) throws Exception {
                RemoteException re = new RemoteException(exception);
                ExceptionJid bj = (ExceptionJid) ExceptionJidFactory.fac.newActor(getMailbox(), null);
                bj.setObject(re);
                agents.remove(requestId);
                write(false, requestId, bj);
            }
        });
        agentJid.setRequestId(requestId);
        StartAgent.req.send(this, agentJid, new RP<Jid>() {
            @Override
            public void processResponse(Jid response) throws Exception {
                agents.remove(requestId);
                write(false, requestId, response);
            }
        });
    }

    @Override
    public void receiveBytes(byte[] bytes) throws Exception {
        RootJid root = new RootJid();
        if (bytes[0] == 1) {
            root.initialize(getMailboxFactory().createAsyncMailbox(), this);
        } else {
            root.initialize(getMailboxFactory().createMailbox(), this);
        }
        root.load(bytes, 1, bytes.length - 1);
        TransportJid transport = (TransportJid) root.getValue();
        boolean requestFlag = transport.isRequest();
        Long requestId = transport.getId();
        Jid jid = transport.getContent();
        if (requestFlag)
            if (requestId == -1)
                gotEvent((AgentJid) jid);
            else
                gotReq(requestId, (AgentJid) jid);
        else
            gotRsp(requestId, jid);
    }

    protected void gotRsp(Long requestId, Jid jid) throws Exception {
        RP rp = rps.remove(requestId);
        if (rp != null) {
            if (jid instanceof ExceptionJid) {
                ExceptionJid ej = (ExceptionJid) jid;
                Exception ex = (Exception) ej.getObject();
                rp.processResponse(ex);
            } else
                rp.processResponse(jid);
        }
    }

    public void shipAgent(final AgentJid agent, final RP rp) throws Exception {
        try {
            if (rp.isEvent()) {
                write(true, -1, agent);
            } else {
                requestId += 1;
                if (requestId == Long.MAX_VALUE)
                    requestId = 0;
                if (agent != null)
                    agent.setRequestId(requestId);
                write(true, requestId, agent);
                rps.put(requestId, rp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void write(final boolean requestFlag, final long requestId, Jid jid) throws Exception {
        if (jid == null)
            writeCopy(requestFlag, requestId, jid);
        else
            (new CopyJID(getMailbox())).send(this, jid, new RP<Actor>() {
                @Override
                public void processResponse(Actor response) throws Exception {
                    writeCopy(requestFlag, requestId, (Jid) response);
                }
            });
    }

    private void writeCopy(boolean requestFlag, long requestId, Jid jid) throws Exception {
        RootJid root = new RootJid();
        root.initialize(this);
        root.setValue(JASocketFactories.TRANSPORT_FACTORY);
        TransportJid transport = (TransportJid) root.getValue();
        transport.setRequest(requestFlag);
        transport.setId(requestId);
        transport.setContent(jid);
        byte[] bytes = new byte[root.getSerializedLength() + 1];
        if (requestFlag && ((AgentJid) jid).async()) {
            bytes[0] = 1;
        } else {
            bytes[0] = 0;
        }
        root.save(bytes, 1);
        writeBytes(bytes);
    }
}
