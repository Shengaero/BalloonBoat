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
public class RatingsTable extends TableHandler
{
    /*
     * RATINGS
     * col long USER_ID
     * col long TARGET_ID
     * col short RATING
     */

    public RatingsTable(Connection connection)
    {
        super(connection, Database.Table.RATINGS);
    }

    public boolean hasRow(User user, User target) throws SQLException
    {
        boolean returns;
        try (Statement statement = connection.createStatement())
        {
            try (ResultSet results = statement.executeQuery(
                    "SELECT * FROM "+table.name()+" WHERE USER_ID = "+user.getIdLong()+" AND TARGET_ID = "+target.getIdLong()))
            {
                // if there is a row then it will be true, if not it will be false
                returns = results.next();
            }
        }
        return returns;
    }

    public short getRating(User user, User target) throws SQLException
    {
        Short rating = select("RATING", "USER_ID = "+user.getIdLong(), "TARGET_ID = "+target.getIdLong());

        return rating == null ? -1 : rating; // returns -1 if there is no rating
    }

    public void setRating(User user, User target, short rating) throws SQLException
    {
        // If the pair of users have a row already we want to update it not create a new one
        if(hasRow(user,target))
            update(user.getIdLong(), target.getIdLong(), rating);
        else
            add(user.getIdLong(), target.getIdLong(), rating);
    }

    private void update(long user, long target, short rating) throws SQLException
    {
        try (Statement statement = connection.createStatement())
        {
            statement.execute("UPDATE "+table.name()+" SET RATING = "+rating+" WHERE USER_ID = "+user+" TARGET_ID = "+target);
        }
    }

    private void add(long user, long target, short rating) throws SQLException
    {
        try (Statement statement = connection.createStatement())
        {
            statement.execute("INSERT INTO "+table.name()+" (USER_ID, TARGET_ID, RATING) VALUES ("+user+","+target+","+rating+")");
        }
    }
}
