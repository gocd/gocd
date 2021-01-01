/*
 * Copyright 2021 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.versioninfos.representers

import com.thoughtworks.go.domain.GoVersion
import com.thoughtworks.go.domain.VersionInfo
import com.thoughtworks.go.util.SystemEnvironment
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class VersionInfoRepresenterTest {
  private SystemEnvironment systemEnvironment
  private GoVersion currentVersion = new GoVersion('1.2.3-1')
  private GoVersion latestVersion = new GoVersion('5.6.7-1')

  @BeforeEach
  void setUp() {
    systemEnvironment = mock(SystemEnvironment.class)

    when(systemEnvironment.getUpdateServerUrl()).thenReturn("https://update.example.com/some/path")
  }

  @Test
  void 'should return empty response when version info is null'() {
    def actualJson = toObjectString({ VersionInfoRepresenter.toJSON(it, null, systemEnvironment) })

    assertThatJson(actualJson).is([])
  }

  @Test
  void 'should render basic version infos'() {
    def info = new VersionInfo("go_server", currentVersion, latestVersion, null)

    def actualJson = toObjectString({ VersionInfoRepresenter.toJSON(it, info, systemEnvironment) })

    def expectedJson = [
      _links           : [
        doc : [
          href: apiDocsUrl("#version-info")
        ],
        self: [
          href: "http://test.host/go/api/version_infos/stale"
        ]
      ],
      component_name   : "go_server",
      update_server_url: "https://update.example.com/some/path?current_version=1.2.3-1",
      installed_version: "1.2.3-1",
      latest_version   : "5.6.7-1"
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should handle when latest version is null'() {
    def info = new VersionInfo("go_server", currentVersion, null, null)

    def actualJson = toObjectString({ VersionInfoRepresenter.toJSON(it, info, systemEnvironment) })

    def expectedJson = [
      _links           : [
        doc : [
          href: apiDocsUrl("#version-info")
        ],
        self: [
          href: "http://test.host/go/api/version_infos/stale"
        ]
      ],
      component_name   : "go_server",
      update_server_url: "https://update.example.com/some/path?current_version=1.2.3-1",
      installed_version: "1.2.3-1",
      latest_version   : null
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should include the query params present in update server url initially'() {
    when(systemEnvironment.getUpdateServerUrl()).thenReturn("https://update.example.com/some/path?foo=bar")
    def info = new VersionInfo("go_server", currentVersion, latestVersion, null)

    def actualJson = toObjectString({ VersionInfoRepresenter.toJSON(it, info, systemEnvironment) })

    def expectedJson = [
      _links           : [
        doc : [
          href: apiDocsUrl("#version-info")
        ],
        self: [
          href: "http://test.host/go/api/version_infos/stale"
        ]
      ],
      component_name   : "go_server",
      update_server_url: "https://update.example.com/some/path?foo=bar&current_version=1.2.3-1",
      installed_version: "1.2.3-1",
      latest_version   : "5.6.7-1"
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should handle if update server url is invalid'() {
    when(systemEnvironment.getUpdateServerUrl()).thenReturn("https:///update.example.com/some path")
    def info = new VersionInfo("go_server", currentVersion, latestVersion, null)

    def actualJson = toObjectString({ VersionInfoRepresenter.toJSON(it, info, systemEnvironment) })

    def expectedJson = [
      _links           : [
        doc : [
          href: apiDocsUrl("#version-info")
        ],
        self: [
          href: "http://test.host/go/api/version_infos/stale"
        ]
      ],
      component_name   : "go_server",
      update_server_url: null,
      installed_version: "1.2.3-1",
      latest_version   : "5.6.7-1"
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
