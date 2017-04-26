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

package com.acciente.securetodo.api;

import com.acciente.securetodo.resources.TodoItemResource;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.ws.rs.core.UriBuilder;

@JsonAutoDetect
public class TodoItem {

   private final long id;
   private final String title;
   private final Boolean completed;
   private final String url;

   private TodoItem() {
      id = -1;
      title = null;
      completed = null;
      url = null;
   }

   public TodoItem(long id, String title, Boolean completed) {
      this.id = id;
      this.title = title;
      this.completed = completed;
      this.url = UriBuilder.fromResource(TodoItemResource.class).path("{todoId}").build(id).toString();
   }

   public long getId() {
      return id;
   }

   public String getTitle() {
      return title;
   }

   public Boolean getCompleted() {
      return completed;
   }

   public String getUrl() {
      return url;
   }

   public TodoItem getPatchedInstance(TodoItem patchItem) {
      return new TodoItem(id,
                          patchItem.title == null ? title : patchItem.title,
                          patchItem.completed == null ? completed : patchItem.completed);
   }

   // Method that must return true for the object to be valid in the context of patching
   @JsonIgnore
   public boolean isPatch() {
      return !((title == null) && (completed == null));
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      TodoItem todoItem = (TodoItem) o;

      if (id != todoItem.id) {
         return false;
      }
      if (title != null ? !title.equals(todoItem.title) : todoItem.title != null) {
         return false;
      }
      if (completed != null ? !completed.equals(todoItem.completed) : todoItem.completed != null) {
         return false;
      }
      return url != null ? url.equals(todoItem.url) : todoItem.url == null;

   }

   @Override
   public int hashCode() {
      int result = (int) (id ^ (id >>> 32));
      result = 31 * result + (title != null ? title.hashCode() : 0);
      result = 31 * result + (completed != null ? completed.hashCode() : 0);
      result = 31 * result + (url != null ? url.hashCode() : 0);
      return result;
   }

   public interface ValidateCreate {
   }
   public interface ValidateRead {
   }
   public interface ValidateUpdate {
   }
}
