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

import party.balloonboat.utils.AlgorithmUtils;

import java.sql.*;
import java.util.*;

/**
 * @author Kaidan Gustave
 */
public class RatingsTable extends TableHandler
{
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
            try (ResultSet set = statement.executeQuery("SELECT * FROM RATINGS " +
                                                        "WHERE USER_ID = "+userId+" " +
                                                        "AND TARGET_ID = "+targetId))
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
            try (ResultSet set = statement.executeQuery("SELECT RATING FROM RATINGS " +
                                                        "WHERE USER_ID = "+userId+" " +
                                                        "AND TARGET_ID = "+targetId))
            {
                set.next();
                returns = set.getShort("RATING");
            }
        }
        return returns;
    }

    public Map<Long, Short> getRatingsByUser(long userId) throws SQLException
    {
        Map<Long, Short> map = new HashMap<>();
        try (Statement statement = connection.createStatement())
        {
            try (ResultSet results = statement.executeQuery("SELECT TARGET_ID, RATING FROM RATINGS " +
                                                            "WHERE USER_ID = "+userId))
            {
                while(results.next())
                {
                    map.put(results.getLong("TARGET_ID"), results.getShort("RATING"));
                }
            }
        }
        return map;
    }

    public void setRating(long userId, long targetId, short rating) throws SQLException
    {
        setRating(userId, targetId, rating, true);
    }

    private void setRating(long userId, long targetId, short rating, boolean recurse) throws SQLException
    {
        int oldPosition = getPosition(targetId);

        short userRating = calcTable.getUserRating(userId, true);

        if(hasRated(userId, targetId))
        {
            try(PreparedStatement statement = connection.prepareStatement(
                    "UPDATE RATINGS " +
                    "SET USER_RATING = ?, " +
                        "RATING = ? " +
                    "WHERE USER_ID = ? " +
                    "AND TARGET_ID = ?"))
            {
                statement.setShort(1, userRating);
                statement.setShort(2, rating);
                statement.setLong(3, userId);
                statement.setLong(4, targetId);
                statement.execute();
            }
        }
        else
        {
            try(PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO RATINGS (USER_RATING, USER_ID, TARGET_ID, RATING) VALUES (?, ?, ?, ?)"
            ))
            {
                statement.setShort(1, userRating);
                statement.setLong(2, userId);
                statement.setLong(3, targetId);
                statement.setShort(4, rating);
                statement.execute();
            }
        }

        calcTable.setAndReturnUserRating(targetId, calculateRating(targetId));

        int newPosition = getPosition(targetId);

        if(recurse && oldPosition == newPosition)
        {
            for(Long recTargetId : getAllTargetsRated(targetId))
                setRating(targetId, recTargetId, getRating(targetId, recTargetId), false);

            try(PreparedStatement statement = connection.prepareStatement(
                    "UPDATE RATINGS SET USER_RATING = (" +
                        "SELECT EFFECTIVE_RATING FROM CALCULATIONS " +
                        "WHERE CALCULATIONS.USER_ID = RATINGS.USER_ID" +
                    ")"
            ))
            {
                statement.execute();
            }
        }
    }

    private Set<Short> getOrderedRatings(long targetId) throws SQLException
    {
        Set<Short> ratingSet = new HashSet<>();
        try(PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM RATINGS " +
                "WHERE TARGET_ID = ? " +
                "GROUP BY(USER_ID) " +
                "ORDER BY(SELECT CALCULATIONS.TRUE_RATING " +
                         "FROM CALCULATIONS " +
                         "WHERE CALCULATIONS.USER_ID = RATINGS.USER_ID) DESC"
        ))
        {
            statement.setLong(1, targetId);
            try(ResultSet set = statement.executeQuery())
            {
                while(set.next())
                    ratingSet.add(set.getShort("RATING"));
            }
        }
        return ratingSet;
    }

    double calculateRating(long userId) throws SQLException
    {
        return AlgorithmUtils.calculateRating(getOrderedRatings(userId));
    }

    public int getPosition(long userId) throws SQLException
    {
        int row = 0;
        try(PreparedStatement statement = connection.prepareStatement(
                "SELECT USER_ID FROM CALCULATIONS ORDER BY TRUE_RATING ASC"
        ))
        {
            try(ResultSet set = statement.executeQuery())
            {
                while(set.next())
                {
                    row += 1;
                    if(set.getLong("USER_ID") == userId)
                        break;
                }
            }
        }
        return row;
    }

    // Gets all user ids rating another user id
    public Map<Long, Short> getAllUsersRating(long userId) throws SQLException
    {
        Map<Long, Short> map = new HashMap<>();
        try (Statement statement = connection.createStatement())
        {
            try (ResultSet results = statement.executeQuery("SELECT * FROM RATINGS " +
                                                            "WHERE TARGET_ID = "+userId))
            {
                while(results.next())
                    map.put(results.getLong("USER_ID"), results.getShort("RATING"));
            }
        }
        return map;
    }

    public Long[] getAllTargetsRated(long userId) throws SQLException
    {
        final Long[] returns;
        try (Statement statement = connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY))
        {
            try (ResultSet set = statement.executeQuery("SELECT TARGET_ID FROM RATINGS " +
                                                        "WHERE USER_ID = "+userId))
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
}
