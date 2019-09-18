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

import m from "mithril";
import Stream from "mithril/stream";
import {PipelineParameter} from "models/pipeline_configs/parameter";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {TemplateCache} from "models/pipeline_configs/templates_cache";
import {Option} from "views/components/forms/input_fields";
import {TestHelper} from "views/pages/spec/test_helper";
import {TemplateEditor} from "../template_editor";

describe("AddPipeline: TemplateEditor", () => {
  const helper = new TestHelper();
  let isUsingTemplate: Stream<boolean>;
  let config: PipelineConfig;
  let paramList: Stream<PipelineParameter[]>;

  beforeEach(() => {
    config = new PipelineConfig("", [], []).withGroup("foo");
    isUsingTemplate = Stream();
    paramList = Stream();
  });

  afterEach(() => {
    isUsingTemplate(false);
    paramList([]);
    helper.unmount();
  });

  it("should display flash when no templates are defined", () => {
    helper.mount(() => <TemplateEditor pipelineConfig={config} isUsingTemplate={isUsingTemplate} cache={new EmptyTestCache()} paramList={paramList}/>);

    helper.clickByDataTestId("switch-paddle");
    expect(isUsingTemplate()).toBeTruthy();
    const flash = helper.byTestId("flash-message-warning");
    expect(flash).toBeInDOM();
    expect(helper.text("code", flash)).toBe("There are no templates configured or you are unauthorized to view the existing templates. Add one via the templates page.");
  });

  it("should show dropdown of templates when defined", () => {
    stubGetTemplateWith(new TemplateConfig("one", []));
    helper.mount(() => <TemplateEditor pipelineConfig={config} isUsingTemplate={isUsingTemplate} cache={new TestCache()} paramList={paramList}/>);

    helper.clickByDataTestId("switch-paddle");
    expect(isUsingTemplate()).toBeTruthy();
    expect(config.template()).toBe("one");
    expect(TemplateConfig.getTemplate).toHaveBeenCalledTimes(1);

    const dropdown = helper.byTestId("form-field-input-template");
    expect(dropdown).toBeInDOM();
    expect(helper.qa("option", dropdown).length).toBe(2);

    helper.clickByDataTestId("switch-paddle");
    expect(isUsingTemplate()).toBe(false);
    expect(config.template()).toBeUndefined();
    expect(TemplateConfig.getTemplate).toHaveBeenCalledTimes(1);
  });

  it("should not display dropdown and should display flash when cache fails to populate", () => {
    helper.mount(() => <TemplateEditor pipelineConfig={config} isUsingTemplate={isUsingTemplate} cache={new FailedTestCache()} paramList={paramList}/>);

    helper.clickByDataTestId("switch-paddle");
    expect(isUsingTemplate()).toBeTruthy();

    const flash = helper.byTestId("flash-message-warning");
    expect(flash).toBeInDOM();
    expect(helper.text("code", flash)).toBe("There are no templates configured or you are unauthorized to view the existing templates. Add one via the templates page.");

    const dropdown = helper.byTestId("form-field-input-template");
    expect(dropdown).not.toExist();
  });

  it("should populate parameters when present", (done) => {
    stubGetTemplateWith(new TemplateConfig("one", [new PipelineParameter("paramName", "")]), () => {
      expect(paramList().length).toBe(1);
      expect(paramList()[0].toApiPayload()).toEqual({ name: "paramName", value: "" });
      done();
    });

    helper.mount(() => <TemplateEditor pipelineConfig={config} isUsingTemplate={isUsingTemplate} cache={new TestCache()} paramList={paramList}/>);

    helper.clickByDataTestId("switch-paddle");
    expect(isUsingTemplate()).toBeTruthy();
    expect(TemplateConfig.getTemplate).toHaveBeenCalled();
  });
});

function stubGetTemplateWith(templateConfig: TemplateConfig, expectations?: () => void) {
  spyOn(TemplateConfig, "getTemplate").and.callFake((name, onSuccess) => {
    onSuccess(templateConfig);

    expect(name).toBe(templateConfig.name());

    if ("function" === typeof expectations) {
      expectations();
    }
  });
}

class TestCache implements TemplateCache<Option> {
  ready() { return true; }
  // tslint:disable-next-line
  prime(onComplete: () => void) { onComplete(); }
   // tslint:disable-next-line
  invalidate() {}
  contents() { return [{name: "one"}, {name: "two"}]; }
  templates() { return [{id: "one", text: "one"}, {id: "two", text: "two"}]; }
  failureReason() { return undefined; }
  failed() { return false; }
}

class EmptyTestCache implements TemplateCache<Option> {
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

class FailedTestCache implements TemplateCache<Option> {
  ready() { return true; }
  prime(onSuccess: () => void, onError?: () => void) { if (onError) {
    onError();
  }}
  // tslint:disable-next-line
  invalidate() {}
  contents() { return []; }
  templates() { return []; }
  failureReason() { return "Unauthorized to perform this action"; }
  failed() { return true; }
}
