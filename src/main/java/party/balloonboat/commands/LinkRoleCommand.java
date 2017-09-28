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
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;
import party.balloonboat.data.Database;
import party.balloonboat.utils.FormatUtils;

import java.util.List;

/**
 * @author Kaidan Gustave
 */
public class LinkRoleCommand extends DatabaseCommand
{
    public LinkRoleCommand(Database database)
    {
        super(database);
        this.name = "LinkRole";
        this.arguments = "[1-5] [Role]";
        this.help = "Links a role with a rating 1-5.";
        this.cooldown = 5;
        this.cooldownScope = CooldownScope.GUILD;
        this.guildOnly = true;
        this.usesTopicTags = false;
        this.userPermissions = new Permission[] { Permission.MANAGE_SERVER };
        this.botPermissions = new Permission[]  { Permission.MANAGE_ROLES  };
    }

    @Override
    protected void execute(CommandEvent event)
    {

        String[] parts = event.getArgs().split("\\s+",2);

        if(parts.length < 2)
        {
            event.replyError("Too few arguments! Try specifying a number 1 and 5 and then a role to link!");
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

        List<Role> roles = FinderUtil.findRoles(parts[1], event.getGuild());

        if(roles.size() > 1)
        {
            event.replyError(FormatUtils.tooManyRoles(parts[1],roles));
            return;
        }

        if(roles.size() < 1)
        {
            event.replyError("Could not find a role matching \""+parts[1]+"\"!");
            return;
        }

        Role role = roles.get(0);

        Role previous = database.getRatingRole(event.getGuild(), rating);

        if(previous != null && previous.equals(role))
        {
            event.replyWarning(role.getName()+" is already the role for users with a rating of `"+rating+"`!");
            return;
        }

        database.setRatingRole(role, rating);

        event.replySuccess("Set "+role.getName()+" as the role for users with a rating of `"+rating+"`!");

        database.getMembersByRating(rating, event.getGuild()).forEach(member -> {
            event.getGuild().getController().addRolesToMember(member, role).queue(v -> {}, v -> {});
        });
    }
}
