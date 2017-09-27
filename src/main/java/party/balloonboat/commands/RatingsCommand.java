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
import com.jagrosh.jdautilities.menu.pagination.PaginatorBuilder;
import com.jagrosh.jdautilities.utils.FinderUtil;
import com.jagrosh.jdautilities.waiter.EventWaiter;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import party.balloonboat.data.Database;
import party.balloonboat.utils.FormatUtils;

import java.util.List;

/**
 * @author Kaidan Gustave
 */
public class RatingsCommand extends DatabaseCommand
{
    private final PaginatorBuilder pBuilder;

    public RatingsCommand(Database database, EventWaiter waiter)
    {
        super(database);
        this.name = "Ratings";
        this.arguments = "<User>";
        this.help = "Gets a list of ratings by a user.";
        this.cooldown = 10;
        this.cooldownScope = CooldownScope.USER;
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

            if(members.size() > 1)
            {
                event.replyError(FormatUtils.tooManyMembers(event.getArgs(),members));
                return;
            }

            if(members.size() < 1)
            {
                event.replyError("Could not find a user matching \""+event.getArgs()+"\"!");
                return;
            }

            member = members.get(0);
        }

        event.replyWarning("Getting Ratings...", message -> {

            pBuilder.clearItems();
            pBuilder.setText((page, total) ->
                    String.format("Ratings made by %#s | Page %d/%d", member.getUser(), page, total));

            database.getRatingsByUser(member.getUser(), event.getJDA())
                    .forEach((user, rating) -> pBuilder.addItems(String.format("%#s %d", user, rating)));

            if(member.getColor() != null)
                pBuilder.setColor(member.getColor());

            pBuilder.build().display(message);
        });
    }
}
