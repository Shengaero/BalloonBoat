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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Kaidan Gustave
 */
@SuppressWarnings("unused")
public class CalculationsTable extends TableHandler
{
    public CalculationsTable(Connection connection)
    {
        super(connection, Database.Table.CALCULATIONS);
    }

    public boolean isRegistered(long userId) throws SQLException
    {
        final boolean isRegistered;
        try (Statement statement = connection.createStatement())
        {
            try (ResultSet results = statement.executeQuery(
                    "SELECT * FROM "+table.name()+" WHERE USER_ID = "+userId
            ))
            {
                isRegistered = results.next();
            }
        }
        return isRegistered;
    }

    public void registerUser(long userId, double trueRating, short effectiveRating) throws SQLException
    {
        try(Statement statement = connection.createStatement())
        {
            statement.execute(
                    "INSERT INTO "+table.name()+" (USER_ID, TRUE_RATING, EFFECTIVE_RATING) VALUES " +
                    "("+userId+","+trueRating+","+effectiveRating+")"
            );
        }
    }

    public short getUserRating(long userId) throws SQLException
    {
        final short returns;
        try (Statement statement = connection.createStatement())
        {
            try (ResultSet results = statement.executeQuery(
                    "SELECT EFFECTIVE_RATING FROM "+table.name()+" " +
                    "WHERE USER_ID = "+userId
            ))
            {
                if(results.next())
                    returns = results.getShort("EFFECTIVE_RATING");
                else
                {
                    returns = 1;
                    registerUser(userId, 1.0, returns);
                }
            }
        }
        return returns;
    }

    public void setUserRating(long userId, double trueRating) throws SQLException
    {
        setAndReturnUserRating(userId, trueRating);
    }

    short setAndReturnUserRating(long userId, double trueRating) throws SQLException
    {
        final short returns = (short)Math.round(trueRating);
        try (Statement statement = connection.createStatement())
        {
            if(isRegistered(userId))
            {
                statement.execute(
                        "UPDATE "+table.name()+" " +
                        "SET TRUE_RATING = "+trueRating+", EFFECTIVE_RATING = "+returns+" " +
                        "WHERE USER_ID = "+userId
                );
            }
            else
            {
                registerUser(userId, trueRating, returns);
            }
        }
        return returns;
    }
}
