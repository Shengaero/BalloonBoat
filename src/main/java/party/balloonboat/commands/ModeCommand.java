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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandClient;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.jagrosh.jdautilities.commandclient.CommandListener;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * @author Kaidan Gustave
 */
public class ModeCommand extends Command
{
    private Mode mode = Mode.STANDARD;

    public ModeCommand()
    {
        this.name = "Mode";
        this.arguments = "[Standard | Debug]";
        this.help = "Sets the bot's mode.";
        this.ownerCommand = true;
        this.usesTopicTags = false;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if(event.getArgs().isEmpty())
        {
            event.reply("Currently set to **"+mode.name()+"**");
            return;
        }

        final Mode newMode;
        try {
            newMode = Mode.valueOf(event.getArgs().toUpperCase());
        } catch(IllegalArgumentException e) {
            event.replyError("Invalid Mode!");
            return;
        }

        if(mode == newMode)
        {
            event.replyWarning("Already set to **"+mode.name()+"**!");
            return;
        }

        if(newMode == Mode.DEBUG)
        {
            ((LoggerContext)LoggerFactory.getILoggerFactory()).getLoggerList()
                    .forEach(logger -> logger.setLevel(Level.DEBUG));
        }

        if(mode == Mode.DEBUG)
        {
            ((LoggerContext)LoggerFactory.getILoggerFactory()).getLoggerList()
                    .forEach(logger -> logger.setLevel(Level.INFO));
        }

        mode = newMode;

        event.getClient().setListener(mode.listener);
        event.replySuccess("Successfully set mode to **"+mode.name()+"**!");
    }

    @SuppressWarnings("unused")
    public enum Mode
    {
        STANDARD(null),

        DEBUG(new CommandListener()
        {
            @Override
            public void onCommand(CommandEvent event, Command command)
            {
                CLIENT_LOGGER.debug("Command Called - "+command.getName());
            }
            @Override
            public void onCompletedCommand(CommandEvent event, Command command)
            {
                CLIENT_LOGGER.debug("Command Completed - "+command.getName());
            }
            @Override
            public void onTerminatedCommand(CommandEvent event, Command command)
            {
                CLIENT_LOGGER.debug("Command Terminated - "+command.getName());
            }
            @Override
            public void onNonCommandMessage(MessageReceivedEvent event){}
        });

        public static final Logger CLIENT_LOGGER = LoggerFactory.getLogger(CommandClient.class);

        @Nullable
        final CommandListener listener;

        Mode(@Nullable CommandListener listener)
        {
            this.listener = listener;
        }
    }
}
