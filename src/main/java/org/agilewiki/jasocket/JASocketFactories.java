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
package org.agilewiki.jasocket;

import org.agilewiki.jactor.Actor;
import org.agilewiki.jactor.Mailbox;
import org.agilewiki.jactor.factory.ActorFactory;
import org.agilewiki.jactor.factory.JAFactory;
import org.agilewiki.jactor.lpc.JLPCActor;
import org.agilewiki.jasocket.jid.ExceptionJidFactory;
import org.agilewiki.jasocket.jid.TransportJidFactory;
import org.agilewiki.jasocket.jid.agent.EvalAgentFactory;
import org.agilewiki.jasocket.cluster.*;
import org.agilewiki.jid.JidFactories;

public class JASocketFactories extends JLPCActor {
    public final static String EXCEPTION_FACTORY = "ExceptionJid";
    public final static String TRANSPORT_FACTORY = "transportJid";
    public final static String ADD_REMOTE_SERVER_NAME_AGENT_FACTORY = "addRemoteServerNameAgent";
    public final static String REMOVE_REMOTE_SERVER_NAME_AGENT_FACTORY = "removeRemoteServerNameAgent";
    public final static String SET_CLIENT_PORT_AGENT_FACTORY = "setClientPortAgent";
    public final static String GET_LOCAL_SERVER_AGENT_FACTORY = "getLocalServerAgent";
    public final static String KEEP_ALIVE_FACTORY = "keepAliveAgent";
    public final static String EVAL_FACTORY = "evalAgent";

    @Override
    public void initialize(Mailbox mailbox, Actor parent, ActorFactory actorFactory)
            throws Exception {
        if (parent == null) {
            parent = new JidFactories();
            ((JidFactories) parent).initialize(mailbox);
        }
        super.initialize(mailbox, parent, actorFactory);

        registerActorFactory(ExceptionJidFactory.fac);
        registerActorFactory(TransportJidFactory.fac);
        registerActorFactory(AddRemoteServerNameAgentFactory.fac);
        registerActorFactory(RemoveServerNameAgentFactory.fac);
        registerActorFactory(SetClientPortAgentFactory.fac);
        registerActorFactory(GetLocalServerAgentFactory.fac);
        registerActorFactory(KeepAliveAgentFactory.fac);
        registerActorFactory(EvalAgentFactory.fac);
    }

    public void registerActorFactory(ActorFactory actorFactory) throws Exception {
        Actor f = getParent();
        while (!(f instanceof JAFactory)) f = f.getParent();
        JAFactory factory = (JAFactory) f;
        factory.registerActorFactory(actorFactory);
    }

    public Actor newActor(String actorType)
            throws Exception {
        return JAFactory.newActor(this, actorType);
    }

    public Actor newActor(String actorType, Mailbox mailbox)
            throws Exception {
        return JAFactory.newActor(this, actorType, mailbox);
    }

    public Actor newActor(String actorType, Mailbox mailbox, Actor parent)
            throws Exception {
        return JAFactory.newActor(this, actorType, mailbox, parent);
    }
}
