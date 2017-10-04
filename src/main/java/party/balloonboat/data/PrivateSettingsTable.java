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

import net.dv8tion.jda.core.entities.User;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Kaidan Gustave
 */
public class PrivateSettingsTable extends TableHandler
{
    /*
     * SETTINGS
     * col long USER_ID
     * col boolean DM_CHANGES
     */

    public PrivateSettingsTable(Connection connection) {
        super(connection, Database.Table.PRIVATE_SETTINGS);
    }

    public boolean hasRow(User user) throws SQLException
    {
        boolean returns;
        try (Statement statement = connection.createStatement())
        {
            try (ResultSet results = statement.executeQuery("SELECT * FROM "+table.name()+" WHERE USER_ID = "+user.getIdLong()))
            {
                // if there is a row then it will be true, if not it will be false
                returns = results.next();
            }
        }
        return returns;
    }

    public boolean isUsingDMChanges(User user) throws SQLException
    {
        final boolean returns;
        try (Statement statement = connection.createStatement())
        {
            try (ResultSet results = statement.executeQuery("SELECT DM_CHANGES FROM "+table.name()+" " +
                                                            "WHERE USER_ID = "+user.getIdLong()))
            {
                returns = results.next() && results.getBoolean("DM_CHANGES");
            }
        }
        return returns;
    }

    public void setUsingDMChanges(User user, boolean isUsing) throws SQLException
    {
        if(hasRow(user))
            update(user.getIdLong(), isUsing);
        else
            add(user.getIdLong(), isUsing);
    }

    private void add(long userId, boolean isUsing) throws SQLException
    {
        try (Statement statement = connection.createStatement())
        {
            statement.execute("INSERT INTO "+table.name()+" (USER_ID, DM_CHANGES) VALUES ("+userId+","+isUsing+")");
        }
    }

    private void update(long userId, boolean isUsing) throws SQLException
    {
        try (Statement statement = connection.createStatement())
        {
            statement.execute("UPDATE "+table.name()+" SET DM_CHANGES = "+isUsing+" WHERE USER_ID = "+userId);
        }
    }
}
