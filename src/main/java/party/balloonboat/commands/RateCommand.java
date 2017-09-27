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
import party.balloonboat.data.Database;
import party.balloonboat.utils.FormatUtils;

import java.util.List;

/**
 * @author Kaidan Gustave
 */
public class RateCommand extends DatabaseCommand
{
    public RateCommand(Database database)
    {
        super(database);
        this.name = "Rate";
        this.aliases = new String[]{"R"};
        this.arguments = "[1-5] [User]";
        this.help = "Rates another user 1-5.";
        this.cooldown = 5;
        this.cooldownScope = CooldownScope.USER;
        this.guildOnly = true;
        this.usesTopicTags = true;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        String[] parts = event.getArgs().split("\\s+",2);

        if(parts.length < 2)
        {
            event.replyError("Too few arguments! Try specifying a number 1 and 5 and then a user to rate!");
            return;
        }

        final short rating;

        try {
            rating = Short.parseShort(parts[0]);
        } catch(NumberFormatException e) {
            event.replyError("\""+parts[0]+"\" is not a valid number! Try a number 1 and 5!");
            return;
        }

        if(rating > 5 || rating < 1)
        {
            event.replyError("Invalid rating! Rating must be a number between 1 and 5");
            return;
        }

        List<Member> members = FinderUtil.findMembers(parts[1], event.getGuild());

        if(members.size() > 1)
        {
            event.replyError(FormatUtils.tooManyMembers(parts[1],members));
            return;
        }

        if(members.size() < 1)
        {
            event.replyError("Could not find a user matching \""+parts[1]+"\"!");
            return;
        }

        Member member = members.get(0);

        if(member.equals(event.getMember()))
        {
            event.replyError("You cannot rate yourself!");
            return;
        }

        database.setRating(event.getAuthor(), member.getUser(), rating);

        event.replySuccess(String.format("You rated **%#s** a %d/5", member.getUser(), rating));
    }
}
