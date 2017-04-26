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

package com.acciente.securetodo.auth;

import com.acciente.oacc.AccessControlContext;
import com.acciente.oacc.PasswordCredentials;
import com.acciente.oacc.Resources;
import com.acciente.securetodo.AccessControlContextFactory;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;

import java.util.Optional;

public class OaccBasicAuthenticator implements Authenticator<BasicCredentials, OaccPrincipal> {
   private final AccessControlContextFactory oaccFactory;

   public OaccBasicAuthenticator(AccessControlContextFactory accessControlContextFactory) {
      this.oaccFactory = accessControlContextFactory;
   }

   @Override
   public Optional<OaccPrincipal> authenticate(BasicCredentials basicCredentials) throws AuthenticationException {
      OaccPrincipal oaccPrincipal = null;

      if (basicCredentials != null) {
         // the email from the basic auth username is the external id of the oacc user resource
         final String normalizedEmail = basicCredentials.getUsername().trim().toLowerCase();

         final AccessControlContext oacc = oaccFactory.build();
         try {
            // authenticate the oacc context and store it in a custom Principal
            oacc.authenticate(Resources.getInstance(normalizedEmail),
                              PasswordCredentials.newInstance(basicCredentials.getPassword().toCharArray()));
            oaccPrincipal = new OaccPrincipalImpl(oacc);
         }
         catch (IllegalArgumentException e) {
            // swallow exception to not divulge any information
         }
         catch (com.acciente.oacc.AuthenticationException e) {
            // swallow Auth exception to not divulge any information
         }
      }

      return Optional.ofNullable(oaccPrincipal);
   }
}
