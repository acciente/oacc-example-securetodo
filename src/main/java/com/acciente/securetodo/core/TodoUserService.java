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
import com.acciente.oacc.PasswordCredentials;
import com.acciente.oacc.Resource;
import com.acciente.securetodo.AccessControlContextFactory;
import com.acciente.securetodo.api.TodoUser;
import com.acciente.securetodo.db.TodoUserDAO;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;

import java.util.Objects;

public class TodoUserService {
   public static final EmailValidator EMAIL_VALIDATOR = new EmailValidator();

   private final TodoUserDAO todoUserDAO;
   private final AccessControlContextFactory oaccFactory;

   public TodoUserService(TodoUserDAO todoUserDAO, AccessControlContextFactory accessControlContextFactory) {
      this.todoUserDAO = todoUserDAO;
      this.oaccFactory = accessControlContextFactory;
   }

   public TodoUser createUser(TodoUser todoUser) {
      assertTodoUserIsValid(todoUser);

      // normalize the user's email address to lowercase
      final TodoUser newTodoUser = new TodoUser(todoUser.getEmail().toLowerCase(),
                                                todoUser.getPassword());

      // let's add the oacc resource first, which will implicitly check if email already exists and if password is valid
      final AccessControlContext oacc = oaccFactory.build();
      final Resource userResource = createUserResource(newTodoUser, oacc);

      try {
         // assign role(s) to new user
         assignUserRoles(userResource);

         // now let's add the new todoUser as an entity in the app model
         todoUserDAO.insert(newTodoUser);
      }
      catch (Exception e) {
         // something went wrong, so let's try to undo the oacc resource creation
         oacc.deleteResource(userResource);
         throw e;
      }

      return new TodoUser(newTodoUser.getEmail());
   }

   private static Resource createUserResource(TodoUser todoUser, AccessControlContext oacc) {
      final Resource userResource;
      try {
         userResource = oacc.createResource(SecurityModel.RESOURCECLASS_USER,
                                            SecurityModel.DOMAIN_SECURE_TODO,
                                            todoUser.getEmail(),
                                            PasswordCredentials.newInstance(todoUser.getPassword()));
      }
      catch (IllegalArgumentException e) {
         if (e.getMessage() != null && e.getMessage().contains("External id is not unique")) {
            final String msg = String.format("A todo user with email %s already exists", todoUser.getEmail());
            throw new IllegalArgumentException(msg);
         }
         else {
            throw e;
         }
      }
      return userResource;
   }

   private void assignUserRoles(Resource userResource) {
      // assign role to new user so they can create todoItems
      final AccessControlContext roleHelperContext = oaccFactory.build();
      roleHelperContext.authenticate(SecurityModel.RESOURCE_ROLEHELPER_TODOCREATOR,
                                     SecurityModel.CREDENTIALS_ROLEHELPER_TODOCREATOR);
      roleHelperContext.grantResourcePermissions(userResource,
                                                 SecurityModel.RESOURCE_ROLE_TODOCREATOR,
                                                 SecurityModel.PERM_INHERIT);
   }

   private static void assertTodoUserIsValid(TodoUser todoUser) {
      Objects.requireNonNull(todoUser, "Todo user is required.");

      final String rawEmail = todoUser.getEmail();

      if (rawEmail == null) {
         throw new IllegalArgumentException("Email is required.");
      }
      if (rawEmail.trim().length() != rawEmail.length()) {
         throw new IllegalArgumentException("Email can not start or end with whitespace.");
      }
      if (!EMAIL_VALIDATOR.isValid(rawEmail, null)) {
         throw new IllegalArgumentException("Email must be a well-formed email address.");
      }

      final char[] rawPassword = todoUser.getPassword();

      if (rawPassword == null) {
         throw new IllegalArgumentException("Password is required.");
      }
      if (rawPassword.length < 1) {
         throw new IllegalArgumentException("Password length must be one or greater.");
      }
   }
}