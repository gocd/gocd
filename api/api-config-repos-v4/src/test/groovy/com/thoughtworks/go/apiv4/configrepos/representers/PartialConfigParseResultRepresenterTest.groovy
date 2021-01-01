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
package com.thoughtworks.go.apiv4.configrepos.representers

import com.thoughtworks.go.config.PartialConfigParseResult
import com.thoughtworks.go.config.remote.PartialConfig
import com.thoughtworks.go.helper.ModificationsMother
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class PartialConfigParseResultRepresenterTest {
  @Test
  void 'toJSON() with no result'() {
    String json = toObjectString({ w -> PartialConfigParseResultRepresenter.toJSON(w, null) })
    assertThatJson(json).isEqualTo([:])
  }

  @Test
  void 'toJSON() with parsed modification being good'() {
    def modification = ModificationsMother.oneModifiedFile("rev1")
    def partialConfig = new PartialConfig()
    def result = PartialConfigParseResult.parseSuccess(modification, partialConfig)
    def json = toObjectString({ w -> PartialConfigParseResultRepresenter.toJSON(w, result) })

    assertThatJson(json).isEqualTo([
      latest_parsed_modification: [
        username     : modification.userName,
        email_address: modification.emailAddress,
        revision     : modification.revision,
        comment      : modification.comment,
        modified_time: jsonDate(modification.modifiedTime)
      ],
      good_modification         : [
        username     : modification.userName,
        email_address: modification.emailAddress,
        revision     : modification.revision,
        comment      : modification.comment,
        modified_time: jsonDate(modification.modifiedTime)
      ],
      error                     : null
    ])
  }

  @Test
  void 'toJSON() with parsed modification being bad'() {
    def modification = ModificationsMother.oneModifiedFile("rev1")
    def exception = new Exception("Boom!")
    PartialConfigParseResult result = PartialConfigParseResult.parseFailed(modification, exception)
    String json = toObjectString({ w -> PartialConfigParseResultRepresenter.toJSON(w, result) })

    assertThatJson(json).isEqualTo([
      latest_parsed_modification: [
        username     : modification.userName,
        email_address: modification.emailAddress,
        revision     : modification.revision,
        comment      : modification.comment,
        modified_time: jsonDate(modification.modifiedTime)
      ],
      good_modification         : null,
      error                     : 'Boom!'
    ])
  }

  @Test
  void 'toJSON() with no modification and having an error'() {
    def exception = new Exception("Boom!")
    PartialConfigParseResult result = PartialConfigParseResult.parseFailed(null, exception)
    String json = toObjectString({ w -> PartialConfigParseResultRepresenter.toJSON(w, result) })

    assertThatJson(json).isEqualTo([
      latest_parsed_modification: null,
      good_modification         : null,
      error                     : 'Boom!'
    ])
  }

  @Test
  void 'toJSON() with old modification being good and latest parsed modification being bad'() {
    def modification = ModificationsMother.oneModifiedFile("rev1")
    def modification2 = ModificationsMother.oneModifiedFile("rev1")
    def exception = new Exception("Boom!")
    PartialConfigParseResult result = PartialConfigParseResult.parseFailed(modification, exception)
    result.setGoodModification(modification2)
    String json = toObjectString({ w -> PartialConfigParseResultRepresenter.toJSON(w, result) })

    assertThatJson(json).isEqualTo([
      latest_parsed_modification: [
        username     : modification.userName,
        email_address: modification.emailAddress,
        revision     : modification.revision,
        comment      : modification.comment,
        modified_time: jsonDate(modification.modifiedTime)
      ],
      good_modification         : [
        username     : modification2.userName,
        email_address: modification2.emailAddress,
        revision     : modification2.revision,
        comment      : modification2.comment,
        modified_time: jsonDate(modification2.modifiedTime)
      ],
      error                     : 'Boom!'
    ])
  }
}
