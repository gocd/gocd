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

import {SparkRoutes} from "helpers/spark_routes";
import {TemplateConfig} from "models/pipeline_configs/template_config";

describe("TemplateConfig model", () => {

  xit("getTemplate()", (done) => {
    jasmine.Ajax.withMock(() => {
      stubGetSuccess();

      TemplateConfig.getTemplate("name", (result) => {
        const params = result.parameters();
        expect(params.length).toBe(1);
        expect(params[0].name).toBe("foo");
        expect(params[0].value).toBe("bar");
        done();
      });
    });
  });
});

function stubGetSuccess() {
  jasmine.Ajax.stubRequest(SparkRoutes.templatesPath("name"), undefined, "GET").
    andReturn({
      responseText: JSON.stringify({ parameters: [{ name: "foo", value: "bar"}]}),
      status: 200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v7+json"
      }
    });
}
