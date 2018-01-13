/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.api

import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple
import com.thoughtworks.go.server.service.SecurityService
import org.springframework.security.GrantedAuthority
import org.springframework.security.context.SecurityContextHolder
import org.springframework.security.providers.TestingAuthenticationToken

import static org.mockito.Matchers.any
import static org.mockito.Matchers.eq
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

trait SecurityServiceTrait {

  SecurityService securityService = mock(SecurityService.class)

  void loginAsAdmin() {
    GoUserPrinciple principal = new GoUserPrinciple("username", "Display", "password", true, true, true, true, new GrantedAuthority[0], null)
    Username username = new Username(principal.username, principal.displayName)
    TestingAuthenticationToken authentication = new TestingAuthenticationToken(principal, null, null);
    SecurityContextHolder.getContext().setAuthentication(authentication)

    when(securityService.isUserAdmin(username)).thenReturn(true)
    when(securityService.isAuthorizedToViewAndEditTemplates(username)).thenReturn(true)
    when(securityService.isAuthorizedToEditTemplate(any(), eq(username))).thenReturn(true)
    when(securityService.isAuthorizedToViewTemplate(any(), eq(username))).thenReturn(true)
    when(securityService.isAuthorizedToViewTemplates(eq(username))).thenReturn(true)
  }

  void loginAsUser() {
    GoUserPrinciple principal = new GoUserPrinciple("username", "Display", "password", true, true, true, true, new GrantedAuthority[0], null)
    Username username = new Username(principal.username, principal.displayName)
    TestingAuthenticationToken authentication = new TestingAuthenticationToken(principal, null, null);
    SecurityContextHolder.getContext().setAuthentication(authentication)

    when(securityService.isUserAdmin(username)).thenReturn(false)
    when(securityService.isUserGroupAdmin(username)).thenReturn(false)
    when(securityService.isUserAdminOfGroup(eq(username.username) as CaseInsensitiveString, any() as String)).thenReturn(false)
    when(securityService.isUserAdminOfGroup(any() as Username, any() as String)).thenReturn(false)
    when(securityService.isAuthorizedToViewAndEditTemplates(username)).thenReturn(false)
    when(securityService.isAuthorizedToEditTemplate(any(), eq(username))).thenReturn(false)
    when(securityService.isAuthorizedToViewTemplate(any(), eq(username))).thenReturn(false)
    when(securityService.isAuthorizedToViewTemplates(eq(username))).thenReturn(false)
  }

  void loginAsAnonymous() {
    SecurityContextHolder.getContext().setAuthentication(null)
    when(securityService.isUserAdmin(Username.ANONYMOUS)).thenReturn(false)
    when(securityService.isUserGroupAdmin(any())).thenReturn(false)
    when(securityService.isAuthorizedToViewAndEditTemplates()).thenReturn(false)
    when(securityService.isAuthorizedToEditTemplate(any(), eq(Username.ANONYMOUS))).thenReturn(false)
    when(securityService.isAuthorizedToViewTemplate(any(), eq(Username.ANONYMOUS))).thenReturn(false)
    when(securityService.isAuthorizedToViewTemplates()).thenReturn(false)
  }

  void enableSecurity() {
    when(securityService.isSecurityEnabled()).thenReturn(true)
  }

  void loginAsGroupAdmin() {
    GoUserPrinciple principal = new GoUserPrinciple("username", "Display", "password", true, true, true, true, new GrantedAuthority[0], null)
    Username username = new Username(principal.username, principal.displayName)
    TestingAuthenticationToken authentication = new TestingAuthenticationToken(principal, null, null);
    SecurityContextHolder.getContext().setAuthentication(authentication)

    when(securityService.isUserAdmin(username)).thenReturn(false)
    when(securityService.isUserGroupAdmin(username)).thenReturn(true)
    when(securityService.isUserAdminOfGroup(eq(username.username) as CaseInsensitiveString, any() as String)).thenReturn(true)
    when(securityService.isUserAdminOfGroup(any() as Username, any() as String)).thenReturn(true)
  }

  void disableSecurity() {
    when(securityService.isSecurityEnabled()).thenReturn(false)
    when(securityService.isUserAdmin(any() as Username)).thenReturn(true)
  }
}
