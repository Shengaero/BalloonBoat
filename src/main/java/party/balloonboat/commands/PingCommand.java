/*
 * Copyright 2017 John Grosh & Kaidan Gustave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package party.balloonboat.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;

import java.time.temporal.ChronoUnit;

/**
 * @author Kaidan Gustave
 */
public class PingCommand extends Command
{
    public PingCommand()
    {
        this.name = "Ping";
        this.help = "Gets the bot's latency.";
        this.usesTopicTags = false;
        this.guildOnly = false;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        event.reply("Ping...", m -> m.editMessage(String.format("Ping: %d ms | WebSocket: %d ms",
            event.getMessage().getCreationTime().until(m.getCreationTime(), ChronoUnit.MILLIS),
            event.getJDA().getPing())).queue());
    }
}
