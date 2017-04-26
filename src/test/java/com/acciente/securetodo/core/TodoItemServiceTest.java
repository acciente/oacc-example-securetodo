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
import com.acciente.oacc.NotAuthorizedException;
import com.acciente.oacc.Resource;
import com.acciente.oacc.ResourcePermission;
import com.acciente.oacc.Resources;
import com.acciente.securetodo.api.TodoItem;
import com.acciente.securetodo.db.TodoItemDAO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class TodoItemServiceTest {
   private static final long   ITEM_ID1 = 1L;
   private static final long   ITEM_ID2 = 2L;
   private static final String TITLE1   = "Write test cases";
   private static final String TITLE2   = "Refactor code";
   private static final String BLANK    = " \t";
   private static final String EMAIL    = "tester@oaccframework.org";

   private TodoItemDAO          todoItemDAO;
   private AccessControlContext oacc;
   private TodoItemService      todoItemService;

   @Before
   public void setUp() throws Exception {
      todoItemDAO = mock(TodoItemDAO.class);
      oacc = mock(AccessControlContext.class);

      todoItemService = new TodoItemService(todoItemDAO);
   }

   @After
   public void tearDown() throws Exception {
   }

   @Test
   public void createItem() throws Exception {
      final TodoItem todoItem = new TodoItem(ITEM_ID1 - 100, TITLE1, false);
      final TodoItem expectedTodoItem = new TodoItem(ITEM_ID1, todoItem.getTitle(), todoItem.getCompleted());
      when(todoItemDAO.insert(any(TodoItem.class))).thenReturn(ITEM_ID1);
      when(todoItemDAO.findById(ITEM_ID1)).thenReturn(expectedTodoItem);

      final TodoItem returnedTodoItem = todoItemService.createItem(oacc, todoItem);

      assertThat(returnedTodoItem).isEqualTo(expectedTodoItem);
      verify(todoItemDAO).insert(todoItem);
      verify(oacc).createResource(SecurityModel.RESOURCECLASS_TODO,
                                  SecurityModel.DOMAIN_SECURE_TODO,
                                  String.valueOf(ITEM_ID1));
   }

   @Test(expected = NullPointerException.class)
   public void createItemWithNull() throws Exception {
      todoItemService.createItem(oacc, null);
   }

   @Test
   public void createItemWithoutCompleted() throws Exception {
      final TodoItem todoItem = new TodoItem(ITEM_ID1 - 100, TITLE1, null);
      final TodoItem expectedTodoItem = new TodoItem(ITEM_ID1, todoItem.getTitle(), false);
      when(todoItemDAO.insert(any(TodoItem.class))).thenReturn(ITEM_ID1);
      when(todoItemDAO.findById(ITEM_ID1)).thenReturn(expectedTodoItem);

      final TodoItem returnedTodoItem = todoItemService.createItem(oacc, todoItem);

      assertThat(returnedTodoItem.getCompleted()).isFalse();
   }

   @Test
   public void createItemWithCompletedAsTrue() throws Exception {
      final TodoItem todoItem = new TodoItem(ITEM_ID1 - 100, TITLE1, true);
      final TodoItem expectedTodoItem = new TodoItem(ITEM_ID1, todoItem.getTitle(), todoItem.getCompleted());
      when(todoItemDAO.insert(any(TodoItem.class))).thenReturn(ITEM_ID1);
      when(todoItemDAO.findById(ITEM_ID1)).thenReturn(expectedTodoItem);

      final TodoItem returnedTodoItem = todoItemService.createItem(oacc, todoItem);

      assertThat(returnedTodoItem.getCompleted()).isTrue();
   }

   @Test(expected = IllegalArgumentException.class)
   public void createItemWithoutTitle() throws Exception {
      final TodoItem todoItem = new TodoItem(ITEM_ID1 - 100, null, false);

      todoItemService.createItem(oacc, todoItem);
   }

   @Test(expected = IllegalArgumentException.class)
   public void createItemWithBlankTitle() throws Exception {
      final TodoItem todoItem = new TodoItem(ITEM_ID1 - 100, BLANK, false);

      todoItemService.createItem(oacc, todoItem);
   }

   @Test(expected = NotAuthorizedException.class)
   public void createItemWhenUnauthorized() throws Exception {
      final TodoItem todoItem = new TodoItem(ITEM_ID1 - 100, TITLE1, false);
      final TodoItem expectedTodoItem = new TodoItem(ITEM_ID1, todoItem.getTitle(), todoItem.getCompleted());
      when(todoItemDAO.insert(any(TodoItem.class))).thenReturn(ITEM_ID1);
      when(todoItemDAO.findById(ITEM_ID1)).thenReturn(expectedTodoItem);
      doThrow(new NotAuthorizedException(""))
            .when(oacc).createResource(SecurityModel.RESOURCECLASS_TODO,
                                       SecurityModel.DOMAIN_SECURE_TODO,
                                       String.valueOf(ITEM_ID1));

      try {
         todoItemService.createItem(oacc, todoItem);
      }
      catch (Exception e) {
         // verify that DAO's delete() is called after insert()
         verify(todoItemDAO).delete(ITEM_ID1);

         // rethrow the caught exception
         throw e;
      }
   }

   @Test
   public void findByAuthenticatedUser() throws Exception {
      final Resource authenticatedResource = Resources.getInstance(22L);
      final Set<Resource> itemResources = Stream.of(Resources.getInstance(33L, String.valueOf(ITEM_ID1)),
                                                    Resources.getInstance(44L, String.valueOf(ITEM_ID2))).collect(Collectors.toSet());
      final List<Long> foundItemIds = Stream.of(ITEM_ID1, ITEM_ID2).collect(Collectors.toList());
      final TodoItem expectedTodoItem1 = new TodoItem(ITEM_ID1, TITLE1, true);
      final TodoItem expectedTodoItem2 = new TodoItem(ITEM_ID2, TITLE2, false);
      List<TodoItem> expectedTodoItems = Stream.of(expectedTodoItem1, expectedTodoItem2).collect(Collectors.toList());
      when(oacc.getSessionResource()).thenReturn(authenticatedResource);
      when(oacc.getResourcesByResourcePermissions(authenticatedResource, SecurityModel.RESOURCECLASS_TODO, SecurityModel.PERM_VIEW))
            .thenReturn(itemResources);
      when(todoItemDAO.findByIds(foundItemIds)).thenReturn(expectedTodoItems);

      final List<TodoItem> foundTodoItems = todoItemService.findByAuthenticatedUser(oacc);

      assertThat(foundTodoItems).containsExactlyInAnyOrder(expectedTodoItem1, expectedTodoItem2);
      verify(oacc).getSessionResource();
      verify(oacc).getResourcesByResourcePermissions(authenticatedResource, SecurityModel.RESOURCECLASS_TODO, SecurityModel.PERM_VIEW);
      verify(todoItemDAO).findByIds(foundItemIds);
      verify(todoItemDAO, never()).findById(anyLong());
   }

   @Test
   public void findByAuthenticatedUserWithoutTodoItems() throws Exception {
      final Resource authenticatedResource = Resources.getInstance(22L);
      final Set<Resource> noItemResources = Collections.emptySet();
      when(oacc.getSessionResource()).thenReturn(authenticatedResource);
      when(oacc.getResourcesByResourcePermissions(authenticatedResource, SecurityModel.RESOURCECLASS_TODO, SecurityModel.PERM_VIEW))
            .thenReturn(noItemResources);

      final List<TodoItem> foundTodoItems = todoItemService.findByAuthenticatedUser(oacc);

      assertThat(foundTodoItems).isEmpty();
      verify(oacc).getSessionResource();
      verify(oacc).getResourcesByResourcePermissions(authenticatedResource, SecurityModel.RESOURCECLASS_TODO, SecurityModel.PERM_VIEW);
      verify(todoItemDAO, never()).findByIds(any());
      verify(todoItemDAO, never()).findById(anyLong());
   }

   @Test
   public void updateItem() throws Exception {
      final long pathParam_itemId = ITEM_ID1;
      TodoItem initial = new TodoItem(pathParam_itemId, TITLE1, false);
      TodoItem patch = new TodoItem(pathParam_itemId, TITLE2, true);
      TodoItem expectedTodoItem = new TodoItem(initial.getId(), patch.getTitle(), patch.getCompleted());
      final Resource authenticatedResource = Resources.getInstance(22L);
      when(oacc.getSessionResource()).thenReturn(authenticatedResource);
      when(todoItemDAO.findById(pathParam_itemId)).thenReturn(initial);

      final TodoItem updatedTodoItem = todoItemService.updateItem(oacc,
                                                                  pathParam_itemId,
                                                                  patch);

      assertThat(updatedTodoItem).isEqualTo(expectedTodoItem);
      ArgumentCaptor<ResourcePermission> permissionCaptor = ArgumentCaptor.forClass(ResourcePermission.class);
      ArgumentCaptor<ResourcePermission> permissionVarargsCaptor = ArgumentCaptor.forClass(ResourcePermission.class);
      verify(oacc).assertResourcePermissions(eq(authenticatedResource),
                                             eq(Resources.getInstance(String.valueOf(pathParam_itemId))),
                                             permissionCaptor.capture(),
                                             permissionVarargsCaptor.capture());
      final List<ResourcePermission> permissions = permissionVarargsCaptor.getAllValues();
      permissions.add(permissionCaptor.getValue());
      assertThat(permissions).containsExactlyInAnyOrder(SecurityModel.PERM_EDIT, SecurityModel.PERM_VIEW);
      verify(todoItemDAO).update(expectedTodoItem);
   }

   @Test(expected = IllegalArgumentException.class)
   public void updateItemDoesNotExist() throws Exception {
      final long pathParam_itemId = ITEM_ID1;
      TodoItem patch = new TodoItem(pathParam_itemId, TITLE2, true);
      final Resource authenticatedResource = Resources.getInstance(22L);
      when(oacc.getSessionResource()).thenReturn(authenticatedResource);
      doThrow(new IllegalArgumentException(""))
            .when(oacc).assertResourcePermissions(any(Resource.class),
                                                  any(Resource.class),
                                                  any(ResourcePermission.class),
                                                  any());

      todoItemService.updateItem(oacc,
                                 pathParam_itemId,
                                 patch);
   }

   @Test(expected = NotAuthorizedException.class)
   public void updateItemWhenUnauthorized() throws Exception {
      final long pathParam_itemId = ITEM_ID1;
      TodoItem patch = new TodoItem(pathParam_itemId, TITLE2, true);
      final Resource authenticatedResource = Resources.getInstance(22L);
      when(oacc.getSessionResource()).thenReturn(authenticatedResource);
      doThrow(new NotAuthorizedException(""))
            .when(oacc).assertResourcePermissions(any(Resource.class),
                                                  any(Resource.class),
                                                  any(ResourcePermission.class),
                                                  any());

      todoItemService.updateItem(oacc,
                                 pathParam_itemId,
                                 patch);
   }

   @Test
   public void updateItemWithTitleOnly() throws Exception {
      final long pathParam_itemId = ITEM_ID1;
      TodoItem initial = new TodoItem(pathParam_itemId, TITLE1, false);
      TodoItem patch = new TodoItem(pathParam_itemId, TITLE2, null);
      TodoItem expectedTodoItem = new TodoItem(initial.getId(), patch.getTitle(), initial.getCompleted());
      final Resource authenticatedResource = Resources.getInstance(22L);
      when(oacc.getSessionResource()).thenReturn(authenticatedResource);
      when(todoItemDAO.findById(pathParam_itemId)).thenReturn(initial);

      final TodoItem updatedTodoItem = todoItemService.updateItem(oacc,
                                                                  pathParam_itemId,
                                                                  patch);

      assertThat(updatedTodoItem).isEqualTo(expectedTodoItem);
      ArgumentCaptor<ResourcePermission> permissionCaptor = ArgumentCaptor.forClass(ResourcePermission.class);
      ArgumentCaptor<ResourcePermission> permissionVarargsCaptor = ArgumentCaptor.forClass(ResourcePermission.class);
      verify(oacc).assertResourcePermissions(eq(authenticatedResource),
                                             eq(Resources.getInstance(String.valueOf(pathParam_itemId))),
                                             permissionCaptor.capture(),
                                             permissionVarargsCaptor.capture());
      final List<ResourcePermission> permissions = permissionVarargsCaptor.getAllValues();
      permissions.add(permissionCaptor.getValue());
      assertThat(permissions).containsExactlyInAnyOrder(SecurityModel.PERM_EDIT, SecurityModel.PERM_VIEW);
      verify(todoItemDAO).update(expectedTodoItem);
   }

   @Test
   public void updateItemWithCompletedOnly() throws Exception {
      final long pathParam_itemId = ITEM_ID1;
      TodoItem initial = new TodoItem(pathParam_itemId, TITLE1, false);
      TodoItem patch = new TodoItem(pathParam_itemId, null, true);
      TodoItem expectedTodoItem = new TodoItem(initial.getId(), initial.getTitle(), patch.getCompleted());
      final Resource authenticatedResource = Resources.getInstance(22L);
      when(oacc.getSessionResource()).thenReturn(authenticatedResource);
      when(todoItemDAO.findById(pathParam_itemId)).thenReturn(initial);

      final TodoItem updatedTodoItem = todoItemService.updateItem(oacc,
                                                                  pathParam_itemId,
                                                                  patch);

      assertThat(updatedTodoItem).isEqualTo(expectedTodoItem);
      ArgumentCaptor<ResourcePermission> permissionCaptor = ArgumentCaptor.forClass(ResourcePermission.class);
      ArgumentCaptor<ResourcePermission> permissionVarargsCaptor = ArgumentCaptor.forClass(ResourcePermission.class);
      verify(oacc).assertResourcePermissions(eq(authenticatedResource),
                                             eq(Resources.getInstance(String.valueOf(pathParam_itemId))),
                                             permissionCaptor.capture(),
                                             permissionVarargsCaptor.capture());
      final List<ResourcePermission> permissions = permissionVarargsCaptor.getAllValues();
      permissions.add(permissionCaptor.getValue());
      assertThat(permissions).containsExactlyInAnyOrder(SecurityModel.PERM_MARK_COMPLETED, SecurityModel.PERM_VIEW);
      verify(todoItemDAO).update(expectedTodoItem);
   }

   @Test(expected = NullPointerException.class)
   public void updateItemWithNullPatch() throws Exception {
      final long pathParam_itemId = ITEM_ID1;
      todoItemService.updateItem(oacc,
                                 pathParam_itemId,
                                 null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void updateItemWithoutTitleOrCompleted() throws Exception {
      final long pathParam_itemId = ITEM_ID1;
      TodoItem patch = new TodoItem(pathParam_itemId, null, null);

      todoItemService.updateItem(oacc,
                                 pathParam_itemId,
                                 patch);
   }

   @Test
   public void shareItem() throws Exception {
      final long pathParam_itemId = ITEM_ID1;
      final Resource authenticatedResource = Resources.getInstance(22L);
      when(oacc.getSessionResource()).thenReturn(authenticatedResource);

      todoItemService.shareItem(oacc,
                                pathParam_itemId,
                                EMAIL);

      ArgumentCaptor<ResourcePermission> permissionCaptor = ArgumentCaptor.forClass(ResourcePermission.class);
      ArgumentCaptor<ResourcePermission> permissionVarargsCaptor = ArgumentCaptor.forClass(ResourcePermission.class);
      verify(oacc).grantResourcePermissions(eq(Resources.getInstance(EMAIL)),
                                            eq(Resources.getInstance(String.valueOf(pathParam_itemId))),
                                            permissionCaptor.capture(),
                                            permissionVarargsCaptor.capture());
      final List<ResourcePermission> permissions = permissionVarargsCaptor.getAllValues();
      permissions.add(permissionCaptor.getValue());
      assertThat(permissions).containsExactlyInAnyOrder(SecurityModel.PERM_MARK_COMPLETED, SecurityModel.PERM_VIEW);
      verify(oacc, never()).assertResourcePermissions(any(Resource.class),
                                                      any(Resource.class),
                                                      any());
      verifyZeroInteractions(todoItemDAO);
   }

   @Test(expected = NotAuthorizedException.class)
   public void shareItemWhenUnauthorized() throws Exception {
      final long pathParam_itemId = ITEM_ID1;
      final Resource authenticatedResource = Resources.getInstance(22L);
      when(oacc.getSessionResource()).thenReturn(authenticatedResource);
      doThrow(new NotAuthorizedException(""))
            .when(oacc).grantResourcePermissions(any(Resource.class),
                                                 any(Resource.class),
                                                 any(ResourcePermission.class),
                                                 any());

      todoItemService.shareItem(oacc,
                                pathParam_itemId,
                                EMAIL);
   }

   @Test(expected = IllegalArgumentException.class)
   public void shareItemDoesNotExist() throws Exception {
      final Resource authenticatedResource = Resources.getInstance(22L);
      when(oacc.getSessionResource()).thenReturn(authenticatedResource);
      doThrow(new IllegalArgumentException(""))
            .when(oacc).grantResourcePermissions(any(Resource.class),
                                                 any(Resource.class),
                                                 any(ResourcePermission.class),
                                                 any());

      todoItemService.shareItem(oacc,
                                ITEM_ID1 - 100,
                                EMAIL);
   }

   @Test(expected = IllegalArgumentException.class)
   public void shareItemWithInvalidUser() throws Exception {
      final long pathParam_itemId = ITEM_ID1;
      final Resource authenticatedResource = Resources.getInstance(22L);
      when(oacc.getSessionResource()).thenReturn(authenticatedResource);
      doThrow(new IllegalArgumentException(""))
            .when(oacc).grantResourcePermissions(any(Resource.class),
                                                 any(Resource.class),
                                                 any(ResourcePermission.class),
                                                 any());

      todoItemService.shareItem(oacc,
                                pathParam_itemId,
                                "nobodys@ema.il");
  }
}