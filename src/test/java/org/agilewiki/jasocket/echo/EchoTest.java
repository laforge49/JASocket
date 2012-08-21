package org.agilewiki.jasocket.echo;

import junit.framework.TestCase;
import org.agilewiki.jactor.JAFuture;
import org.agilewiki.jactor.JAMailboxFactory;
import org.agilewiki.jactor.MailboxFactory;

public class EchoTest extends TestCase {
    public void test() throws Exception {
        MailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(10);
        try {
            Driver driver = new Driver();
            driver.initialize(mailboxFactory.createMailbox());
            DoIt.req.send(new JAFuture(), driver);
            Thread.sleep(200);
            driver.close();
        } finally {
            mailboxFactory.close();
        }
    }
}
