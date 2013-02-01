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
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jasocket.agentChannel.AgentChannel;
import org.agilewiki.jasocket.cluster.AgentChannelManager;
import org.agilewiki.jasocket.node.Node;
import org.agilewiki.jid.Jid;
import org.agilewiki.jid.collection.flenc.AppJid;

abstract public class AgentJid extends AppJid {
    private long requestId = -1L;
    protected RP startRP;

    public void _start(RP<Jid> rp) throws Exception {
        startRP = rp;
        if (requestId > -1) {
            agentChannel().agents.put(requestId, this);
        }
        start(rp);
    }

    abstract public void start(RP<Jid> rp) throws Exception;

    public void _userInterrupt() throws Exception {
        setExceptionHandler(new ExceptionHandler() {
            @Override
            public void process(Exception exception) throws Exception {
                startRP.processResponse(exception);
            }
        });
        userInterrupt();
    }

    public void userInterrupt() throws Exception {
        startRP.processResponse(null);
    }

    protected AgentChannel agentChannel() {
        JLPCActor actor = getParent();
        if (actor instanceof  AgentChannel)
            return (AgentChannel) getParent();
        return null;
    }

    protected String remoteAddress() {
        return agentChannel().remoteAddress();
    }

    protected boolean isLocal() {
        return getParent() instanceof AgentChannelManager;
    }

    protected boolean isLocalAddress(String address) throws Exception {
        return agentChannelManager().isLocalAddress(address);
    }

    protected AgentChannelManager agentChannelManager() {
        if (isLocal())
            return (AgentChannelManager) getParent();
        return agentChannel().agentChannelManager();
    }

    protected Node node() {
        return agentChannelManager().node;
    }

    public boolean async() {
        return true;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public long getRequestId() {
        return requestId;
    }
}
