/*
 * Copyright 2017 Kaidan Gustave
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
package party.balloonboat;

import com.jagrosh.jdautilities.commandclient.CommandClientBuilder;
import com.jagrosh.jdautilities.waiter.EventWaiter;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import party.balloonboat.commands.*;
import party.balloonboat.data.Database;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

/**
 * @author Kaidan Gustave
 */
@SuppressWarnings("unused")
public class Bot extends ListenerAdapter
{
    public static final Logger LOG = LoggerFactory.getLogger("Bot");

    public static void main(String[] args)
    {
        try {
            LOG.info("Initializing BalloonBoat...");
            new Bot();
        } catch(Exception e) {
            LOG.error("An exception was thrown while starting the bot!", e);
        }
    }

    private final Database database;

    private Bot() throws IOException, SQLException, JSONException,
            LoginException, RateLimitedException, IllegalAccessException,
            InstantiationException, ClassNotFoundException
    {
        Config config = new Config(Paths.get(System.getProperty("user.dir"),"config.json"));

        this.database = new Database(
                config.getDatabasePathname(),
                config.getDatabaseUsername(),
                config.getDatabasePassword()
        );

        EventWaiter waiter = new EventWaiter();

        CommandClientBuilder builder = new CommandClientBuilder();

        builder.setPrefix(Config.PREFIX);
        builder.setPlaying(Config.GAME);

        builder.setOwnerId(config.getJagroshId());    // jagrosh
        builder.setCoOwnerIds(config.getMonitorId()); // monitor

        builder.setEmojis(
                Config.SUCCESS_EMOJI,
                Config.WARNING_EMOJI,
                Config.ERROR_EMOJI);

        builder.addCommands(
                new PingCommand(),
                new EvalCommand(database),
                new RateCommand(database),
                new RatingsCommand(database, waiter),
                new ShutdownCommand(database)
        );

        // Set Discord Bots Key
        if(config.getDiscordBotsKey() != null)
            builder.setDiscordBotsKey(config.getDiscordBotsKey());

        // Set Carbonitex Key
        if(config.getCarbonitexKey() != null)
            builder.setCarbonitexKey(config.getCarbonitexKey());

        // Set Discord Bots List Key
        if(config.getDiscordBotsListKey() != null)
            builder.setDiscordBotListKey(config.getDiscordBotsListKey());

        new JDABuilder(AccountType.BOT)
                .setToken(config.getToken())
                .addEventListener(builder.build(), waiter)
                .buildAsync();
    }

    @Override
    public void onShutdown(ShutdownEvent event)
    {
        // ONLY shutdown database here
        database.shutdown();
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event)
    {
        short rating = database.getUserRating(event.getUser());

        Role role = database.getRatingRole(event.getGuild(), rating);
        if(role != null)
            event.getGuild().getController().addRolesToMember(event.getMember(), role).queue(v -> {}, v -> {});
    }

    public static class Config
    {
        // Static Configurations
        public static final String PREFIX = "b-";
        public static final String GAME = "Type b-help";
        public static final String SUCCESS_EMOJI = "<:BalloonSuccess:359189166149599233>";
        public static final String WARNING_EMOJI = "<:BalloonWarning:359189166531018762>";
        public static final String ERROR_EMOJI = "<:BalloonError:359189166439006209> ";

        // Non-Static Configurations
        private final long jagroshId;
        private final long monitorId;
        private final String token;
        private final String databaseUsername;
        private final String databasePassword;
        private final String databasePathname;
        private final String discordBotsKey;
        private final String carbonitexKey;
        private final String discordBotsListKey;

        public Config(Path path) throws IOException, JSONException
        {
            JSONObject json = new JSONObject(new String(Files.readAllBytes(path)));

            JSONArray ownerIds = json.getJSONArray("owner_ids");

            this.jagroshId = ownerIds.getLong(0);
            this.monitorId = ownerIds.getLong(1);

            this.token = json.getString("token");

            this.databaseUsername = json.getString("database_username");
            this.databasePassword = json.getString("database_password");
            this.databasePathname = json.getString("database_pathname");

            this.discordBotsKey = json.optString("discord_bots_key", null);
            this.carbonitexKey = json.optString("carbonitex_key", null);
            this.discordBotsListKey = json.optString("discord_bots_list_key", null);
        }

        public String getJagroshId()
        {
            return String.valueOf(jagroshId);
        }

        public String getMonitorId()
        {
            return String.valueOf(monitorId);
        }

        public String getToken()
        {
            return token;
        }

        public String getDatabaseUsername()
        {
            return databaseUsername;
        }

        public String getDatabasePassword()
        {
            return databasePassword;
        }

        public String getDatabasePathname()
        {
            return databasePathname;
        }

        public String getDiscordBotsKey()
        {
            return discordBotsKey;
        }

        public String getCarbonitexKey()
        {
            return carbonitexKey;
        }

        public String getDiscordBotsListKey()
        {
            return discordBotsListKey;
        }
    }
}