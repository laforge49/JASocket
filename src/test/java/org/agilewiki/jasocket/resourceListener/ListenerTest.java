package org.agilewiki.jasocket.resourceListener;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.discovery.Discovery;
import org.agilewiki.jasocket.server.AgentChannelManager;
import org.agilewiki.jasocket.server.RegisterResource;
import org.agilewiki.jasocket.server.SubscribeResourceNotifications;
import org.agilewiki.jasocket.server.UnregisterResource;
import org.agilewiki.jid.scalar.vlens.string.StringJid;

public class ListenerTest extends TestCase {
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

        System.out.println("\nregister local resources 0a and 1a");
        (new RegisterResource("0a", new StringJid())).send(future, agentChannelManager0);
        (new RegisterResource("1a", new StringJid())).send(future, agentChannelManager1);

        System.out.println("\nadd listeners");
        Listener listener0 = new Listener();
        listener0.initialize(mailboxFactory.createMailbox());
        listener0.txt = "0";
        (new SubscribeResourceNotifications(listener0)).send(future, agentChannelManager0);
        Listener listener1 = new Listener();
        listener1.initialize(mailboxFactory.createMailbox());
        listener1.txt = "1";
        (new SubscribeResourceNotifications(listener1)).send(future, agentChannelManager1);

        System.out.println("\nopen channel");
        new Discovery(agentChannelManager0);
        new Discovery(agentChannelManager1);
        Thread.sleep(100);

        System.out.println("\nregister local resources 0b and 1b");
        (new RegisterResource("0b", new StringJid())).send(future, agentChannelManager0);
        (new RegisterResource("1b", new StringJid())).send(future, agentChannelManager1);
        Thread.sleep(100);

        System.out.println("\nregister local resources 0b and 1b, again");
        (new RegisterResource("0b", new StringJid())).send(future, agentChannelManager0);
        (new RegisterResource("1b", new StringJid())).send(future, agentChannelManager1);
        Thread.sleep(100);

        System.out.println("\nunregister local resources 0a and 1a");
        (new UnregisterResource("0a")).send(future, agentChannelManager0);
        (new UnregisterResource("1a")).send(future, agentChannelManager1);
        Thread.sleep(100);

        System.out.println("\nunregister local resources 0a and 1a, again");
        (new UnregisterResource("0a")).send(future, agentChannelManager0);
        (new UnregisterResource("1a")).send(future, agentChannelManager1);
        Thread.sleep(100);

        System.out.println("\nshutdown");
        mailboxFactory.close();
    }
}
