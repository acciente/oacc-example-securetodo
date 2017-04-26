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

import com.acciente.oacc.Credentials;
import com.acciente.oacc.PasswordCredentials;
import com.acciente.oacc.Resource;
import com.acciente.oacc.ResourcePermission;
import com.acciente.oacc.ResourcePermissions;
import com.acciente.oacc.Resources;

public class SecurityModel {
   // domain names
   public static final String DOMAIN_SECURE_TODO = "secure-todo";

   // resource class names
   public static final String RESOURCECLASS_USER = "user";
   public static final String RESOURCECLASS_TODO = "todo";

   // permissions
   public static final ResourcePermission PERM_INHERIT              = ResourcePermissions.getInstance(ResourcePermissions.INHERIT);
   public static final ResourcePermission PERM_VIEW                 = ResourcePermissions.getInstance("VIEW");
   public static final ResourcePermission PERM_GRANT_VIEW           = ResourcePermissions.getInstanceWithGrantOption("VIEW");
   public static final ResourcePermission PERM_EDIT                 = ResourcePermissions.getInstance("EDIT");
   public static final ResourcePermission PERM_MARK_COMPLETED       = ResourcePermissions.getInstance("MARK-COMPLETED");
   public static final ResourcePermission PERM_GRANT_MARK_COMPLETED = ResourcePermissions.getInstanceWithGrantOption("MARK-COMPLETED");

   // resources & credentials
   public static final Credentials CREDENTIALS_ROLEHELPER_TODOCREATOR = PasswordCredentials.newInstance("todoCreatorHelperPassword".toCharArray());
   public static final Resource    RESOURCE_ROLEHELPER_TODOCREATOR    = Resources.getInstance("todo-creator-helper");
   public static final Resource    RESOURCE_ROLE_TODOCREATOR          = Resources.getInstance("todo-creator");

   private SecurityModel() {
   }
}
