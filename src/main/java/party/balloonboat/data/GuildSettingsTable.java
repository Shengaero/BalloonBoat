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

import java.sql.Connection;

/**
 * @author Kaidan Gustave
 */
public class GuildSettingsTable extends Table
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

    GuildSettingsTable(Connection connection, String name) {
        super(connection, name);
    }

    @Override
    public void create() {

    }
}
