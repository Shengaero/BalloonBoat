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
import com.jagrosh.jdautilities.utils.FinderUtil;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import party.balloonboat.data.Database;
import party.balloonboat.utils.FormatUtils;

import java.util.List;

/**
 * @author Kaidan Gustave
 */
public class RankCommand extends DatabaseCommand
{
    public RankCommand(Database database)
    {
        super(database);
        this.name = "Rank";
        this.arguments = "<User>";
        this.help = "Gets a user's rank.";
        this.cooldown = 3;
        this.cooldownScope = CooldownScope.USER_CHANNEL;
        this.guildOnly = true;
        this.usesTopicTags = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        final User user;
        if(event.getArgs().isEmpty())
            user = event.getAuthor();
        else {
            List<Member> members = FinderUtil.findMembers(event.getArgs(), event.getGuild());

            if(members.size() < 1) {
                event.replyError("Could not find a user matching \"" + event.getArgs() + "\"!");
                return;
            }

            if(members.size() > 1) {
                event.replyError(FormatUtils.tooManyMembers(event.getArgs(), members));
                return;
            }

            user = members.get(0).getUser();
        }

        short rating = database.getUserRating(user);

        if(event.getAuthor().equals(user))
            event.replySuccess("Your rank is `" + rating + "`!");
        else
            event.replySuccess("**" + user.getName() + "**#" + user.getDiscriminator() + "'s rank is `" + rating + "`!");
    }
}
