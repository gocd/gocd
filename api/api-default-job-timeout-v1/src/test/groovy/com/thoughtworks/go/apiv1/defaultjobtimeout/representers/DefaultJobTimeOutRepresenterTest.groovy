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
package com.thoughtworks.go.apiv1.defaultjobtimeout.representers

import com.thoughtworks.go.api.representers.JsonReader
import com.thoughtworks.go.api.util.GsonTransformer
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.assertj.core.api.Assertions.assertThat

class DefaultJobTimeOutRepresenterTest {

  @Test
  void 'should represent default job timeout'() {
    String defaultJobTimeout = "10"
    def json = toObjectString({ DefaultJobTimeOutRepresenter.toJSON(it, defaultJobTimeout) })

    assertThatJson(json).isEqualTo(["_links"             :
                                      ["doc" : ["href": apiDocsUrl("#default-job-timeout")],
                                       "self": ["href": "http://test.host/go/api/admin/config/server/default_job_timeout"]],
                                    "default_job_timeout": "10"])
  }

  @Test
  void 'should deserialize default job timeout'() {
    def json = ["default_job_timeout": "10"]
    JsonReader jsonReader = GsonTransformer.getInstance().jsonReaderFrom(json)
    def config = DefaultJobTimeOutRepresenter.fromJson(jsonReader)

    def defaultJobTimeout = "10"

    assertThat(config).isEqualTo(defaultJobTimeout)
  }
}
