package org.agilewiki.jasocket.locateResource;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.discovery.Discovery;
import org.agilewiki.jasocket.server.AgentChannelManager;
import org.agilewiki.jasocket.server.LocateResource;
import org.agilewiki.jasocket.server.RegisterResource;
import org.agilewiki.jid.scalar.vlens.string.StringJid;

import java.util.Iterator;
import java.util.List;

public class LocateResourceTest extends TestCase {
    public void test() throws Exception {
        JAMailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(10);
        JASocketFactories factory = new JASocketFactories();
        factory.initialize();
        JAFuture future = new JAFuture();

        AgentChannelManager agentChannelManager0 = new AgentChannelManager();
        agentChannelManager0.initialize(mailboxFactory.createMailbox(), factory);
        agentChannelManager0.openServerSocket(8880);

        AgentChannelManager agentChannelManager1 = new AgentChannelManager();
        agentChannelManager1.initialize(mailboxFactory.createMailbox(), factory);
        agentChannelManager1.openServerSocket(8881);

        (new RegisterResource("a", new StringJid())).send(future, agentChannelManager0);
        (new RegisterResource("a", new StringJid())).send(future, agentChannelManager1);

        new Discovery(agentChannelManager0);
        new Discovery(agentChannelManager1);
        Thread.sleep(100);

        List<String> addresses = (new LocateResource("a")).send(future, agentChannelManager0);
        Iterator<String> it = addresses.iterator();
        while (it.hasNext()) {
            System.out.println(it.next());
        }

        mailboxFactory.close();
    }
}
