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
package org.agilewiki.jasocket.console;

import org.agilewiki.jactor.JAMailboxFactory;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

public class Console {
    BufferedReader inbr;
    TreeMap<String, Command> commands = new TreeMap<String, Command>();
    String[] args;

    String input() throws IOException {
        return inbr.readLine();
    }

    void cmd(String name, String description, String type) {
        commands.put(name, new Command(description, type));
    }

    public void process(String[] args) throws Exception {
        this.args = args;
        cmd("help", "Displays this list of commands", "");
        cmd("exit", "Exit (only) this console", "");
        System.out.println("\n*** JASocket Test Console***\n");
        JAMailboxFactory mailboxFactory = JAMailboxFactory.newMailboxFactory(100);
        try {
            inbr = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.print(">");
                String in = input();
                int i = in.indexOf(' ');
                String rem = "";
                if (i > -1) {
                    rem = in.substring(i + 1);
                    in = in.substring(0, i);
                }
                if (in.equals("exit"))
                    return;
                if (in.equals("help")) {
                    Iterator<String> it = commands.keySet().iterator();
                    while(it.hasNext()) {
                        String name = it.next();
                        Command cmd = commands.get(name);
                        System.out.println(name + " - " + cmd.description());
                    }
                    continue;
                }
                Command cmd = commands.get(in);
                if (cmd == null) {
                    System.out.println("No such command: " + in + ". (Use the help command for more information.)");
                }
            }
        } finally {
            mailboxFactory.close();
        }
    }

    public static void main(String[] args) throws Exception {
        (new Console()).process(args);
    }
}
