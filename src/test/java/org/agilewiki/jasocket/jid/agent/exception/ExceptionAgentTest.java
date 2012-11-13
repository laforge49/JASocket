package org.agilewiki.jasocket.jid.agent.exception;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.jid.ShipAgent;
import org.agilewiki.jasocket.jid.agent.AgentChannel;
import org.agilewiki.jasocket.server.AgentChannelManager;
import org.agilewiki.jasocket.server.OpenAgentChannel;

public class ExceptionAgentTest extends TestCase {
    public void test() throws Exception {
        JAMailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(10);
        JASocketFactories factory = new JASocketFactories();
        factory.initialize();
        AgentChannelManager agentChannelManager0 = new AgentChannelManager();
        agentChannelManager0.initialize(mailboxFactory.createMailbox(), factory);
        agentChannelManager0.openServerSocket(8880);
        AgentChannelManager agentChannelManager1 = new AgentChannelManager();
        agentChannelManager1.initialize(mailboxFactory.createMailbox(), factory);
        agentChannelManager1.openServerSocket(8881);
        JAFuture future = new JAFuture();
        AgentChannel agentChannel01 = (new OpenAgentChannel(8881)).send(future, agentChannelManager0);
        factory.registerActorFactory(ExceptionAgentFactory.fac);
        ExceptionAgent echoAgent = (ExceptionAgent)
                factory.newActor("ExceptionAgent", mailboxFactory.createMailbox());
        try {
            (new ShipAgent(echoAgent)).send(future, agentChannel01);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        agentChannelManager0.closeAll();
        agentChannelManager1.closeAll();
        mailboxFactory.close();
    }
}
