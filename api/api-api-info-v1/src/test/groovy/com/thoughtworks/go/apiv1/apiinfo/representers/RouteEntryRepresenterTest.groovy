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

package com.thoughtworks.go.apiv1.apiinfo.representers

import com.thoughtworks.go.spark.spring.RouteEntry
import org.junit.jupiter.api.Test
import spark.route.HttpMethod

import static com.thoughtworks.go.api.base.JsonUtils.toArrayString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class RouteEntryRepresenterTest {

  @Test
  void shouldConvertRouteEntriesToJson() {
    def entries = List.of(new RouteEntry(HttpMethod.get, "/api/:foo/:bar", "application/vnd.go.cd+v1.json", new Object()))
    def json = toArrayString({
      RouteEntryRepresenter.toJSON(it, entries)
    })

    def expectedJSON = [
      ["method"     : "get",
       "path"       : "/api/:foo/:bar",
       "version"    : "application/vnd.go.cd+v1.json",
       "path_params": [":foo", ":bar"]
      ]
    ]

    assertThatJson(json).isEqualTo(expectedJSON)
  }
}
