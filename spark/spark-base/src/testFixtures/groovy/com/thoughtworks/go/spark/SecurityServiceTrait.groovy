/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils
import com.thoughtworks.go.server.security.GoAuthority
import com.thoughtworks.go.server.security.userdetail.GoUserPrincipal
import com.thoughtworks.go.server.service.GoConfigService
import com.thoughtworks.go.server.service.SecurityService
import com.thoughtworks.go.spark.util.Random
import com.thoughtworks.go.util.SystemEnvironment
import groovy.transform.NamedParam
import org.junit.jupiter.api.AfterEach

import static com.thoughtworks.go.config.CaseInsensitiveString.cis
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

trait SecurityServiceTrait {
  SecurityService securityService = mock(SecurityService.class)
  GoConfigService goConfigService = mock(GoConfigService.class)
  SystemEnvironment systemEnvironment = mock(SystemEnvironment.class)

  void enableSecurity() {
    when(securityService.isSecurityEnabled()).thenReturn(true)
  }

  void disableSecurity() {
    SessionUtils.setCurrentUser(new GoUserPrincipal("anonymous", "anonymous", GoAuthority.ALL_AUTHORITIES))
    when(securityService.isSecurityEnabled()).thenReturn(false)
    when(securityService.isUserAdmin(any() as Username)).thenReturn(true)
  }

  void loginAsAnonymous() {
    if (securityService.isSecurityEnabled()) {
      SessionUtils.setCurrentUser(new GoUserPrincipal("anonymous", "anonymous", GoAuthority.ROLE_ANONYMOUS.asAuthority()))
    } else {
      SessionUtils.setCurrentUser(new GoUserPrincipal("anonymous", "anonymous", GoAuthority.ALL_AUTHORITIES))
    }
    when(securityService.isUserAdmin(Username.ANONYMOUS)).thenReturn(false)
    when(securityService.isUserGroupAdmin(Username.ANONYMOUS)).thenReturn(false)
    when(securityService.isAuthorizedToEditTemplates(Username.ANONYMOUS)).thenReturn(false)
    when(securityService.isAuthorizedToEditTemplate(any() as CaseInsensitiveString, eq(Username.ANONYMOUS))).thenReturn(false)
    when(securityService.isAuthorizedToViewTemplate(any() as CaseInsensitiveString, eq(Username.ANONYMOUS))).thenReturn(false)
    when(securityService.isAuthorizedToViewTemplates(Username.ANONYMOUS)).thenReturn(false)
    when(goConfigService.groups()).thenReturn(new PipelineGroups())
    when(goConfigService.findGroupNameByPipelineOptional(any())).thenReturn(Optional.of(PipelineSpecifier.generateGroupName())) // allow other pipelines to be found; but in random groups
  }

  void loginAsAdmin() {
    enableSecurity()
    Username username = loginAsRandomUser()

    when(securityService.isUserAdmin(username)).thenReturn(true)
    when(securityService.canEditSomeAdminPage(username)).thenReturn(true)
    when(securityService.canViewSomeAdminPage(username)).thenReturn(true)
    when(securityService.isAuthorizedToEditTemplates(username)).thenReturn(true)
    when(securityService.isAuthorizedToEditTemplate(any() as CaseInsensitiveString, eq(username))).thenReturn(true)
    when(securityService.isAuthorizedToViewTemplate(any() as CaseInsensitiveString, eq(username))).thenReturn(true)
    when(securityService.isAuthorizedToViewTemplates(eq(username))).thenReturn(true)
  }

  void loginAsUser() {
    enableSecurity()
    Username username = loginAsRandomUser()

    when(securityService.isUserAdmin(username)).thenReturn(false)
    when(securityService.isUserGroupAdmin(username)).thenReturn(false)
    when(securityService.isUserAdminOfGroup(eq(username.username) as CaseInsensitiveString, any() as String)).thenReturn(false)
    when(securityService.isUserAdminOfGroup(any() as Username, any() as String)).thenReturn(false)
    when(securityService.isAuthorizedToEditTemplates(username)).thenReturn(false)
    when(securityService.isAuthorizedToEditTemplate(any() as CaseInsensitiveString, eq(username))).thenReturn(false)
    when(securityService.isAuthorizedToViewTemplate(any() as CaseInsensitiveString, eq(username))).thenReturn(false)
    when(securityService.isAuthorizedToViewTemplates(eq(username))).thenReturn(false)
    when(goConfigService.groups()).thenReturn(new PipelineGroups())
    when(goConfigService.findGroupNameByPipelineOptional(any())).thenReturn(Optional.of(PipelineSpecifier.generateGroupName())) // allow other pipelines to be found; but in random groups
  }

