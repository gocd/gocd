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

package com.thoughtworks.go.apiv1.internalscms.representers

import com.thoughtworks.go.domain.config.Configuration
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import com.thoughtworks.go.domain.scm.SCM
import com.thoughtworks.go.domain.scm.SCMMother
import com.thoughtworks.go.plugin.api.response.Result
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class VerifyConnectionResultRepresenterTest {
    @Test
    void 'should return verify connection result with success status and messages'() {
        SCM scm = SCMMother.create("1", "foobar", "plugin1", "v1.0", new Configuration(
                ConfigurationPropertyMother.create("key1", false, "value1")
        ))
        Result result = new Result();
        result.withSuccessMessages("message 1", "message 2")

        def actualJson = toObjectString({ VerifyConnectionResultRepresenter.toJSON(it, scm, result) })

        def expectedJson = [
        "status"  : "success",
                "messages": [
        "message 1",
                "message 2"
      ],
        "scm"     : toObject({ SCMRepresenter.toJSON(it, scm) })
    ]

        assertThatJson(actualJson).isEqualTo(expectedJson)
    }

    @Test
    void 'should return verify connection result with failure status and messages'() {
        SCM scm = SCMMother.create("1", "foobar", "plugin1", "v1.0", new Configuration(
                ConfigurationPropertyMother.create("key1", false, "value1")
        ))
        Result result = new Result();
        result.withErrorMessages("message 1", "message 2")

        def actualJson = toObjectString({ VerifyConnectionResultRepresenter.toJSON(it, scm, result) })

        def expectedJson = [
        "status"  : "failure",
                "messages": [
        "message 1",
                "message 2"
      ],
        "scm"     : toObject({ SCMRepresenter.toJSON(it, scm) })
    ]

        assertThatJson(actualJson).isEqualTo(expectedJson)
    }
}