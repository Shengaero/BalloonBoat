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

import com.jagrosh.jdautilities.JDAUtilitiesInfo;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import net.dv8tion.jda.bot.entities.ApplicationInfo;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.User;
import party.balloonboat.Bot;
import party.balloonboat.data.Database;

/**
 * @author Kaidan Gustave
 */
public class AboutCommand extends DatabaseCommand
{
    private String invite;
    private User jagrosh;
    private User monitor;

    private final Permission[] permissions;

    public AboutCommand(Database database, Permission... permissions)
    {
        super(database);
        this.name = "About";
        this.help = "Gets information about the bot.";
        this.guildOnly = false;
        this.usesTopicTags = false;
        this.botPermissions = new Permission[] { Permission.MESSAGE_EMBED_LINKS };
        this.permissions = permissions;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if(invite == null)
        {
            try {
                ApplicationInfo appInfo = event.getJDA().asBot().getApplicationInfo().complete();
                if(appInfo.isBotPublic())
                    invite = appInfo.getInviteUrl(permissions);
                else
                    invite = "";
            } catch(Exception e) {
                Bot.LOG.warn("Failed to generate invite URL! ", e);
            }
        }
        EmbedBuilder b = new EmbedBuilder();
        b.setColor(Bot.Config.BLURPLE_COLOR);
        b.setTitle(Bot.Config.BOT_EMOJI + " All about BalloonBoat! " + Bot.Config.BOT_EMOJI);

        if(jagrosh == null)
            jagrosh = event.getJDA().retrieveUserById(event.getClient().getOwnerId()).complete();
        if(monitor == null)
            monitor = event.getJDA().retrieveUserById(event.getClient().getCoOwnerIds()[0]).complete();

        String description =
                "Hello! I'm **" + event.getSelfUser().getName() + "**, " +
                "a simple rating bot for Discord! " + "I was created by **" +
                jagrosh.getName() + "**#" + jagrosh.getDiscriminator() + " (" +
                jagrosh.getAsMention() + ") and **" + monitor.getName() + "**#" +
                monitor.getDiscriminator() + " (" + monitor.getAsMention() +
                ") using the [JDA Library](" + JDAInfo.GITHUB + ") and " +
                "[JDA-Utilities](" + JDAUtilitiesInfo.GITHUB + ")!\n" +
                "Use `" + event.getClient().getPrefix() + "Rate`, to rate another user `" +
                event.getClient().getPrefix() + "Rank` to see your rank, or use `" +
                event.getClient().getPrefix() + "Help` for a full list of my commands!";

        b.setDescription(description);

        if(event.getJDA().getShardInfo() != null)
            b.addField("\uD83D\uDCCA Stats", event.getClient().getTotalGuilds()+" servers\n"+
                    (event.getJDA().getShardInfo().getShardId()+1)+" of "+
                    (event.getJDA().getShardInfo().getShardTotal())+"shards\n"+
                    event.getJDA().getUserCache().size()+" users", true);
        else
            b.addField("\uD83D\uDCCA Stats", event.getClient().getTotalGuilds()+" servers\n"+
                    event.getJDA().getUserCache().size()+" users", true);

        b.addField(Bot.Config.BOT_EMOJI+" Ratings", database.getGlobalTotalRatings()+" unique ratings\n"+
                String.format("%.3f average rating", database.getGlobalAverage()), true);

        b.addField("\uD83C\uDF10 Links",
                (invite != null && !invite.isEmpty() ? "[Invite]("+invite+")\n" : "")+
                "[Support]("+event.getClient().getServerInvite()+")", true);

        b.setFooter("Last Restart", null);
        b.setTimestamp(event.getClient().getStartTime());

        event.reply(b.build());
    }
}