  void loginAsGroupAdmin(@NamedParam(value = 'groupName', type = String) @NamedParam(value = 'pipelineName', type = String) Map<String, String> opts) {
    loginAsGroupAdmin(new PipelineSpecifier(opts))
  }

  void loginAsGroupAdmin(PipelineSpecifier opts = PipelineSpecifier.random()) {
    enableSecurity()
    Username username = loginAsRandomUser()
    when(securityService.isUserAdmin(username)).thenReturn(false)
    when(securityService.canEditSomeAdminPage(username)).thenReturn(true)
    when(securityService.canViewSomeAdminPage(username)).thenReturn(true)
    when(securityService.isUserGroupAdmin(username)).thenReturn(true)
    when(securityService.isUserAdminOfGroup(eq(username.username) as CaseInsensitiveString, eq(opts.groupName) as String)).thenReturn(true)
    when(securityService.isUserAdminOfGroup(eq(username) as Username, eq(opts.groupName))).thenReturn(true)

    PipelineGroups groups = mock(PipelineGroups.class)
    when(goConfigService.findGroupNameByPipelineOptional(any())).thenReturn(Optional.of(PipelineSpecifier.generateGroupName())) // allow other pipelines to be found; but in random groups
    when(goConfigService.findGroupNameByPipelineOptional(cis(opts.pipelineName))).thenReturn(Optional.of(opts.groupName))
    when(goConfigService.groups()).thenReturn(groups)
    when(groups.hasGroup(opts.groupName)).thenReturn(true)
    when(securityService.hasOperatePermissionForGroup(username.username, opts.groupName)).thenReturn(true)
  }

  void loginAsGroupOperateUser(@NamedParam(value = 'groupName', type = String) @NamedParam(value = 'pipelineName', type = String) Map<String, String> opts) {
      loginAsGroupOperateUser(new PipelineSpecifier(opts))
  }

  void loginAsGroupOperateUser(PipelineSpecifier opts = PipelineSpecifier.random()) {
    enableSecurity()
    Username username = loginAsRandomUser()
    when(securityService.isUserAdmin(username)).thenReturn(false)
    when(securityService.isUserGroupAdmin(username)).thenReturn(false)
    when(securityService.isUserAdminOfGroup(eq(username.username) as CaseInsensitiveString, eq(opts.groupName) as String)).thenReturn(false)
    when(securityService.isUserAdminOfGroup(eq(username) as Username, eq(opts.groupName))).thenReturn(false)

    PipelineGroups groups = mock(PipelineGroups.class)
    when(goConfigService.findGroupNameByPipelineOptional(any())).thenReturn(Optional.of(PipelineSpecifier.generateGroupName())) // allow other pipelines to be found; but in random groups
    when(goConfigService.findGroupNameByPipelineOptional(cis(opts.pipelineName))).thenReturn(Optional.of(opts.groupName))
    when(goConfigService.groups()).thenReturn(groups)
    when(groups.hasGroup(opts.groupName)).thenReturn(true)
    when(securityService.hasOperatePermissionForGroup(eq(username.username), eq(opts.groupName))).thenReturn(true)
  }

  void loginAsTemplateAdmin() {
    enableSecurity()
    Username username = loginAsRandomUser()

    PipelineGroups groups = mock(PipelineGroups.class)
    when(goConfigService.groups()).thenReturn(groups)
    when(groups.hasGroup(anyString())).thenReturn(true)
    when(goConfigService.findGroupNameByPipelineOptional(any())).thenReturn(Optional.of(PipelineSpecifier.generateGroupName())) // allow other pipelines to be found; but in random groups

    when(securityService.isUserAdmin(username)).thenReturn(false)
    when(securityService.canEditSomeAdminPage(username)).thenReturn(true)
    when(securityService.canViewSomeAdminPage(username)).thenReturn(true)
    when(securityService.isUserGroupAdmin(username)).thenReturn(false)
    when(securityService.isUserAdminOfGroup(eq(username) as Username, any(String.class))).thenReturn(false)
    when(securityService.isAuthorizedToEditTemplates(username)).thenReturn(true)
    when(securityService.isAuthorizedToEditTemplate(any() as CaseInsensitiveString, eq(username))).thenReturn(true)
    when(securityService.isAuthorizedToViewTemplate(any() as CaseInsensitiveString, eq(username))).thenReturn(true)
    when(securityService.isAuthorizedToViewTemplates(username)).thenReturn(true)
  }

