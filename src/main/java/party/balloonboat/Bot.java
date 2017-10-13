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
package party.balloonboat;

import com.jagrosh.jdautilities.commandclient.CommandClientBuilder;
import com.jagrosh.jdautilities.waiter.EventWaiter;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.ReadyEvent;
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
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * @author Kaidan Gustave
 */
public class Bot extends ListenerAdapter
{
    public static final Logger LOG = LoggerFactory.getLogger(Bot.class);

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

        database = new Database(
                config.getDatabasePathname(),
                config.getDatabaseUsername(),
                config.getDatabasePassword(),
                config.getWebhookId(),
                config.getWebhookToken()
        );

        database.init();

        // Event Waiter
        EventWaiter waiter = new EventWaiter();

        CommandClientBuilder builder = new CommandClientBuilder();

        // No default help
        builder.useHelpBuilder(false);

        builder.addCommands(
                new AboutCommand(database, Config.PERMISSIONS),
                new FromCommand(database, waiter),
                new HelpCommand(),
                new LinkRoleCommand(database),
                new PingCommand(),
                new RankCommand(database),
                new RateCommand(database),
                new ToCommand(database, waiter),

                new EvalCommand(database),
                new ModeCommand(),
                new ShutdownCommand(database)
        );

        builder.setPrefix(Config.PREFIX);
        builder.setPlaying(Config.GAME);
        builder.setServerInvite(Config.HUB_SERVER_INVITE);
        builder.setOwnerId(config.getJagroshId());    // jagrosh
        builder.setCoOwnerIds(config.getMonitorId()); // monitor

        builder.setEmojis(Config.SUCCESS_EMOJI, Config.WARNING_EMOJI, Config.ERROR_EMOJI);

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
                .addEventListener(builder.build(), waiter, this)
                .buildAsync();
    }

    @Override
    public void onReady(ReadyEvent event)
    {
        LOG.info("Starting Top Rating Updater...");
        database.updateTopRatings(
                event.getJDA().getTextChannelById(Config.TOP_USERS_CHAN_ID)
                .getMessageById(Config.TOP_USERS_MSG_ID).complete(),
                5, TimeUnit.MINUTES
        );

        database.updateRoles(event.getJDA(), 10, TimeUnit.MINUTES);
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
        // Add roles to new member

        short rating = database.getUserRating(event.getUser());

        if(rating == -1)
            return;

        Role role = database.getRatingRole(event.getGuild(), rating);

        // Role is not null
        // Can interact with role
        // Can interact with member
        if(role != null && event.getGuild().getSelfMember().canInteract(role)
                && event.getGuild().getSelfMember().canInteract(event.getMember()))
        {
            event.getGuild().getController().addRolesToMember(event.getMember(), role).queue(v -> {}, v -> {});
        }
    }

    public static class Config
    {
        // Static Configurations
        public static final String PREFIX = "b-";
        public static final String GAME = "Type b-help";
        public static final String SUCCESS_EMOJI = "<:BalloonSuccess:359189166149599233>";
        public static final String WARNING_EMOJI = "<:BalloonWarning:359189166531018762>";
        public static final String ERROR_EMOJI = "<:BalloonError:359189166439006209>";
        public static final String BOT_EMOJI = "<:BalloonBoat:359188538392313878>";
        public static final String HUB_SERVER_INVITE = "https://discord.gg/A2XmF9a";
        public static final long TOP_USERS_CHAN_ID = 359187922928402444L;
        public static final long TOP_USERS_MSG_ID = 362750549688320000L;

        public static final Permission[] PERMISSIONS = new Permission[] {
                Permission.MANAGE_ROLES,
                Permission.MESSAGE_EMBED_LINKS,
                Permission.MESSAGE_ADD_REACTION,
                Permission.MESSAGE_MANAGE
        };

        public static final Color BLURPLE_COLOR = Color.decode("#7289DA");

        // Non-Static Configurations
        private final long jagroshId;
        private final long monitorId;
        private final String token;
        private final String databaseUsername;
        private final String databasePassword;
        private final String databasePathname;
        private final long webhookId;
        private final String webhookToken;
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
            this.webhookId = json.getLong("webhook_id");
            this.webhookToken = json.getString("webhook_token");

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

        public long getWebhookId()
        {
            return webhookId;
        }

        public String getWebhookToken()
        {
            return webhookToken;
        }
    }
}