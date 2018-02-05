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

package com.thoughtworks.go.spark

import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.domain.PipelineGroups
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple
import com.thoughtworks.go.server.service.GoConfigService
import com.thoughtworks.go.server.service.SecurityService
import com.thoughtworks.go.spark.util.SecureRandom
import org.junit.jupiter.api.AfterEach
import org.springframework.security.GrantedAuthority
import org.springframework.security.context.SecurityContextHolder
import org.springframework.security.providers.TestingAuthenticationToken

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

trait SecurityServiceTrait {
  SecurityService securityService = mock(SecurityService.class)
  GoConfigService goConfigService = mock(GoConfigService.class)

  void loginAsAdmin() {
    Username username = loginAsRandomUser()

    when(securityService.isUserAdmin(username)).thenReturn(true)
    when(securityService.isAuthorizedToViewAndEditTemplates(username)).thenReturn(true)
    when(securityService.isAuthorizedToEditTemplate(any() as CaseInsensitiveString, eq(username))).thenReturn(true)
    when(securityService.isAuthorizedToViewTemplate(any() as CaseInsensitiveString, eq(username))).thenReturn(true)
    when(securityService.isAuthorizedToViewTemplates(eq(username))).thenReturn(true)
  }

  void loginAsUser() {
    Username username = loginAsRandomUser()

    when(securityService.isUserAdmin(username)).thenReturn(false)
    when(securityService.isUserGroupAdmin(username)).thenReturn(false)
    when(securityService.isUserAdminOfGroup(eq(username.username) as CaseInsensitiveString, any() as String)).thenReturn(false)
    when(securityService.isUserAdminOfGroup(any() as Username, any() as String)).thenReturn(false)
    when(securityService.isAuthorizedToViewAndEditTemplates(username)).thenReturn(false)
    when(securityService.isAuthorizedToEditTemplate(any() as CaseInsensitiveString, eq(username))).thenReturn(false)
    when(securityService.isAuthorizedToViewTemplate(any() as CaseInsensitiveString, eq(username))).thenReturn(false)
    when(securityService.isAuthorizedToViewTemplates(eq(username))).thenReturn(false)
    when(goConfigService.groups()).thenReturn(new PipelineGroups())
  }

  void loginAsAnonymous() {
    SecurityContextHolder.getContext().setAuthentication(null)
    when(securityService.isUserAdmin(Username.ANONYMOUS)).thenReturn(false)
    when(securityService.isUserGroupAdmin(Username.ANONYMOUS)).thenReturn(false)
    when(securityService.isAuthorizedToViewAndEditTemplates(Username.ANONYMOUS)).thenReturn(false)
    when(securityService.isAuthorizedToEditTemplate(any() as CaseInsensitiveString, eq(Username.ANONYMOUS))).thenReturn(false)
    when(securityService.isAuthorizedToViewTemplate(any() as CaseInsensitiveString, eq(Username.ANONYMOUS))).thenReturn(false)
    when(securityService.isAuthorizedToViewTemplates(Username.ANONYMOUS)).thenReturn(false)
    when(goConfigService.groups()).thenReturn(new PipelineGroups())
  }

  void enableSecurity() {
    when(securityService.isSecurityEnabled()).thenReturn(true)
  }

  void loginAsGroupAdmin() {
    Username username = loginAsRandomUser()

    when(securityService.isUserAdmin(username)).thenReturn(false)
    when(securityService.isUserGroupAdmin(username)).thenReturn(true)
    when(securityService.isUserAdminOfGroup(eq(username.username) as CaseInsensitiveString, any() as String)).thenReturn(true)

    PipelineGroups groups = mock(PipelineGroups.class)
    when(goConfigService.groups()).thenReturn(groups)
    when(groups.hasGroup(any() as String)).thenReturn(true)
    when(securityService.hasOperatePermissionForGroup(any() as CaseInsensitiveString, any() as String)).thenReturn(true)
  }

  void loginAsGroupOperateUser() {
    Username username = loginAsRandomUser()

    when(securityService.isUserAdmin(username)).thenReturn(false)
    when(securityService.isUserGroupAdmin(username)).thenReturn(false)
    when(securityService.isUserAdminOfGroup(eq(username.username) as CaseInsensitiveString, any() as String)).thenReturn(false)

    PipelineGroups groups = mock(PipelineGroups.class)
    when(goConfigService.groups()).thenReturn(groups)
    when(groups.hasGroup(any() as String)).thenReturn(true)
    when(securityService.hasOperatePermissionForGroup(any() as CaseInsensitiveString, any() as String)).thenReturn(true)
  }

  void disableSecurity() {
    SecurityContextHolder.getContext().setAuthentication(null)
    when(securityService.isSecurityEnabled()).thenReturn(false)
    when(securityService.isUserAdmin(any() as Username)).thenReturn(true)
  }

  void loginAsTemplateAdmin() {
    Username username = loginAsRandomUser()

    when(securityService.isUserAdmin(username)).thenReturn(false)
    when(securityService.isUserGroupAdmin(username)).thenReturn(false)
    when(securityService.isAuthorizedToViewAndEditTemplates(username)).thenReturn(true)
    when(securityService.isAuthorizedToEditTemplate(any() as CaseInsensitiveString, eq(username))).thenReturn(true)
    when(securityService.isAuthorizedToViewTemplate(any() as CaseInsensitiveString, eq(username))).thenReturn(true)
    when(securityService.isAuthorizedToViewTemplates(username)).thenReturn(true)
  }

  private Username loginAsRandomUser() {
    def hex = SecureRandom.hex(20)
    String loginName = "jdoe-${hex}"
    String displayName = "Jon Doe ${hex}"
    GoUserPrinciple principal = new GoUserPrinciple(loginName, displayName, "password", true, true, true, true, new GrantedAuthority[0], null)
    Username username = new Username(principal.username, principal.displayName)
    TestingAuthenticationToken authentication = new TestingAuthenticationToken(principal, null, null)
    SecurityContextHolder.getContext().setAuthentication(authentication)
    username
  }

  @AfterEach
  void logout() {
    SecurityContextHolder.getContext().setAuthentication(null)
  }
}
