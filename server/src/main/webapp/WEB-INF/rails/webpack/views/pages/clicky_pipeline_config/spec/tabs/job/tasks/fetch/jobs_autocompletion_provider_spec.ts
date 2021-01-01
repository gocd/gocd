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
import {JobsAutocompletionProvider} from "views/pages/clicky_pipeline_config/tabs/job/tasks/fetch/jobs_autocompletion_provider";
import {suggestions} from "./auto_suggestions_test_data";

describe("Jobs AutoCompletion Provider", () => {
  let pipeline: Stream<string | undefined>;
  let stage: Stream<string>;
  let provider: JobsAutocompletionProvider;

  function mount(pipelineName?: string, stageName?: string) {
    pipeline = Stream(pipelineName);
    stage    = Stream(stageName || "");
    provider = new JobsAutocompletionProvider(pipeline, stage, Stream(suggestions()));
  }

  it("should not get auto suggestions when no stage is configured", (done) => {
    mount();
    provider.getData().then((data) => {
      expect(data).toEqual([]);
      done();
    });
  });

  it("should get auto suggestions for a pipeline > stage", (done) => {
    mount("pipeline1", "pipeline1_stage2");
    provider.getData().then((data) => {
      expect(data).toEqual(["pipeline1_stage2_job1", "pipeline1_stage2_job2"]);
      done();
    });
  });
});
