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

import java.sql.*;

/**
 * @author Kaidan Gustave
 */
public abstract class TableHandler
{
    protected final Connection connection;
    protected final Database.Table table;

    public TableHandler(Connection connection, Database.Table table)
    {
        this.connection = connection;
        this.table = table;
    }

    @SuppressWarnings("unused")
    // This is really only necessary if for some reason we'd need to create an individual table
    public final void create() throws SQLException
    {
        table.createUsing(connection);
    }
}
