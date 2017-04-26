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
import com.acciente.oacc.Credentials;
import com.acciente.oacc.Resource;
import com.acciente.oacc.Resources;
import com.acciente.securetodo.AccessControlContextFactory;
import com.acciente.securetodo.api.TodoUser;
import com.acciente.securetodo.db.TodoUserDAO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TodoUserServiceTest {
   private static final String EMAIL             = "tester@oaccframework.org";
   private static final String PASSWORD          = "secret";
   private static final char[] PASSWORD_AS_CHARS = PASSWORD.toCharArray();
   private static final String BLANK             = " \t";

   private TodoUserDAO                 todoUserDAO;
   private AccessControlContextFactory accessControlContextFactory;
   private AccessControlContext        oacc;
   private TodoUserService             todoUserService;

   @Before
   public void setUp() throws Exception {
      todoUserDAO = mock(TodoUserDAO.class);
      accessControlContextFactory = mock(AccessControlContextFactory.class);
      oacc = mock(AccessControlContext.class);
      when(accessControlContextFactory.build()).thenReturn(oacc);

      todoUserService = new TodoUserService(todoUserDAO, accessControlContextFactory);
   }

   @After
   public void tearDown() throws Exception {
   }

   @Test(expected = NullPointerException.class)
   public void createUserWithNull() throws Exception {
      todoUserService.createUser(null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void createUserWithoutPassword() throws Exception {
      todoUserService.createUser(new TodoUser(EMAIL, null));
   }

   @Test(expected = IllegalArgumentException.class)
   public void createUserWithBlankPassword() throws Exception {
      when(oacc.createResource(anyString(), anyString(), anyString(), any(Credentials.class)))
            .thenThrow(new IllegalArgumentException());

      todoUserService.createUser(new TodoUser(EMAIL, BLANK.toCharArray()));
   }

   @Test(expected = IllegalArgumentException.class)
   public void createUserWithoutEmail() throws Exception {
      todoUserService.createUser(new TodoUser(null, PASSWORD_AS_CHARS));
   }

   @Test(expected = IllegalArgumentException.class)
   public void createUserWithBlankEmail() throws Exception {
      todoUserService.createUser(new TodoUser(BLANK, PASSWORD_AS_CHARS));
   }

   @Test(expected = IllegalArgumentException.class)
   public void createUserWithUntrimmedEmail() throws Exception {
      todoUserService.createUser(new TodoUser(" " + EMAIL + "\t", PASSWORD_AS_CHARS));
   }

   @Test(expected = IllegalArgumentException.class)
   public void createUserWithMalformedEmail() throws Exception {
      todoUserService.createUser(new TodoUser("invalid email", PASSWORD_AS_CHARS));
   }

   @Test
   public void createUser() throws Exception {
      final Resource createdResource = Resources.getInstance(1L, EMAIL);
      when(oacc.createResource(anyString(), anyString(), anyString(), any(Credentials.class)))
            .thenReturn(createdResource);
      final TodoUser submittedTodoUser = new TodoUser(EMAIL, PASSWORD_AS_CHARS);

      final TodoUser returnedTodoUser = todoUserService.createUser(submittedTodoUser);

      assertThat(returnedTodoUser).isEqualTo(submittedTodoUser);
      assertThat(returnedTodoUser.getPassword()).isNull();
//      verify(oacc).createResource(SecurityModel.RESOURCECLASS_USER,
//                                  SecurityModel.DOMAIN_SECURE_TODO,
//                                  submittedTodoUser.getEmail(),
//                                  PasswordCredentials.newInstance(submittedTodoUser.getPassword()));  // pwdCreds don't have equals(), so can't use eq() or value directly
      verify(oacc).createResource(eq(SecurityModel.RESOURCECLASS_USER),
                                  eq(SecurityModel.DOMAIN_SECURE_TODO),
                                  eq(submittedTodoUser.getEmail()),
                                  any(Credentials.class));
      verify(accessControlContextFactory, times(2)).build();
      verify(oacc).grantResourcePermissions(createdResource,
                                            SecurityModel.RESOURCE_ROLE_TODOCREATOR,
                                            SecurityModel.PERM_INHERIT);
      verify(todoUserDAO).insert(submittedTodoUser);
      verify(oacc, never()).deleteResource(any(Resource.class));
   }

   @Test
   public void createUserWithMixedCaseEmail() throws Exception {
      final String email_lowercase = EMAIL.toLowerCase();
      final String email_uppercase = EMAIL.toUpperCase();
      final Resource createdResource = Resources.getInstance(1L, email_lowercase);
      when(oacc.createResource(anyString(), anyString(), anyString(), any(Credentials.class)))
            .thenReturn(createdResource);

      final TodoUser upperTodoUser = new TodoUser(email_uppercase, PASSWORD_AS_CHARS);
      final TodoUser lowerTodoUser = new TodoUser(email_lowercase, PASSWORD_AS_CHARS);
      todoUserService.createUser(upperTodoUser);

//      verify(oacc).createResource(SecurityModel.RESOURCECLASS_USER,
//                                  SecurityModel.DOMAIN_SECURE_TODO,
//                                  email_lowercase,
//                                  PasswordCredentials.newInstance(upperTodoUser.getPassword()));  // pwdCreds don't have equals(), so can't use eq() or value directly
      verify(oacc).createResource(eq(SecurityModel.RESOURCECLASS_USER),
                                  eq(SecurityModel.DOMAIN_SECURE_TODO),
                                  eq(email_lowercase),
                                  any(Credentials.class));
      verify(todoUserDAO).insert(lowerTodoUser);
      verify(oacc, never()).deleteResource(any(Resource.class));
   }

   @Test(expected = IllegalArgumentException.class)
   public void createUserThatAlreadyExists() throws Exception {
      when(oacc.createResource(anyString(), anyString(), anyString(), any(Credentials.class)))
            .thenThrow(new IllegalArgumentException("External id is not unique"));

      todoUserService.createUser(new TodoUser(EMAIL, PASSWORD_AS_CHARS));
   }
}