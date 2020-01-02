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
package com.thoughtworks.go.apiv1.mailserver.representers

import com.thoughtworks.go.CurrentGoCDVersion
import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.MailHost
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat

class MailServerRepresenterTest {

  @Test
  void "should represent mail host configuration"() {
    def mailhost = new MailHost("ghost.name", 25, "loser", "boozer", true, false, "go@foo.mail.com", "admin@foo.mail.com")

    def actualJson = toObjectString({ MailServerRepresenter.toJSON(it, mailhost) })

    def expectedJson = [
      _links            : [
        self: [href: 'http://test.host/go/api/config/mailserver'],
        doc : [href: CurrentGoCDVersion.apiDocsUrl('mailserver-config')]
      ],
      hostname          : mailhost.hostName,
      port              : mailhost.port,
      username          : mailhost.username,
      encrypted_password: mailhost.encryptedPassword,
      tls               : mailhost.tls,
      sender_email      : mailhost.from,
      admin_email       : mailhost.adminMail
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void "should deserialize mail host configurations"() {
    def mailhost = new MailHost("ghost.name", 25, "loser", "boozer", true, false, "go@foo.mail.com", "admin@foo.mail.com")

    def json = [
      hostname    : mailhost.hostName,
      port        : mailhost.port,
      username    : mailhost.username,
      password    : mailhost.password,
      tls         : mailhost.tls,
      sender_email: mailhost.from,
      admin_email : mailhost.adminMail
    ]

    def jsonReader = GsonTransformer.instance.jsonReaderFrom(json)
    def deserializedMailhost = MailServerRepresenter.fromJSON(jsonReader)

    assertThat(deserializedMailhost).isEqualTo(mailhost)
  }
}
