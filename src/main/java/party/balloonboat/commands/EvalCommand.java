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
import net.dv8tion.jda.core.entities.ChannelType;
import party.balloonboat.data.Database;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * @author Kaidan Gustave
 */
public class EvalCommand extends DatabaseCommand
{
    private final ScriptEngine engine;

    public EvalCommand(Database database)
    {
        super(database);
        this.name = "Eval";
        this.aliases = new String[]{"Evaluate"};
        this.arguments = "<Script>";
        this.help = "Evaluates script using Nashorn.";
        this.guildOnly = false;
        this.ownerCommand = true;
        this.usesTopicTags = false;

        this.engine = new ScriptEngineManager().getEngineByName("nashorn");
    }

    @Override
    protected void execute(CommandEvent event)
    {
        engine.put("jda", event.getJDA());
        engine.put("channel", event.getChannel());
        engine.put("author", event.getAuthor());
        engine.put("selfUser", event.getSelfUser());
        engine.put("client", event.getClient());
        engine.put("database", database);

        if(event.isFromType(ChannelType.TEXT))
        {
            engine.put("member", event.getMember());
            engine.put("guild", event.getGuild());
            engine.put("textChannel", event.getTextChannel());
            engine.put("selfMember", event.getSelfMember());
        }
        else
            engine.put("privateChannel", event.getPrivateChannel());

        try {
            event.reply("```js\n"+event.getArgs()+"``` Evaluated: ```\n"+engine.eval(event.getArgs())+"```");
        } catch(ScriptException e) {
            event.reply("```js\n"+event.getArgs()+"``` A ScriptException was thrown: ```\n"+e.getMessage()+"```");
        } catch(Exception e) {
            event.reply("```js\n"+event.getArgs()+"``` Exception: ```\n"+e.getMessage()+"```");
        }
    }
}
