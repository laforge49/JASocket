package org.agilewiki.jasocket.applicationListener;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jasocket.JASocketFactories;
import org.agilewiki.jasocket.application.HelloWorld;
import org.agilewiki.jasocket.discovery.Discovery;
import org.agilewiki.jasocket.server.AgentChannelManager;
import org.agilewiki.jasocket.server.RegisterApplication;
import org.agilewiki.jasocket.server.SubscribeApplicationNameNotifications;
import org.agilewiki.jasocket.server.UnregisterApplication;
import org.agilewiki.jid.scalar.vlens.string.StringJid;

import java.net.InetAddress;
import java.net.NetworkInterface;

public class ListenerTest extends TestCase {
    public void test() throws Exception {
        JAMailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(10);
        JASocketFactories factory = new JASocketFactories();
        factory.initialize();
        JAFuture future = new JAFuture();
        AgentChannelManager agentChannelManager0 = new AgentChannelManager();
        agentChannelManager0.maxPacketSize = 64000;
        agentChannelManager0.initialize(mailboxFactory.createMailbox(), factory);
        agentChannelManager0.openServerSocket(8880);
        AgentChannelManager agentChannelManager1 = new AgentChannelManager();
        agentChannelManager1.maxPacketSize = 64000;
        agentChannelManager1.initialize(mailboxFactory.createMailbox(), factory);
        agentChannelManager1.openServerSocket(8881);

        System.out.println("\nregister local applicationNames 0a and 1a");
        (new RegisterApplication("0a", new HelloWorld())).send(future, agentChannelManager0);
        (new RegisterApplication("1a", new HelloWorld())).send(future, agentChannelManager1);

        System.out.println("\nadd listeners");
        Listener listener0 = new Listener();
        listener0.initialize(mailboxFactory.createMailbox());
        listener0.txt = "0";
        (new SubscribeApplicationNameNotifications(listener0)).send(future, agentChannelManager0);
        Listener listener1 = new Listener();
        listener1.initialize(mailboxFactory.createMailbox());
        listener1.txt = "1";
        (new SubscribeApplicationNameNotifications(listener1)).send(future, agentChannelManager1);

        System.out.println("\nopen channel");
        new Discovery(agentChannelManager0,
                NetworkInterface.getByInetAddress(InetAddress.getLocalHost()),
                "225.49.42.13",
                8887,
                2000);
        new Discovery(agentChannelManager1,
                NetworkInterface.getByInetAddress(InetAddress.getLocalHost()),
                "225.49.42.13",
                8887,
                2000);
        Thread.sleep(100);

        System.out.println("\nregister local applicationNames 0b and 1b");
        (new RegisterApplication("0b", new HelloWorld())).send(future, agentChannelManager0);
        (new RegisterApplication("1b", new HelloWorld())).send(future, agentChannelManager1);
        Thread.sleep(100);

        System.out.println("\nregister local applicationNames 0b and 1b, again");
        (new RegisterApplication("0b", new HelloWorld())).send(future, agentChannelManager0);
        (new RegisterApplication("1b", new HelloWorld())).send(future, agentChannelManager1);
        Thread.sleep(100);

        System.out.println("\nunregister local applicationNames 0a and 1a");
        (new UnregisterApplication("0a")).send(future, agentChannelManager0);
        (new UnregisterApplication("1a")).send(future, agentChannelManager1);
        Thread.sleep(100);

        System.out.println("\nunregister local applicationNames 0a and 1a, again");
        (new UnregisterApplication("0a")).send(future, agentChannelManager0);
        (new UnregisterApplication("1a")).send(future, agentChannelManager1);
        Thread.sleep(100);

        System.out.println("\nshutdown");
        mailboxFactory.close();
    }
}
