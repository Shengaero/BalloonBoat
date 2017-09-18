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

import com.jagrosh.jdautilities.commandclient.Command;
import party.balloonboat.data.Database;

/**
 * @author Kaidan Gustave
 */
public abstract class DatabaseCommand extends Command
{
    // Database Wrapper Command for BalloonBoat commands using the database

    final Database database;

    public DatabaseCommand(Database database)
    {
        this.database = database;
    }
}
