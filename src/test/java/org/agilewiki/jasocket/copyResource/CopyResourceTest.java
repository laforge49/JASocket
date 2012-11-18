package org.agilewiki.jasocket.copyResource;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.server.AgentChannelManager;
import org.agilewiki.jasocket.server.CopyResource;
import org.agilewiki.jasocket.server.RegisterResource;
import org.agilewiki.jid.JidFactories;
import org.agilewiki.jid.scalar.vlens.string.StringJid;

public class CopyResourceTest extends TestCase {
    public void test() throws Exception {
        JAMailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(10);
        JASocketFactories factory = new JASocketFactories();
        factory.initialize();
        JAFuture future = new JAFuture();
        AgentChannelManager agentChannelManager0 = new AgentChannelManager();
        agentChannelManager0.initialize(mailboxFactory.createMailbox(), factory);
        agentChannelManager0.openServerSocket(8880);
        String address0 = agentChannelManager0.agentChannelManagerAddress();

        StringJid sj0 = (StringJid) (new CopyResource(address0, "hi")).send(future, agentChannelManager0);
        System.out.println(sj0);

        StringJid sj = (StringJid)
                factory.newActor(JidFactories.STRING_JID_TYPE, mailboxFactory.createMailbox());
        sj.setValue("Hello world!");
        (new RegisterResource("hi", sj)).send(future, agentChannelManager0);

        StringJid sj1 = (StringJid) (new CopyResource(address0, "hi")).send(future, agentChannelManager0);
        System.out.println(sj1.getValue());

        AgentChannelManager agentChannelManager1 = new AgentChannelManager();
        agentChannelManager1.initialize(mailboxFactory.createMailbox(), factory);
        agentChannelManager1.openServerSocket(8881);

        StringJid sj2 = (StringJid) (new CopyResource(address0, "ho")).send(future, agentChannelManager1);
        System.out.println(sj0);

        StringJid sj3 = (StringJid) (new CopyResource(address0, "hi")).send(future, agentChannelManager1);
        System.out.println(sj1.getValue());

        mailboxFactory.close();
    }
}
