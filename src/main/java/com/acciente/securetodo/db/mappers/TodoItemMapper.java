/*
 * Copyright 2016 - 2017, Acciente LLC
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

package com.acciente.securetodo.db.mappers;

import com.acciente.securetodo.api.TodoItem;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TodoItemMapper implements ResultSetMapper<TodoItem> {
   @Override
   public TodoItem map(int rowIndex, ResultSet resultSet, StatementContext statementContext) throws SQLException {
      return new TodoItem(resultSet.getLong("id"),
                          resultSet.getString("title"),
                          resultSet.getBoolean("completed"));
   }
}
