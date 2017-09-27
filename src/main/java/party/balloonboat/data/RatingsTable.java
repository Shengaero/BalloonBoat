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

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.User;
import party.balloonboat.utils.AlgorithmUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Kaidan Gustave
 */
public class RatingsTable extends TableHandler
{
    /*
     * RATINGS
     * col USER_RATING SMALLINT
     * col USER_ID LONG
     * col TARGET_ID LONG
     * col RATING SMALLINT
     */

    private final CalculationsTable calcTable;

    public RatingsTable(Connection connection, CalculationsTable calcTable)
    {
        super(connection, Database.Table.RATINGS);
        this.calcTable = calcTable;
    }

    public boolean hasRated(long userId, long targetId) throws SQLException
    {
        final boolean returns;
        try (Statement statement = connection.createStatement())
        {
            try (ResultSet set = statement.executeQuery(
                    "SELECT * FROM RATINGS WHERE USER_ID = "+userId+" AND TARGET_ID = "+targetId
            ))
            {
                returns = set.next();
            }
        }
        return returns;
    }

    public short getRating(long userId, long targetId) throws SQLException
    {
        final short returns;
        try (Statement statement = connection.createStatement())
        {
            try (ResultSet set = statement.executeQuery(
                    "SELECT RATING FROM RATINGS " +
                    "WHERE USER_ID = "+userId+" " +
                    "AND TARGET_ID = "+targetId
            ))
            {
                set.next();
                returns = set.getShort("RATING");
            }
        }
        return returns;
    }

    public Map<User, Short> getRatingsByUser(long userId, JDA jda) throws SQLException
    {
        Map<User, Short> map = new HashMap<>();
        try (Statement statement = connection.createStatement())
        {
            try (ResultSet results = statement.executeQuery(
                    "SELECT TARGET_ID, RATING FROM RATINGS WHERE USER_ID = "+userId
            ))
            {
                while(results.next())
                {
                    User user = jda.getUserById(results.getLong("TARGET_ID"));
                    if(user != null)
                        map.put(user, results.getShort("RATING"));
                }
            }
        }
        return map;
    }

    public void setRating(short userRating, long userId, long targetId, short rating) throws SQLException
    {
        // Check if we're updating a row or inserting a new one
        if(hasRated(userId, targetId))
        {
            try(Statement statement = connection.createStatement())
            {
                // Update USER_RATING and RATING simultaneously.
                statement.execute(
                        "UPDATE RATINGS SET USER_RATING = "+userRating+", RATING = "+rating+" WHERE" +
                        " USER_ID = "+userId+" AND TARGET_ID = "+targetId
                );
            }
        }
        else
        {
            try (Statement statement = connection.createStatement())
            {
                // Insertion statement
                statement.execute(
                        "INSERT INTO RATINGS (USER_RATING, USER_ID, TARGET_ID, RATING) VALUES" +
                        "("+userRating+","+userId+","+targetId+","+rating+")"
                );
            }
        }

        checkAndReevaluated(targetId);
    }

    // May recurse back up to the method above!!
    private void checkAndReevaluated(long userId) throws SQLException
    {
        List<Short[]> list = new ArrayList<>(5);

        // For ranks 5 - 1
        for(short i = 5; i >= 1; i--)
            list.add(getAllTargetRatingsForRank(i, userId));

        short userRatingBefore = calcTable.getUserRating(userId);
        double trueRating = AlgorithmUtils.calculateRating(list);
        short userRatingAfter = calcTable.setAndReturnUserRating(userId, trueRating);

        // We reevaluate the ratings of the target if and only if their effective rating
        // has changed by now.
        // While this could recurse several times potentially, it will most certainly not
        // go forever (please don't let me be wrong...)
        if(userRatingAfter != userRatingBefore)
        {
            for(long targetId : getAllTargetsRated(userId))
            {
                // RECURSION //
                setRating(userRatingAfter, userId, targetId, getRating(userId, targetId));
            }
        }
    }

    public void setRating(long userId, long targetId, short rating) throws SQLException
    {
        setRating(calcTable.getUserRating(userId), userId, targetId, rating);
    }

    public Long[] getAllTargetsRated(long userId) throws SQLException
    {
        final Long[] returns;
        try (Statement statement = connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY))
        {
            try(ResultSet set = statement.executeQuery(
                    "SELECT TARGET_ID FROM RATINGS WHERE USER_ID = "+userId
            ))
            {
                if(set.last())
                    returns = new Long[set.getRow()];
                else
                    returns = new Long[0];
                set.beforeFirst();
                for(int i = 0; i < returns.length; i++)
                {
                    set.next();
                    returns[i] = set.getLong("TARGET_ID");
                }
            }
        }
        return returns;
    }

    public Short[] getAllTargetRatingsForRank(short rank, long targetId) throws SQLException
    {
        final Short[] returns;
        try (Statement statement = connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY))
        {
            try (ResultSet set = statement.executeQuery(
                    "SELECT RATING FROM RATINGS " +
                    "WHERE USER_RATING = "+rank+" " +
                    "AND NOT USER_ID = "+targetId+" " +
                    "AND TARGET_ID = "+targetId
            ))
            {
                set.last();
                returns = new Short[set.getRow()];
                set.beforeFirst();
                for(int i = 0; i<returns.length; i++)
                {
                    set.next();
                    returns[i] = set.getShort("RATING");
                }
            }
        }
        return returns;
    }
}
