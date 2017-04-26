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

package com.acciente.securetodo.db;

import com.acciente.securetodo.api.TodoItem;
import com.acciente.securetodo.db.mappers.TodoItemMapper;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;

import java.util.Collection;
import java.util.List;

@UseStringTemplate3StatementLocator
@RegisterMapper(TodoItemMapper.class)
public interface TodoItemDAO {
   @SqlUpdate("INSERT INTO todo.todoItem(title, completed) VALUES (:title, CASE WHEN :completed IS NULL THEN FALSE ELSE :completed END )")
   @GetGeneratedKeys
   long insert(@BindBean TodoItem newTodoItem);

   @SqlUpdate("UPDATE todo.todoItem SET title = :title, completed = :completed WHERE id = :id")
   int update(@BindBean TodoItem todoItem);

   @SqlUpdate("DELETE FROM todo.todoItem WHERE id = :id")
   int delete(@Bind("id") long id);

   @SqlQuery("SELECT * FROM todo.todoItem WHERE id = :id")
   TodoItem findById(@Bind("id") long id);

   @SqlQuery("SELECT * FROM todo.todoItem WHERE id IN (<ids>)")
   List<TodoItem> findByIds(@BindIn("ids") Collection<Long> ids);
}
