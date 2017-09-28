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
import com.jagrosh.jdautilities.menu.pagination.PaginatorBuilder;
import com.jagrosh.jdautilities.utils.FinderUtil;
import com.jagrosh.jdautilities.waiter.EventWaiter;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import party.balloonboat.data.Database;
import party.balloonboat.utils.FormatUtils;

import java.util.List;

/**
 * @author Kaidan Gustave
 */
public class RatingCommand extends DatabaseCommand
{
    public RatingCommand(Database database, EventWaiter waiter)
    {
        super(database);
        this.name = "Rating";
        this.arguments = "<User>";
        this.help = "Gets a users rating.";
        this.cooldown = 5;
        this.cooldownScope = CooldownScope.USER_CHANNEL;
        this.guildOnly = true;
        this.usesTopicTags = true;
        this.children = new Command[]{ new RatingListCommand(database, waiter) };
    }

    @Override
    protected void execute(CommandEvent event)
    {
        final User user;
        if(event.getArgs().isEmpty())
            user = event.getAuthor();
        else
        {
            List<Member> members = FinderUtil.findMembers(event.getArgs(), event.getGuild());

            if(members.size() < 1)
            {
                event.replyError("Could not find a user matching \""+event.getArgs()+"\"!");
                return;
            }

            if(members.size() > 1)
            {
                event.replyError(FormatUtils.tooManyMembers(event.getArgs(),members));
                return;
            }

            user = members.get(0).getUser();
        }

        short rating = database.getUserRating(user);

        if(event.getAuthor().equals(user))
            event.replySuccess("Your rating is `"+rating+"`!");
        else
            event.replySuccess("**"+user.getName()+"**#"+user.getDiscriminator()+"'s rating is `"+rating+"`!");
    }

    private class RatingListCommand extends DatabaseCommand
    {
        private final PaginatorBuilder pBuilder;

        public RatingListCommand(Database database, EventWaiter waiter)
        {
            super(database);
            this.name = "List";
            this.arguments = "<User>";
            this.help = "Gets a list of ratings made on the user.";
            this.cooldown = 10;
            this.cooldownScope = CooldownScope.USER_GUILD;
            this.guildOnly = true;
            this.usesTopicTags = true;
            this.botPermissions = new Permission[]{
                    Permission.MESSAGE_ADD_REACTION,
                    Permission.MESSAGE_MANAGE,
                    Permission.MESSAGE_EMBED_LINKS
            };
            this.pBuilder = new PaginatorBuilder()
                    .waitOnSinglePage(false)
                    .setFinalAction(m -> m.clearReactions().queue(v -> {}, v -> {}))
                    .setItemsPerPage(10)
                    .setEventWaiter(waiter);
        }

        @Override
        protected void execute(CommandEvent event)
        {
            final Member member;
            if(event.getArgs().isEmpty())
                member = event.getMember();
            else
            {
                List<Member> members = FinderUtil.findMembers(event.getArgs(), event.getGuild());

                if(members.size() < 1)
                {
                    event.replyError("Could not find a user matching \""+event.getArgs()+"\"!");
                    return;
                }

                if(members.size() > 1)
                {
                    event.replyError(FormatUtils.tooManyMembers(event.getArgs(),members));
                    return;
                }

                member = members.get(0);
            }

            event.replyWarning("Getting Ratings...", message -> {

                pBuilder.clearItems();

                pBuilder.setText((page, total) -> String.format("Ratings of %#s | Page %d/%d",member.getUser(), page, total));

                database.getUsersWhoRated(member.getUser()).forEach((l, s) -> {
                    User user = event.getJDA().getUserById(l);
                    if(user == null)
                        pBuilder.addItems("UNKNOWN (ID: "+l+") - "+s);
                    else
                        pBuilder.addItems("**"+user.getName()+"**#"+user.getDiscriminator()+" - "+s);
                });

                if(member.getColor() != null)
                    pBuilder.setColor(member.getColor());

                pBuilder.build().display(message);
            });
        }
    }
}
