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

import net.dv8tion.jda.core.utils.SimpleLog;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author Kaidan Gustave
 */
public class Database
{
    public static final SimpleLog LOG = SimpleLog.getLog("SQL");

    private final Connection connection;
    private final RatingsTable ratings;

    public Database(String user, String pass, String url)
            throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException
    {
        Class.forName("org.h2.Driver").newInstance();

        connection = DriverManager.getConnection(user, pass, url);

        ratings = new RatingsTable(connection);

        // TODO Automatic table setup
    }

    public Connection getConnection() {
        return connection;
    }

    public RatingsTable getRatings() {
        return ratings;
    }
}
