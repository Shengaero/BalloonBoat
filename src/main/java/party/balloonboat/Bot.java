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

import com.jagrosh.jdautilities.commandclient.CommandClient;
import com.jagrosh.jdautilities.commandclient.CommandClientBuilder;
import com.jagrosh.jdautilities.commandclient.examples.PingCommand;
import com.jagrosh.jdautilities.commandclient.examples.ShutdownCommand;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import org.json.JSONArray;
import org.json.JSONObject;
import party.balloonboat.commands.EvalCommand;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Kaidan Gustave
 */
public class Bot
{
    public static void main(String[] args) throws IOException, LoginException, RateLimitedException {
        JSONObject json = new JSONObject(
                new String(Files.readAllBytes(Paths.get(System.getProperty("user.dir"),"config.json")))
        );

        JSONArray ownerIds = json.getJSONArray("owner_ids");

        CommandClientBuilder builder = new CommandClientBuilder();

        builder.setPrefix("b-");

        builder.setPlaying("Type b-help");

        builder.setOwnerId(String.valueOf(ownerIds.getLong(0)));    // jagrosh

        builder.setCoOwnerIds(String.valueOf(ownerIds.getLong(1))); // monitor

        builder.setEmojis(Config.SUCCESS_EMOJI, Config.WARNING_EMOJI, Config.ERROR_EMOJI);

        builder.addCommands(
                new PingCommand(),
                new EvalCommand(),
                new ShutdownCommand()
        );

        //builder.setDiscordBotsKey(json.getString("discord_bots_key"));
        //builder.setCarbonitexKey(json.getString("carbonitex_key"));
        //builder.setDiscordBotListKey(json.getString("discord_bots_list_key"));

        CommandClient client = builder.build();

        new JDABuilder(AccountType.BOT)
                .setToken(json.getString("token"))
                .addEventListener(client)
                .buildAsync();
    }

    public static class Config
    {
        public static final String SUCCESS_EMOJI = "<:BalloonSuccess:359189166149599233>";
        public static final String WARNING_EMOJI = "<:BalloonWarning:359189166531018762>";
        public static final String ERROR_EMOJI = "<:BalloonError:359189166439006209> ";

    }
}
