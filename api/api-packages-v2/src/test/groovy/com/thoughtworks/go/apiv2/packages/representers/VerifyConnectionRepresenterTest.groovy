/*
 * Copyright 2020 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv2.packages.representers

import com.thoughtworks.go.api.base.JsonUtils
import com.thoughtworks.go.domain.config.Configuration
import com.thoughtworks.go.domain.config.ConfigurationKey
import com.thoughtworks.go.domain.config.ConfigurationProperty
import com.thoughtworks.go.domain.config.ConfigurationValue
import com.thoughtworks.go.domain.packagerepository.PackageDefinition
import com.thoughtworks.go.domain.packagerepository.PackageRepository
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class VerifyConnectionRepresenterTest {
  static packageDefinition

  static {
    def configurationProperties = new Configuration(new ConfigurationProperty(new ConfigurationKey('PACKAGE_NAME'), new ConfigurationValue('foo')))
    packageDefinition = new PackageDefinition('package-id-1', 'package-1', configurationProperties)
    packageDefinition.setRepository(new PackageRepository('package-repo-id-1', 'package-repo-name-1', null, null))
  }

  def expectedJson = [
    message: "Boom!, can not verify connection",
    package: JsonUtils.toObject({ PackageDefinitionRepresenter.toJSON(it, packageDefinition) })
  ]

  @Test
  void 'should serialize verify connection response'() {
    def result = new HttpLocalizedOperationResult()
    result.badRequest("Boom!, can not verify connection")
    def actualJson = toObjectString({ VerifyConnectionRepresenter.toJSON(it, result, packageDefinition) })

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
