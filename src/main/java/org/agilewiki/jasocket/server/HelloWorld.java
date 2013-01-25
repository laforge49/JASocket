package org.agilewiki.jasocket.server;

import org.agilewiki.jactor.RP;
import org.agilewiki.jactor.continuation.Continuation;
import org.agilewiki.jasocket.jid.PrintJid;
import org.agilewiki.jasocket.node.Node;

import java.util.Timer;
import java.util.TimerTask;

public class HelloWorld extends Server {

    @Override
    protected String serverName() {
        return "helloWorld";
    }

    @Override
    protected void startServer(PrintJid out, RP rp) throws Exception {
        registerHi();
        registerException();
        registerPause();
        registerInterruptException();
        super.startServer(out, rp);
    }

    public void registerHi() {
        registerServerCommand(new ServerCommand("hi", "says hello") {
            @Override
            public void eval(String operatorName,
                             String args,
                             PrintJid out,
                             long requestId,
                             RP<PrintJid> rp) throws Exception {
                out.println("Hello!");
                rp.processResponse(out);
            }
        });
    }

    public void registerException() {
        registerServerCommand(new ServerCommand("exception", "User-raised exception") {
            @Override
            public void eval(String operatorName,
                             String args,
                             PrintJid out,
                             long requestId,
                             RP<PrintJid> rp) throws Exception {
                throw new Exception("User-raised exception");
            }
        });
    }

    public void registerPause() {
        registerServerCommand(new InterruptableServerCommand<TimerTask>(
                "pause",
                "pause for n seconds, where n defaults to 5") {
            @Override
            public void eval(String operatorName,
                             String args,
                             final PrintJid out,
                             long requestId,
                             RP<PrintJid> rp) throws Exception {
                int sec = 5;
                if (args.length() > 0) {
                    sec = Integer.valueOf(args);
                }
                Timer timer = getMailboxFactory().timer();
                final Continuation<PrintJid> c = new Continuation<PrintJid>(HelloWorld.this, rp);
                TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            c.processResponse(out);
                        } catch (Exception ex) {
                        }
                    }
                };
                timer.schedule(timerTask, sec * 1000);
                contextMap.put(requestId, timerTask);
            }

            @Override
            public void serverUserInterrupt(String args,
                                            PrintJid out,
                                            long requestId) throws Exception {
                TimerTask timerTask = contextMap.get(requestId);
                timerTask.cancel();
                out.println("*** Pause Interrupted ***");
            }
        });
    }

    public void registerInterruptException() {
        registerServerCommand(new InterruptableServerCommand<Void>(
                "interruptException",
                "Hangs and then throws an exception in response to a user interrupt") {
            @Override
            public void eval(String operatorName,
                             String args,
                             final PrintJid out,
                             long requestId,
                             RP<PrintJid> rp) throws Exception {
            }

            @Override
            public void serverUserInterrupt(String args,
                                            PrintJid out,
                                            long requestId) throws Exception {
                throw new Exception("User interrupt received");
            }
        });
    }

    public static void main(String[] args) throws Exception {
        Node node = new Node(args, 100);
        try {
            node.process();
            node.startup(HelloWorld.class, "");
        } catch (Exception ex) {
            node.mailboxFactory().close();
            throw ex;
        }
    }
}
