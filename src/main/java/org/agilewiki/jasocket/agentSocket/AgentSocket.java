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
package org.agilewiki.jasocket.agentSocket;

import org.agilewiki.jasocket.agentChannel.AgentChannel;
import org.agilewiki.jasocket.agentChannel.ReceiveBytes;

import java.nio.ByteBuffer;

public class AgentSocket extends RawSocket {
    byte[] lengthBytes = new byte[4];
    int lengthIndex = 0;
    int length;
    byte[] bytes = null;
    int bytesIndex;

    public void setAgentChannel(AgentChannel agentChannel) {
        this.agentChannel = agentChannel;
        exceptionProcessor = agentChannel;
    }

    @Override
    protected void receiveByteBuffer(ByteBuffer byteBuffer) throws Exception {
        while (byteBuffer.remaining() > 0) {
            if (bytes == null)
                buildLength(byteBuffer);
            else
                buildBytes(byteBuffer);
        }
    }

    void buildLength(ByteBuffer byteBuffer) {
        int r = byteBuffer.remaining();
        int l = 4 - lengthIndex;
        if (l > r)
            l = r;
        byteBuffer.get(lengthBytes, lengthIndex, l);
        lengthIndex += l;
        if (lengthIndex < 4)
            return;
        ByteBuffer lengthBuffer = ByteBuffer.wrap(lengthBytes);
        length = lengthBuffer.getInt();
        bytes = new byte[length];
        bytesIndex = 0;
        lengthIndex = 0;
    }

    void buildBytes(ByteBuffer byteBuffer) throws Exception {
        int r = byteBuffer.remaining();
        int l = length - bytesIndex;
        if (l > r)
            l = r;
        byteBuffer.get(bytes, bytesIndex, l);
        bytesIndex += l;
        if (bytesIndex < length)
            return;
        byte[] b = bytes;
        bytes = null;
        (new ReceiveBytes(b)).sendEvent(this, agentChannel);
    }

    @Override
    public void processException(Exception exception) {
        getMailboxFactory().logException(false, "AgentSocket threw unhandled exception", exception);
    }
}
