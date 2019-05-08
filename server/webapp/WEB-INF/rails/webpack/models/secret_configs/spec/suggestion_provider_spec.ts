/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import {DynamicSuggestionProvider} from "models/secret_configs/suggestion_provider";

interface PipelineGroup {
  name: string;
}

function pipelineGroupTestData() {
  return {
    _embedded: {
      groups: [
        {
          name: "first"
        }
        , {
          name: "second"
        }
      ]
    }
  };
}

function pipelineGroupsResponse() {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
      "ETag": "some-etag"
    },
    responseText: JSON.stringify(pipelineGroupTestData())
  };
}

describe("DynamicSuggestionProvider", () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  it("should show suggestions for pipeline group", (done) => {
    jasmine.Ajax.stubRequest("/go/api/admin/pipeline_groups").andReturn(pipelineGroupsResponse());
    const provider = new DynamicSuggestionProvider("pipeline_group");

    const onResponse = jasmine.createSpy().and.callFake((response: PipelineGroup[]) => {
      expect(response).toHaveLength(2);
      done();
    });

    provider.getData().then(onResponse);
  });
});
