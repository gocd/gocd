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

package com.thoughtworks.go.representers.config.artifactstorev1

import cd.go.jrepresenter.TestRequestContext
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import gen.com.thoughtworks.go.apiv1.admin.representers.ConfigurationPropertyMapper
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class ConfigurationPropertyRepresenterTest {
  @Nested
  class Serialize {
    @Test
    void 'it should serialize an unencrypted value if property type is unsecure'() {
      def configProperty = ConfigurationPropertyMother.create("user", false, "bob")
      def map = ConfigurationPropertyMapper.toJSON(configProperty, new TestRequestContext())
      assertThatJson(map).isEqualTo([key: 'user', value: 'bob'])
    }

    @Test
    void 'it should serialize an encrypted value if property type is secure'() {
      def configProperty = ConfigurationPropertyMother.create("password", true, "p@ssw0rd")
      def map = ConfigurationPropertyMapper.toJSON(configProperty, new TestRequestContext())
      assertThatJson(map).isEqualTo([key: 'password', encrypted_value: configProperty.getEncryptedValue()])
    }

    @Test
    void 'it should serialize property with null value'() {
      def configProperty = ConfigurationPropertyMother.createKeyOnly("Username")
      def map = ConfigurationPropertyMapper.toJSON(configProperty, new TestRequestContext())
      assertThatJson(map).isEqualTo([key: 'Username'])
    }

    @Test
    void 'it should serialize with errors'() {
      def configProperty = ConfigurationPropertyMother.create("user", false, "bob")
      configProperty.addError("encryptedValue", "blah")
      configProperty.addError("encryptedValue", "boo")

      def map = ConfigurationPropertyMapper.toJSON(configProperty, new TestRequestContext())
      assertThatJson(map).isEqualTo([key: 'user', value: 'bob', "errors": ["encrypted_value": ["blah", "boo"]]])
    }
  }

}