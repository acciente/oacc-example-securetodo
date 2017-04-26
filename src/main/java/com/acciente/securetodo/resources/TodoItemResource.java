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

package com.acciente.securetodo.resources;

import com.acciente.securetodo.api.TodoItem;
import com.acciente.securetodo.auth.OaccPrincipal;
import com.acciente.securetodo.core.TodoItemService;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.PATCH;
import io.dropwizard.jersey.params.LongParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/todos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TodoItemResource {
   private final TodoItemService todoItemService;

   public TodoItemResource(TodoItemService todoItemService) {
      this.todoItemService = todoItemService;
   }

   @POST
   public TodoItem createItem(@Auth OaccPrincipal oaccPrincipal,
                              TodoItem newTodoItem) {

      return todoItemService.createItem(oaccPrincipal.getAccessControlContext(), newTodoItem);
   }

   @GET
   public List<TodoItem> findByAuthenticatedUser(@Auth OaccPrincipal oaccPrincipal) {

      return todoItemService.findByAuthenticatedUser(oaccPrincipal.getAccessControlContext());
   }

   @Path("/{id}/")
   @PUT
   public void shareItem(@Auth OaccPrincipal oaccPrincipal,
                         @PathParam("id") LongParam todoItemId,
                         @QueryParam("share_with") String email) {

      todoItemService.shareItem(oaccPrincipal.getAccessControlContext(), todoItemId.get(), email);
   }

   @Path("/{id}")
   @PATCH
   // @Consumes(MediaType.APPLICATION_MERGE_PATCH_JSON)
   public TodoItem updateItem(@Auth OaccPrincipal oaccPrincipal,
                              @PathParam("id") LongParam todoItemId,
                              TodoItem patchItem) {

      return todoItemService.updateItem(oaccPrincipal.getAccessControlContext(), todoItemId.get(), patchItem);
   }
}
