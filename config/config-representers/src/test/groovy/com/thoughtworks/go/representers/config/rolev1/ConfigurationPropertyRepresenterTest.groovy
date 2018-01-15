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

package com.thoughtworks.go.representers.config.rolev1

import cd.go.jrepresenter.TestRequestContext
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import com.thoughtworks.go.security.GoCipher
import gen.com.thoughtworks.go.representers.config.rolev1.ConfigurationPropertyMapper
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat

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
  }

  @Nested
  class Deserialize {
    @Test
    void 'it should deserialize to encrypted property if encrypted value is submitted'() {
      def encryptedValue = new GoCipher().encrypt('password')
      def property = ConfigurationPropertyMapper.fromJSON([key: 'user', encrypted_value: encryptedValue])
      assertThat(property).isEqualTo(ConfigurationPropertyMother.create("user", true, 'password'))
    }

    @Test
    void 'it should deserialize to simple property if plain-text value is submitted'() {
      def property = ConfigurationPropertyMapper.fromJSON([key: 'user', value: 'bob'])
      assertThat(property).isEqualTo(ConfigurationPropertyMother.create("user", false, "bob"))
    }

    @Test
    void 'it should deserialize with errors'() {
      def encryptedValue = new GoCipher().encrypt('p@ssword')

      def property = ConfigurationPropertyMapper.fromJSON([key: 'password', value: 'p@ssword', encrypted_value: encryptedValue])

      assertThat(property.hasErrors()).isTrue()
      assertThat(property.errors()).hasSize(2)
      assertThat(property.errors().getAllOn("value")).isEqualTo(["You may only specify `value` or `encrypted_value`, not both!"])
      assertThat(property.errors().getAllOn("encryptedValue")).isEqualTo(["You may only specify `value` or `encrypted_value`, not both!"])
    }
  }
}
