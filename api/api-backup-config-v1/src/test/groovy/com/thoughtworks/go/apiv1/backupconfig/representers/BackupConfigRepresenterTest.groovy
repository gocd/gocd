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
package com.thoughtworks.go.apiv1.backupconfig.representers

import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.config.BackupConfig
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat

class BackupConfigRepresenterTest {

  @Test
  void "should serialize"() {
    BackupConfig config = new BackupConfig()
      .setEmailOnFailure(true)
      .setEmailOnSuccess(true)
      .setPostBackupScript("/tmp/post-backup")
      .setSchedule("0 0 12 * * ?")

    def json = toObjectString({ BackupConfigRepresenter.toJSON(it, config) })

    assertThatJson(json).isEqualTo([
      _links            : [
        doc : [
          href: apiDocsUrl("#backup-config")
        ],
        self: [
          href: "http://test.host/go/api/config/backup"
        ]
      ],
      email_on_failure  : true,
      email_on_success  : true,
      post_backup_script: '/tmp/post-backup',
      schedule          : '0 0 12 * * ?'
    ])
  }

  @Test
  void 'should deserialize'() {
    def jsonReader = GsonTransformer.instance.jsonReaderFrom([
      email_on_failure  : true,
      email_on_success  : true,
      post_backup_script: '/tmp/post-backup',
      schedule          : '0 0 12 * * ?'
    ])

    BackupConfig expectedObject = new BackupConfig()
      .setEmailOnFailure(true)
      .setEmailOnSuccess(true)
      .setPostBackupScript("/tmp/post-backup")
      .setSchedule("0 0 12 * * ?")

    def actualObject = BackupConfigRepresenter.fromJSON(jsonReader)

    assertThat(actualObject).isEqualTo(expectedObject)
  }
}
