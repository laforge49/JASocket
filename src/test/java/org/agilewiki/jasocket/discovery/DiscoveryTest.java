package org.agilewiki.jasocket.discovery;

import junit.framework.TestCase;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class DiscoveryTest extends TestCase {
    public void test() throws Exception {
        // Select the appropriate network interface
        NetworkInterface interf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());

        //Select a multicasting IP address and port
        InetAddress group = InetAddress.getByName("225.0.0.100");
        int port = 8887;

        final DatagramChannel dc = DatagramChannel.open(StandardProtocolFamily.INET)
                .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                .bind(new InetSocketAddress(port))
                .setOption(StandardSocketOptions.IP_MULTICAST_IF, interf);
        dc.join(group, interf);

        final DatagramChannel dc1 = DatagramChannel.open(StandardProtocolFamily.INET)
                .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                .bind(new InetSocketAddress(port))
                .setOption(StandardSocketOptions.IP_MULTICAST_IF, interf);
        dc1.join(group, interf);

        Thread t = new Thread() {
            @Override
            public void run() {
                ByteBuffer in = ByteBuffer.allocate(4);
                try {
                    SocketAddress sa = dc.receive(in);
                    in.flip();
                    byte[] bytes = new byte[in.limit()];
                    System.out.println(in.getInt());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        t.start();

        Thread t1 = new Thread() {
            @Override
            public void run() {
                ByteBuffer in = ByteBuffer.allocate(4);
                try {
                    SocketAddress sa = dc1.receive(in);
                    in.flip();
                    byte[] bytes = new byte[in.limit()];
                    System.out.println(in.getInt());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        t1.start();

        Thread.sleep(100);

        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(4400);
        bb.flip();
        dc.send(bb, new InetSocketAddress(group, port));

        Thread.sleep(100);
        dc.close();
        dc1.close();
    }
}
