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

import com.jagrosh.jdautilities.commandclient.CommandEvent;
import party.balloonboat.Bot;
import party.balloonboat.data.Database;

/**
 * @author Kaidan Gustave
 */
public class ShutdownCommand extends DatabaseCommand
{
    public ShutdownCommand(Database database)
    {
        super(database);
        this.name = "Shutdown";
        this.help = "Safely shuts BalloonBoat down.";
        this.usesTopicTags = false;
        this.guildOnly = false;
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        Bot.LOG.info("Shutting Down...");

        // Complete to block the thread so we don't shutdown JDA early
        try {
            event.getChannel().sendMessage(Bot.Config.WARNING_EMOJI + " Shutting Down...").complete();
        } catch(Exception ignored) {}

        // Shutdown JDA
        // Database will shutdown in the ShutdownEvent
        event.getJDA().shutdown();

    }
}
