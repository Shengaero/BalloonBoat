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
package party.balloonboat.utils;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;

import java.util.List;

/**
 * @author Kaidan Gustave
 */
public class FormatUtils
{
    public static String tooManyMembers(String query, List<Member> members)
    {
        StringBuilder builder = new StringBuilder("Found multiple members matching \""+query+"\":\n");

        for(int i = 0; i < 5 && i < members.size(); i++)
        {
            builder.append("- ").append(String.format("%#s", members.get(i).getUser())).append("\n");
            if(i == 4 && members.size() != i)
                builder.append("**And ").append(members.size() - 5).append(" more...**");
        }

        return builder.toString();
    }

    public static String tooManyRoles(String query, List<Role> roles)
    {
        StringBuilder builder = new StringBuilder("Found multiple roles matching \""+query+"\":\n");

        for(int i = 0; i < 5 && i < roles.size(); i++)
        {
            builder.append("- ").append(roles.get(i).getName()).append("\n");
            if(i == 4 && roles.size() != i)
                builder.append("**And ").append(roles.size() - 5).append(" more...**");
        }

        return builder.toString();
    }
}
