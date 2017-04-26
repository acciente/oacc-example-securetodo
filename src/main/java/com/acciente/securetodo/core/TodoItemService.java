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

package com.acciente.securetodo.core;

import com.acciente.oacc.AccessControlContext;
import com.acciente.oacc.Resource;
import com.acciente.oacc.Resources;
import com.acciente.securetodo.api.TodoItem;
import com.acciente.securetodo.db.TodoItemDAO;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TodoItemService {
   public static final EmailValidator EMAIL_VALIDATOR = new EmailValidator();

   private final TodoItemDAO todoItemDAO;

   public TodoItemService(TodoItemDAO todoItemDAO) {
      this.todoItemDAO = todoItemDAO;
   }

   public TodoItem createItem(AccessControlContext oacc,
                              TodoItem newTodoItem) {
      assertTodoItemIsValidForCreation(newTodoItem);

      // add the new todo item to our application domain table
      final long newId = todoItemDAO.insert(newTodoItem);

      final TodoItem todoItem;
      try {
         // look up the created todo item so we can return it
         todoItem = todoItemDAO.findById(newId);

         // register the created todo item as a secured object in OACC
         oacc.createResource(SecurityModel.RESOURCECLASS_TODO,
                             SecurityModel.DOMAIN_SECURE_TODO,
                             String.valueOf(todoItem.getId()));
      }
      catch (Exception e) {
         // something went wrong, so let's try to undo the todoItem creation
         todoItemDAO.delete(newId);
         throw e;
      }

      return todoItem;
   }

   public List<TodoItem> findByAuthenticatedUser(AccessControlContext oacc) {
      // find all todoItem resources which the authenticated user is authorized to view
      final Set<Resource> todoItemResources = oacc.getResourcesByResourcePermissions(oacc.getSessionResource(),
                                                                                     SecurityModel.RESOURCECLASS_TODO,
                                                                                     SecurityModel.PERM_VIEW);
      // return the corresponding list of todoItems
      if (todoItemResources.isEmpty()) {
         return Collections.emptyList();
      }
      else {
         // convert list of oacc resources to list of IDs from the application domain
         final List<Long> todoItemIds = todoItemResources.stream()
               .map(todoItemResource -> Long.valueOf(todoItemResource.getExternalId()))
               .collect(Collectors.toList());

         // look up the todo items in the application domain table and return them
         return todoItemDAO.findByIds(todoItemIds);
      }
   }

   public void shareItem(AccessControlContext oacc,
                         long todoItemId,
                         String email) {
      assertEmailIsValid(email);

      // "share" todoItem with other user
      oacc.grantResourcePermissions(Resources.getInstance(email.toLowerCase()),
                                    Resources.getInstance(String.valueOf(todoItemId)),
                                    SecurityModel.PERM_VIEW, SecurityModel.PERM_MARK_COMPLETED);
   }

   public TodoItem updateItem(AccessControlContext oacc,
                              long todoItemId,
                              TodoItem patchItem) {
      assertTodoItemIsValidForUpdate(patchItem);

      // check permission
      oacc.assertResourcePermissions(oacc.getSessionResource(),
                                     Resources.getInstance(String.valueOf(todoItemId)),
                                     SecurityModel.PERM_VIEW,
                                     patchItem.getTitle() != null
                                     ? SecurityModel.PERM_EDIT
                                     : SecurityModel.PERM_MARK_COMPLETED);

      // load existing todoItem
      final TodoItem currentTodoItem = todoItemDAO.findById(todoItemId);

      // get updated value object with new data
      final TodoItem todoItem = currentTodoItem.getPatchedInstance(patchItem);

      // update database
      todoItemDAO.update(todoItem);

      return todoItem;
   }

   private static void assertEmailIsValid(String email) {
      Objects.requireNonNull(email, "Email is required.");

      if (email.trim().length() != email.length()) {
         throw new IllegalArgumentException("Email can not start or end with whitespace.");
      }
      if (!EMAIL_VALIDATOR.isValid(email, null)) {
         throw new IllegalArgumentException("Email must be a well-formed email address.");
      }
   }

   private static void assertTodoItemIsValidForCreation(TodoItem todoItem) {
      Objects.requireNonNull(todoItem, "Todo item is required.");

      final String rawTitle = todoItem.getTitle();
      if (rawTitle == null) {
         throw new IllegalArgumentException("Title is required.");
      }
      if (rawTitle.trim().isEmpty()) {
         throw new IllegalArgumentException("Title can not be blank.");
      }
   }

   private static void assertTodoItemIsValidForUpdate(TodoItem todoItem) {
      Objects.requireNonNull(todoItem, "Todo item is required.");

      if (!todoItem.isPatch()) {
         throw new IllegalArgumentException("Either title or completed is required.");
      }

      final String title = todoItem.getTitle();
      if (title != null) {
         if (title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title can not be blank.");
         }
      }
   }
}