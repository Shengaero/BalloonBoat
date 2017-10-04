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
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.User;
import party.balloonboat.Bot;

import java.util.List;

/**
 * @author Kaidan Gustave
 */
public class HelpCommand extends Command
{
    private User jagrosh;
    private User monitor;

    public HelpCommand()
    {
        this.name = "Help";
        this.help = "Gets a list of commands.";
        this.usesTopicTags = false;
        this.guildOnly = false;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if(jagrosh == null)
            jagrosh = event.getJDA().retrieveUserById(event.getClient().getOwnerId()).complete();
        if(monitor == null)
            monitor = event.getJDA().retrieveUserById(event.getClient().getCoOwnerIds()[0]).complete();

        String prefix = event.getClient().getPrefix();

        List<Command> commands = event.getClient().getCommands();
        StringBuilder sb = new StringBuilder();

        sb.append(Bot.Config.BOT_EMOJI).append(" **__BalloonBoat Commands__**").append("\n\n");

        commands.stream().filter(command -> !command.isOwnerCommand()).forEach(command -> {
            appendCommand(sb, prefix, command);
        });

        sb.append("\n");

        if(event.isOwner() || event.isCoOwner())
        {
            sb.append(Bot.Config.SUCCESS_EMOJI).append(" **__Developer Commands__**").append("\n\n");

            commands.stream().filter(Command::isOwnerCommand).forEach(command -> {
                appendCommand(sb, prefix, command);
            });

            sb.append("\n");
        }

        sb.append("If you require additional help contact **")
                .append(jagrosh.getName()).append("**#").append(jagrosh.getDiscriminator())
                .append(" or **")
                .append(monitor.getName()).append("**#").append(monitor.getDiscriminator());

        if(event.getClient().getServerInvite() != null)
        {
            sb.append(", or join my support server: ").append(event.getClient().getServerInvite());
        }

        if(event.isFromType(ChannelType.TEXT))
            event.reactSuccess();
        event.replyInDM(sb.toString());
    }

    private void appendCommand(StringBuilder sb, String prefix, Command command)
    {
        sb.append("`").append(prefix).append(command.getName());

        if(command.getArguments() != null && !command.getArguments().isEmpty())
            sb.append(" ").append(command.getArguments());

        sb.append("`");

        if(command.getHelp() != null && !command.getHelp().isEmpty())
            sb.append(" - ").append(command.getHelp());

        sb.append("\n");
    }
}
