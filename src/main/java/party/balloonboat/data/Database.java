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

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import party.balloonboat.Bot;

import javax.annotation.Nullable;
import java.sql.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Kaidan Gustave
 */
@SuppressWarnings("unused")
public class Database
{
    private static final Logger LOG = LoggerFactory.getLogger(Database.class);
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final Connection connection;

    private final CalculationsTable calcTable;
    private final RatingsTable ratings;
    private final GuildSettingsTable guildSettings;
    private final PrivateSettingsTable privateSettings;

    public Database(String url, String user, String pass)
            throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException
    {

        Class.forName("org.h2.Driver").newInstance();

        connection = DriverManager.getConnection(url, user, pass);

        calcTable = new CalculationsTable(connection);
        ratings = new RatingsTable(connection, calcTable);
        guildSettings = new GuildSettingsTable(connection);
        privateSettings = new PrivateSettingsTable(connection);
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
                LOG.warn("Failed to create table "+table.name(),e);
                return false;
            }
        }
        return true;
    }

    // Only fire this once!
    public void updateTopRatings(Message message, long delay, TimeUnit unit)
    {
        StringBuilder b = new StringBuilder();

        b.append(Bot.Config.SUCCESS_EMOJI)
         .append(" __**TOP RATED USERS**__ ")
         .append(Bot.Config.SUCCESS_EMOJI).append("\n\n");

        try {
            int i = 1;
            for(User user : calcTable.getTop5(message.getJDA()))
            {
                b.append(String.format("`%d` - **%#s**", i, user)).append("\n\n");
                i++;
            }

        } catch(SQLException e) {

            LOG.warn("Encountered an SQLException: ",e);

            // Retry again later
            executor.schedule(() -> updateTopRatings(message,delay,unit), delay, unit);
            return;
        }

        // Update again later
        message.editMessage(b.toString().trim()).queueAfter(
                delay, unit,
                msg -> updateTopRatings(message, delay, unit),
                err -> updateTopRatings(message, delay, unit),
                executor
        );
    }

    public short getRating(User user, User target)
    {
        return getRating(user.getIdLong(), target.getIdLong());
    }

    public short getRating(long userId, long targetId)
    {
        try {
            return ratings.getRating(userId, targetId);
        } catch(SQLException e) {
            LOG.warn("Encountered an SQLException: ",e);
            return -1;
        }
    }

    public short getUserRating(User user)
    {
        return getUserRating(user.getIdLong());
    }

    public short getUserRating(long userId)
    {
        try {
            return calcTable.getUserRating(userId);
        } catch(SQLException e) {
            LOG.warn("Encountered an SQLException: ",e);
            return -1;
        }
    }

    public void setRating(User user, User target, short rating)
    {
        setRating(user.getIdLong(), target.getIdLong(), rating);
    }

    public void setRating(long userId, long targetId, short rating)
    {
        try {
            ratings.setRating(userId, targetId, rating);
        } catch(SQLException e) {
            LOG.warn("Encountered an SQLException: ",e);
        }
    }

    public void setRating(short userRating, User user, User target, short rating)
    {
        setRating(userRating, user.getIdLong(), target.getIdLong(), rating);
    }

    public void setRating(short userRating, long userId, long targetId, short rating)
    {
        try {
            ratings.setRating(userRating, userId, targetId, rating);
        } catch(SQLException e) {
            LOG.warn("Encountered an SQLException: ",e);
        }
    }

    public List<Member> getMembersByRating(short rating, Guild guild)
    {
        try {
            return calcTable.getMembersByRating(rating, guild);
        } catch(SQLException e) {
            LOG.warn("Encountered an SQLException", e);
            return Collections.emptyList();
        }
    }

    public Map<User, Short> getRatingsByUser(User user, JDA jda)
    {
        return getRatingsByUser(user.getIdLong(), jda);
    }

    public Map<User, Short> getRatingsByUser(long userId, JDA jda)
    {
        try {
            return ratings.getRatingsByUser(userId, jda);
        } catch(SQLException e) {
            LOG.warn("Encountered an SQLException: ",e);
            return Collections.emptyMap();
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
            LOG.warn("Encountered an SQLException: ",e);
            role = null;
        }
        return role;
    }

    public void setRatingRole(Role role, short number)
    {
        try {
            guildSettings.setRole(role, number);
        } catch(SQLException e) {
            LOG.warn("Encountered an SQLException: ",e);
        }
    }

    public boolean isUsingDMChanges(User user, boolean isUsing)
    {
        try {
            return privateSettings.isUsingDMChanges(user);
        } catch(SQLException e) {
            LOG.warn("Encountered an SQLException: ",e);
            return false; // Default to false
        }
    }

    public void setUsingDMChanges(User user, boolean isUsing)
    {
        try {
            privateSettings.setUsingDMChanges(user, isUsing);
        } catch(SQLException e) {
            LOG.warn("Encountered an SQLException: ",e);
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

    public long getPing()
    {
        long start = System.currentTimeMillis();

        try {
            connection.getMetaData();
        } catch(SQLException e) {
            LOG.warn("Encountered an SQLException: ",e);
        }

        return System.currentTimeMillis() - start;
    }

    public void shutdown()
    {
        executor.shutdownNow();
        LOG.info("Attempting to close JDBC connection...");
        try {
            connection.close();
            LOG.info("JDBC connection has been closed!");
        } catch(SQLException e) {
            LOG.warn("Encountered an SQLException: ",e);
        }
    }

    // Column Data Holder And Table Generator Enum
    public enum Table
    {
        RATINGS(
                "USER_RATING SMALLINT",
                "USER_ID LONG",
                "TARGET_ID LONG",
                "RATING SMALLINT"
        ),

        CALCULATIONS(
                "USER_ID LONG",
                "TRUE_RATING DOUBLE",
                "EFFECTIVE_RATING SMALLINT"
        ),

        PRIVATE_SETTINGS(
                "USER_ID LONG",
                "DM_CHANGES BOOLEAN"
        ),

        GUILD_SETTINGS(
                "GUILD_ID LONG",
                "ROLE_1 LONG",
                "ROLE_2 LONG",
                "ROLE_3 LONG",
                "ROLE_4 LONG",
                "ROLE_5 LONG"
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
