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
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.utils.SimpleLog;

import javax.annotation.Nullable;
import java.sql.*;
import java.util.function.Consumer;

/**
 * @author Kaidan Gustave
 */
@SuppressWarnings("unused")
public class Database
{
    public static final SimpleLog LOG = SimpleLog.getLog("SQL");

    private final Connection connection;
    private final RatingsTable ratings;
    private final GuildSettingsTable guildSettings;

    public Database(String user, String pass, String url)
            // TODO Simplify this so that it only throws critical errors
            throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException
    {
        Class.forName("org.h2.Driver").newInstance();

        connection = DriverManager.getConnection(user, pass, url);

        ratings = new RatingsTable(connection);
        guildSettings = new GuildSettingsTable(connection);
    }

    public boolean init()
    {
        for(Table table : Table.values())
        {
            try {
                try (ResultSet results = connection.getMetaData().getTables(null, null, table.name(), null))
                {
                    // Only create if there is no table
                    if(!results.next())
                        table.createUsing(connection);
                }
            } catch(SQLException e) {
                LOG.warn("Failed to create table "+table.name());
                LOG.warn(e);
                return false;
            }
        }
        return true;
    }

    public short getRating(User user, User target)
    {
        try {
            return ratings.getRating(user, target);
        } catch(SQLException e) {
            LOG.warn(e);
            return -1;
        }
    }

    public void setRating(User user, User target, short rating)
    {
        try {
            ratings.setRating(user, target, rating);
        } catch(SQLException e) {
            LOG.warn(e);
        }
    }

    @Nullable
    public Role getRatingRole(Guild guild, short number)
    {
        Role role;
        try {
            long roleId = guildSettings.getRoleId(guild, number);
            if(roleId == -1L)
                role = null;
            else
                role = guild.getRoleById(roleId);
        } catch(SQLException e) {
            LOG.warn(e);
            role = null;
        }
        return role;
    }

    public void setRatingRole(Role role, short number)
    {
        try {
            guildSettings.setRole(role, number);
        } catch(SQLException e) {
            LOG.warn(e);
        }
    }

    @SuppressWarnings("unused")
    public void evaluate(String sql) throws SQLException
    {
        try (Statement statement = connection.createStatement())
        {
            statement.execute(sql);
        }
    }

    @SuppressWarnings("unused")
    public void evaluate(String sql, Consumer<ResultSet> resultConsumer) throws SQLException
    {
        try (Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE))
        {
            try (ResultSet results = statement.executeQuery(sql))
            {
                resultConsumer.accept(results);
            }
        }
    }

    public void shutdown()
    {
        try {
            connection.close();
        } catch(SQLException e) {
            LOG.warn(e);
        }
    }

    // Column Data Holder And Table Generator Enum
    public enum Table
    {
        RATINGS(
                "LONG USER_ID",
                "LONG TARGET_ID",
                "SHORT RATING"
        ),

        CALCULATED(
                "LONG USER_ID",
                "DOUBLE TRUE_RATING",
                "SHORT EFFECTIVE_RATING"
        ),

        PRIVATE_SETTINGS(
                "LONG USER_ID",
                "BOOLEAN DM_CHANGES"
        ),

        GUILD_SETTINGS(
                "LONG GUILD_ID",
                "LONG ROLE_1",
                "LONG ROLE_2",
                "LONG ROLE_3",
                "LONG ROLE_4",
                "LONG ROLE_5"
        );

        private final String[] typeAndColumn;

        Table(String... typeAndColumn)
        {
            this.typeAndColumn = typeAndColumn;
        }

        public void createUsing(Connection connection) throws SQLException
        {
            try (Statement statement = connection.createStatement()) {
                StringBuilder builder = new StringBuilder();
                for(int i = 0; i < typeAndColumn.length; i++)
                {
                    builder.append(typeAndColumn[i]);
                    if(i != typeAndColumn.length -1)
                        builder.append(", ");
                }
                // Execute SQL statement
                statement.execute("CREATE TABLE "+this.name()+" ("+builder.toString()+")");
            }
        }
    }
}
