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

import m from "mithril";
import Stream from "mithril/stream";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {PipelineGroupCache} from "models/pipeline_configs/pipeline_groups_cache";
import {TemplateCache} from "models/pipeline_configs/templates_cache";
import {Option} from "views/components/forms/input_fields";
import {TestHelper} from "views/pages/spec/test_helper";
import {PipelineInfoEditor} from "../pipeline_info_editor";

const flag: (val?: boolean) => Stream<boolean> = Stream;

describe("AddPipeline: PipelineInfoEditor", () => {
  const helper = new TestHelper();
  let config: PipelineConfig;

  beforeEach(() => {
    config = new PipelineConfig("", [], []).withGroup("foo");
    helper.mount(() => <PipelineInfoEditor pipelineConfig={config} cache={new TestCache()} isUsingTemplate={flag(false)} templatesCache={new EmptyTemplatesTestCache()}/>);
  });

  afterEach(helper.unmount.bind(helper));

  it("Generates structure", () => {
    expect(helper.byTestId("form-field-label-pipeline-name")).toBeTruthy();
    expect(helper.byTestId("form-field-label-pipeline-name").textContent).toBe("Pipeline Name*");

    expect(helper.byTestId("form-field-input-pipeline-name")).toBeTruthy();
  });

  it("Binds to model", () => {
    expect(config.name()).toBe("");

    helper.oninput(helper.byTestId("form-field-input-pipeline-name"), "my-pipeline");
    expect(config.name()).toBe("my-pipeline");
  });
});

class TestCache implements PipelineGroupCache<Option> {
  ready() { return true; }
  // tslint:disable-next-line
  prime(onComplete: () => void) {}
  // tslint:disable-next-line
  invalidate() {}
  contents() { return []; }
  pipelineGroups() { return []; }
  stages(pipeline: string) { return []; }
  failureReason() { return undefined; }
  failed() { return false; }
}

class EmptyTemplatesTestCache extends TemplateCache {
  ready() { return true; }
  // tslint:disable-next-line
  prime(onComplete: () => void) { onComplete(); }
  // tslint:disable-next-line
  invalidate() {}
  contents() { return []; }
  templates() { return []; }
  failureReason() { return undefined; }
  failed() { return false; }
}
