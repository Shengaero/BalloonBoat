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

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.utils.cache.SnowflakeCacheView;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import party.balloonboat.Bot;

import javax.annotation.Nullable;
import java.sql.*;
import java.time.temporal.ChronoUnit;
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
    public static final Logger LOG = LoggerFactory.getLogger(Database.class);
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor();
    private static final String WEBHOOK_FORMAT = "%s **%s**#%s (ID: %d) rated **%s**#%s (ID: %d) as `%d`";

    private final Connection connection;

    private final CalculationsTable calcTable;
    private final RatingsTable ratings;
    private final GuildSettingsTable guildSettings;
    private final PrivateSettingsTable privateSettings;
    private final WebhookClient webhook;

    public Database(String url, String user, String pass, long webhookId, String webhookToken)
            throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException
    {
        Class.forName("org.h2.Driver").newInstance();

        connection = DriverManager.getConnection(url, user, pass);

        calcTable = new CalculationsTable(connection);
        ratings = new RatingsTable(connection, calcTable);
        guildSettings = new GuildSettingsTable(connection);
        privateSettings = new PrivateSettingsTable(connection);

        webhook = new WebhookClientBuilder(webhookId, webhookToken).setExecutorService(EXECUTOR).build();
    }

    public void init() throws SQLException
    {
        for(Table table : Table.values())
        {
            try (ResultSet results = connection.getMetaData().getTables(null, null, table.name(), null))
            {
                // Only create if there is no table
                if(!results.next())
                    table.createUsing(connection);
            }
        }
    }

    // Only fire this once!
    public void updateTopRatings(Message message, long delay, TimeUnit unit)
    {
        EmbedBuilder b = new EmbedBuilder();

        b.setTitle(Bot.Config.SUCCESS_EMOJI + " __**TOP RATED USERS**__ " + Bot.Config.SUCCESS_EMOJI);

        try {
            int i = 1;
            for(Long userId : calcTable.getTop20())
            {
                b.appendDescription("`"+i+"` - ");
                Member member = message.getGuild().getMemberById(userId);
                if(member != null)
                    b.appendDescription(member.getAsMention());
                else
                    b.appendDescription("<@"+userId+">");

                for(short balloons = 1; balloons <= getUserRating(userId); balloons++)
                    b.appendDescription(" ").appendDescription(Bot.Config.BOT_EMOJI);

                b.appendDescription("\n");

                i++;
            }
        } catch(SQLException e) {
            LOG.warn("Encountered an SQLException: ",e);

            // Retry again later
            EXECUTOR.schedule(() -> updateTopRatings(message,delay,unit), delay, unit);
            return;
        }

        b.setColor(message.getGuild().getSelfMember().getColor());
        b.setFooter("Last Updated", null);
        b.setTimestamp(message.getEditedTime().plus(delay, ChronoUnit.MINUTES));

        // Update again later
        message.editMessage(b.build()).queueAfter(
                delay, unit,
                msg -> updateTopRatings(message, delay, unit),
                err -> updateTopRatings(message, delay, unit),
                EXECUTOR
        );
    }

    // Only fire this once!
    public void updateRoles(JDA jda, long delay, TimeUnit unit)
    {
        final SnowflakeCacheView<Guild> guilds;
        if(jda.getShardInfo() != null)
            guilds = jda.asBot().getShardManager().getGuildCache();
        else
            guilds = jda.getGuildCache();

        guilds.forEach(guild -> {
            for(short rating = 1; rating <= 5; rating++)
            {
                short r = rating;
                Role role = getRatingRole(guild, r);

                // There is a role
                if(role != null)
                {
                    guild.getMembersWithRoles(role).stream().filter(member -> {
                        return getUserRating(member.getUser()) != r; // Has a role that doesn't represent their rank
                    }).forEach(member ->
                            guild.getController().removeRolesFromMember(member, role).queue(v -> {}, v -> {}));

                    getMembersByRating(r, guild).stream().filter(member -> {
                        return !member.getRoles().contains(role); // Doesn't have the proper role
                    }).forEach(member ->
                            guild.getController().addRolesToMember(member, role).queue(v -> {}, v -> {}));
                }
            }
        });

        EXECUTOR.schedule(() -> updateRoles(jda, delay, unit), delay, unit);
    }

    public boolean ratingEquals(User user, User target, short rating)
    {
        try {
            return ratings.hasRated(user.getIdLong(), target.getIdLong()) && getRating(user, target) == rating;
        } catch(SQLException e) {
            LOG.warn("Encountered an SQLException: ",e);
            return false;
        }
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
            return calcTable.getUserRating(userId, false);
        } catch(SQLException e) {
            LOG.warn("Encountered an SQLException: ",e);
            return -1;
        }
    }

    public void setRating(User user, User target, short rating)
    {
        try {
            // Sends rating to the database
            ratings.setRating(user.getIdLong(), target.getIdLong(), rating);

            StringBuilder msg = new StringBuilder(String.format(WEBHOOK_FORMAT, Bot.Config.SUCCESS_EMOJI,
                    user.getName(), user.getDiscriminator(), user.getIdLong(),
                    target.getName(), target.getDiscriminator(), target.getIdLong(),
                    rating));

            for(short balloons = 1; balloons <= getUserRating(user); balloons++)
            {
                msg.append(" " + Bot.Config.BOT_EMOJI);
            }

            webhook.send(msg.toString());
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

    public Map<Long, Short> getRatingsTo(User user)
    {
        return getRatingsTo(user.getIdLong());
    }

    public Map<Long, Short> getRatingsTo(long userId)
    {
        try {
            return ratings.getAllUsersRating(userId);
        } catch(SQLException e) {
            LOG.warn("Encountered an SQLException: ",e);
            return Collections.emptyMap();
        }
    }

    public Map<Long, Short> getRatingsFrom(User user)
    {
        return getRatingsFrom(user.getIdLong());
    }

    public Map<Long, Short> getRatingsFrom(long userId)
    {
        try {
            return ratings.getRatingsByUser(userId);
        } catch(SQLException e) {
            LOG.warn("Encountered an SQLException: ",e);
            return Collections.emptyMap();
        }
    }

    @Nullable
    public Role getRatingRole(Guild guild, short number)
    {
        final Role role;
        try {
            long roleId = guildSettings.getRoleId(guild, number);
            if(roleId == -1L)
                role = null;
            else
                role = guild.getRoleById(roleId);
        } catch(SQLException e) {
            LOG.warn("Encountered an SQLException: ",e);
            return null;
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


    // IMPORTANT!!
    // This has THREE return cases:
    // if this returns -1, the role has not been linked to a rating.
    // if this returns 0, there was an SQL Error, and no further action should be taken.
    // if this returns 1, 2, 3, 4, or 5, the role has been linked to a rating of the returns.
    public short getRoleRating(Role role)
    {
        try {
            return guildSettings.getRoleRating(role);
        } catch(SQLException e) {
            LOG.warn("Encountered an SQLException: ",e);
            return 0;
        }
    }

    public boolean isUsingDMChanges(User user)
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

    public double getGlobalAverage()
    {
        double returns = 0;
        try(Statement statement = connection.createStatement())
        {
            try(ResultSet results = statement.executeQuery("SELECT TRUE_RATING FROM CALCULATIONS"))
            {
                int total = 0;
                while(results.next())
                {
                    total++;
                    returns += results.getDouble("TRUE_RATING");
                }
                returns /= total;
            }
        } catch(SQLException e) {
            LOG.warn("Encountered an SQLException: ",e);
            returns = -1;
        }
        return returns;
    }

    public long getGlobalTotalRatings()
    {
        long returns = 0;
        try(Statement statement = connection.createStatement())
        {

            try(ResultSet results = statement.executeQuery("SELECT * FROM RATINGS"))
            {
                while(results.next())
                    returns += 1L;
            }
        } catch(SQLException e) {
            LOG.warn("Encountered an SQLException: ",e);
            returns = -1;
        }

        return returns;
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

    @SuppressWarnings("unused")
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
        // Close webhook
        webhook.close();

        EXECUTOR.shutdownNow();

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
