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

import * as m from "mithril";
import * as stream from "mithril/stream";
import {Stream} from "mithril/stream";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {TemplateCache} from "models/pipeline_configs/templates_cache";
import {Option} from "views/components/forms/input_fields";
import {TestHelper} from "views/pages/spec/test_helper";
import {TemplateEditor} from "../template_editor";

describe("AddPipeline: TemplateEditor", () => {
  const helper = new TestHelper();
  const isUsingTemplate: Stream<boolean> = stream(false);
  let config: PipelineConfig;

  beforeEach(() => {
    config = new PipelineConfig("", [], []);
    helper.mount(() => <TemplateEditor pipelineConfig={config} isUsingTemplate={isUsingTemplate} cache={new EmptyTestCache()}/>);
  });

  afterEach(() => {
    isUsingTemplate(false);
    helper.unmount();
  });

  it("should display flash when no templates are defined", () => {
    helper.click(helper.byTestId("switch-paddle"));
    expect(isUsingTemplate()).toBeTruthy();
    const flash = helper.byTestId("flash-message-info");
    expect(flash).toBeTruthy();
    expect(helper.text("pre", flash)).toBe("There are no pipeline templates configured. Add one via the templates page.");
  });

  it("should show dropdown of templates when defined", () => {
    helper.unmount();
    helper.mount(() => <TemplateEditor pipelineConfig={config} isUsingTemplate={isUsingTemplate} cache={new TestCache()}/>);
    helper.click(helper.byTestId("switch-paddle"));
    expect(isUsingTemplate()).toBeTruthy();
    expect(config.template()).toBe("one");
    const dropdown = helper.byTestId("form-field-input-template");
    expect(dropdown).toBeTruthy();
    expect(helper.qa("option", dropdown).length).toBe(2);

    helper.click(helper.byTestId("switch-paddle"));
    expect(isUsingTemplate()).toBe(false);
    expect(config.template() === undefined).toBeTruthy();
  });
});

class TestCache implements TemplateCache<Option> {
  ready() { return true; }
  // tslint:disable-next-line
  prime(onComplete: () => void) { onComplete(); }
  templates() { return [{id: "one", text: "one"}, {id: "two", text: "two"}]; }
  failureReason() { return undefined; }
  failed() { return false; }
}

class EmptyTestCache implements TemplateCache<Option> {
  ready() { return true; }
  // tslint:disable-next-line
  prime(onComplete: () => void) { onComplete(); }
  templates() { return []; }
  failureReason() { return undefined; }
  failed() { return false; }
}