  void loginAsTemplateViewUser() {
    enableSecurity()
    Username username = loginAsRandomUser()

    when(securityService.isUserAdmin(username)).thenReturn(false)
    when(securityService.canEditSomeAdminPage(username)).thenReturn(false)
    when(securityService.canViewSomeAdminPage(username)).thenReturn(true)
    when(securityService.isUserGroupAdmin(username)).thenReturn(false)
    when(securityService.isUserAdminOfGroup(eq(username) as Username, any(String.class))).thenReturn(false)

    when(securityService.isAuthorizedToEditTemplates(username)).thenReturn(false)
    when(securityService.isAuthorizedToEditTemplate(any() as CaseInsensitiveString, eq(username))).thenReturn(false)
    when(securityService.isAuthorizedToViewTemplate(any() as CaseInsensitiveString, eq(username))).thenReturn(true)
    when(securityService.isAuthorizedToViewTemplates(username)).thenReturn(true)
  }

  void loginAsPipelineViewUser(@NamedParam(value = 'groupName', type = String) @NamedParam(value = 'pipelineName', type = String) Map<String, String> opts) {
    loginAsPipelineViewUser(new PipelineSpecifier(opts))
  }

  void loginAsPipelineViewUser(PipelineSpecifier opts = PipelineSpecifier.random()) {
    enableSecurity()
    Username username = loginAsRandomUser()

    when(securityService.isUserAdmin(username)).thenReturn(false)
    when(securityService.isUserGroupAdmin(username)).thenReturn(false)
    when(securityService.isUserAdminOfGroup(eq(username.username) as CaseInsensitiveString, any() as String)).thenReturn(false)
    when(securityService.isUserAdminOfGroup(any() as Username, any() as String)).thenReturn(false)

    when(securityService.isAuthorizedToEditTemplates(username)).thenReturn(false)
    when(securityService.isAuthorizedToEditTemplate(any() as CaseInsensitiveString, eq(username))).thenReturn(false)
    when(securityService.isAuthorizedToViewTemplate(any() as CaseInsensitiveString, eq(username))).thenReturn(false)
    when(securityService.isAuthorizedToViewTemplates(eq(username))).thenReturn(false)

    PipelineGroups groups = mock(PipelineGroups.class)
    when(goConfigService.findGroupNameByPipelineOptional(any())).thenReturn(Optional.of(PipelineSpecifier.generateGroupName())) // allow other pipelines to be found; but in random groups
    when(goConfigService.findGroupNameByPipelineOptional(cis(opts.pipelineName))).thenReturn(Optional.of(opts.groupName))
    when(goConfigService.groups()).thenReturn(groups)
    when(groups.hasGroup(opts.groupName)).thenReturn(true)
    when(securityService.hasOperatePermissionForGroup(eq(username.username), eq(opts.groupName))).thenReturn(false)
    when(securityService.hasViewPermissionForPipeline(eq(username), eq(opts.pipelineName))).thenReturn(true)
  }

  private Username loginAsRandomUser() {
    def hex = Random.hex(20)
    String loginName = "jdoe-${hex}"
    String displayName = "Jon Doe ${hex}"
    GoUserPrincipal principal = new GoUserPrincipal(loginName, displayName)
    SessionUtils.setCurrentUser(principal)
    principal.asUsernameObject()
  }

  @AfterEach
  void logout() {
    SessionUtils.unsetCurrentUser()
  }

  static record PipelineSpecifier(String groupName = generateGroupName(), String pipelineName = generatePipelineName()) {
    static PipelineSpecifier random() {
      new PipelineSpecifier(groupName: generateGroupName(), pipelineName: generatePipelineName())
    }

    private static String generateGroupName() {
      "group-" + Random.hex(20)
    }

    private static String generatePipelineName() {
      "pipeline-" + Random.hex(20)
    }
  }
}
