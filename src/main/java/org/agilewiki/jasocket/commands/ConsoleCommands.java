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
package org.agilewiki.jasocket.commands;

public class ConsoleCommands extends Commands {
    @Override
    protected void configure() throws Exception {
        cmd("help", "Display this list of commands",
                HelpAgentFactory.fac);
        cmd("to", "Send a command to another node",
                ToAgentFactory.fac);
        cmd("channels", "List all the open channels to other nodes",
                ChannelsAgentFactory.fac);
        cmd("servers", "list all accessible servers running in the cluster",
                ServersAgentFactory.fac);
        cmd("localServers", "list the local server names/operators/runtime/startupArgs",
                LocalServersAgentFactory.fac);
        cmd("halt", "Exit (only) the local node",
                HaltAgentFactory.fac);
        cmd("exception", "Throw an exception",
                ExceptionAgentFactory.fac);
        cmd("latencyTest", "Benchmark latency between this node and another node",
                LatencyBenchmarkAgentFactory.fac);
        cmd("throughputTest", "Benchmark throughput between this node and another node",
                ThroughputBenchmarkAgentFactory.fac);
        cmd("server", "Send a command to a server",
                ServerEvalAgentFactory.fac);
        cmd("startup", "Start and register a server",
                StartupAgentFactory.fac);
        cmd("pause", "pause for n seconds, where n defaults to 5",
                PauseAgentFactory.fac);
        cmd("write", "send a message to a user",
                WriteAgentFactory.fac);
        cmd("broadcast", "send a message to all users",
                BroadcastAgentFactory.fac);
        cmd("who", "List all user names/node/logonTime/commandCount/idleTime",
                WhoAgentFactory.fac);
    }
}
