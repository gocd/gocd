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

import Stream from "mithril/stream";
import {PipelinesAutocompletionProvider} from "views/pages/clicky_pipeline_config/tabs/job/tasks/fetch/pipelines_autocompletion_provider";
import {suggestions} from "./auto_suggestions_test_data";

describe("Pipelines AutoCompletion Provider", () => {
  let provider: PipelinesAutocompletionProvider;

  function mount() {
    provider = new PipelinesAutocompletionProvider(Stream(suggestions()));
  }

  it("should get auto suggestions", (done) => {
    mount();
    provider.getData().then((data) => {
      expect(data).toEqual(["pipeline1", "pipeline2"]);
      done();
    });
  });
});
