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
package org.agilewiki.jasocket.cluster;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jactor.lpc.Request;
import org.agilewiki.jasocket.agentChannel.AgentChannel;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class OpenAgentChannel extends Request<AgentChannel, AgentChannelManager> {
    private final InetSocketAddress inetSocketAddress;

    public OpenAgentChannel(int port) throws Exception {
        InetAddress inetAddress = InetAddress.getLocalHost();
        this.inetSocketAddress = new InetSocketAddress(inetAddress, port);
    }

    public OpenAgentChannel(String address) throws Exception {
        int i = address.indexOf(":");
        String host = address.substring(0, i);
        int port = Integer.valueOf(address.substring(i + 1));
        inetSocketAddress = new InetSocketAddress(host, port);
    }

    public OpenAgentChannel(InetSocketAddress inetSocketAddress) {
        this.inetSocketAddress = inetSocketAddress;
    }

    @Override
    public boolean isTargetType(Actor targetActor) {
        return targetActor instanceof AgentChannelManager;
    }

    @Override
    public void processRequest(JLPCActor targetActor, RP rp) throws Exception {
        ((AgentChannelManager) targetActor).agentChannel(inetSocketAddress, rp);
    }
}
