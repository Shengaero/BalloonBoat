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

import javax.annotation.Nullable;
import java.sql.*;

/**
 * @author Kaidan Gustave
 */
abstract class Table
{
    protected final Connection connection;
    protected final String name;

    Table(Connection connection, String name)
    {
        this.connection = connection;
        this.name = name;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public  <T> T select(String column, String... where) throws SQLException
    {
        T returns = null;
        try (Statement statement = connection.createStatement())
        {
            try (ResultSet results = statement.executeQuery("SELECT "+column+" FROM "+name+" "+mergeToString(where)))
            {
                if(results.next())
                    returns = (T) results.getObject(column);
            }
        }
        return returns;
    }

    public abstract void create();

    private static String mergeToString(String... params)
    {
        StringBuilder builder = new StringBuilder("WHERE ");
        for(int i = 0; i<params.length; i++)
        {
            builder.append(params[i]);
            if(i < params.length-1)
                builder.append(" AND ");
        }
        return builder.toString();
    }
}
