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
package party.balloonboat.data;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Kaidan Gustave
 */
public class GuildSettingsTable extends TableHandler
{
    /*
     * GUILD_SETTINGS
     * col long GUILD_ID
     * col long ROLE_1
     * col long ROLE_2
     * col long ROLE_3
     * col long ROLE_4
     * col long ROLE_5
     */

    public GuildSettingsTable(Connection connection)
    {
        super(connection, Database.Table.GUILD_SETTINGS);
    }

    public boolean hasRow(Guild guild) throws SQLException
    {
        boolean returns;
        try (Statement statement = connection.createStatement())
        {
            try (ResultSet results = statement.executeQuery("SELECT * FROM "+table.name()+" WHERE GUILD_ID = "+guild.getIdLong()))
            {
                // if there is a row then it will be true, if not it will be false
                returns = results.next();
            }
        }
        return returns;
    }

    public long getRoleId(Guild guild, short number) throws SQLException
    {
        Long l = select("ROLE_"+number, "GUILD_ID = "+guild.getIdLong());

        // returns -1 if the query provided null OR if the query pointed to an unset
        // role ID (unset role IDs are saved as 0L)
        return l == null || l == 0L ? -1L : l;
    }

    public void setRole(Role role, short number) throws SQLException
    {
        // If the guild already has a row already we want to update it not create a new one
        if(hasRow(role.getGuild()))
            update(role.getGuild().getIdLong(), role.getIdLong(), number);
        else
            add(role.getGuild().getIdLong(), role.getIdLong(), number);
    }

    private void update(long guildId, long roleId, short number) throws SQLException
    {
        try (Statement statement = connection.createStatement())
        {
            statement.execute("UPDATE "+table.name()+" SET ROLE_"+number+" = "+roleId+" WHERE GUILD_ID = "+guildId);
        }
    }

    private void add(long guildId, long roleId, short number) throws SQLException
    {
        try (Statement statement = connection.createStatement())
        {
            StringBuilder valueB = new StringBuilder("(").append(guildId).append(", ");

            for(int i = 1; i <= 5; i++)
            {
                // Unset role ID's are saved as 0L
                if(number != i)
                    valueB.append(0L);
                else
                    valueB.append(roleId);

                // Last parameter is doesn't require a comma after it
                if(i < 5)
                    valueB.append(", ");
            }
            valueB.append(")");

            String values = valueB.toString();

            // Concat and execute SQL String with values.
            statement.execute("INSERT INTO "+table.name()+" (GUILD_ID, ROLE_1, ROLE_2, ROLE_3, ROLE_4, ROLE_5) VALUES "+values);
        }
    }
}
