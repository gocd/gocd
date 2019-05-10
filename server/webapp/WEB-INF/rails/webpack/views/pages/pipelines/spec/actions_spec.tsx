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

import * as Routes from "gen/ts-routes";
import {ApiResult} from "helpers/api_request_builder";
import asSelector from "helpers/selector_proxy";
import * as m from "mithril";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {TestHelper} from "views/pages/spec/test_helper";
import {LocationHandler, PipelineActions} from "../actions";
import * as css from "../components.scss";

class TestLocationHandler implements LocationHandler {
  urls: string[] = [];

  go(url: string) {
    this.urls.push(url);
  }

  last(): string | undefined {
    return this.urls[this.urls.length - 1];
  }

  reset() {
    this.urls = [];
  }
}

const sel = asSelector<typeof css>(css);
const helper = new TestHelper();
const loc = new TestLocationHandler();

describe("AddPipeline: Actions Section", () => {

  let config: PipelineConfig;

  beforeEach(() => {
    loc.reset();
    config = new PipelineConfig("hello", [], []);
    helper.mount(() => {
      return <PipelineActions pipelineConfig={config} loc={loc}/>;
    });
  });

  afterEach(helper.unmount.bind(helper));

  it("Generates element hierarchy and child elements", () => {
    const top = helper.q(sel.actions);
    expect(top).toBeTruthy();

    expect(helper.q(sel.btnCancel, top)).toBeTruthy();
    expect(helper.q(sel.btnCancel, top).textContent).toBe("Cancel");

    const saveActions = helper.q(sel.saveBtns, top);
    expect(saveActions).toBeTruthy();

    expect(helper.q(sel.errorResponse, saveActions)).toBeTruthy();
    expect(helper.q(sel.errorResponse, saveActions).textContent).toBe("");

    expect(helper.q(sel.btnSecondary, saveActions)).toBeTruthy();
    expect(helper.q(sel.btnSecondary, saveActions).textContent).toBe("Save + Edit Full Config");

    expect(helper.q(sel.btnPrimary, saveActions)).toBeTruthy();
    expect(helper.q(sel.btnPrimary, saveActions).textContent).toBe("Save + Run This Pipeline");
  });

  it("Does not create a pipeline unless the model validates", () => {
    config.create = jasmine.createSpy();
    config.isValid = jasmine.createSpy("isValid").and.returnValue(false);
    helper.click(sel.btnPrimary);

    expect(config.isValid).toHaveBeenCalled();
    expect(config.create).not.toHaveBeenCalled();
    expect(helper.text(sel.errorResponse)).toBe("Please fix the validation errors above before proceeding.");
  });

  it("Cancel goes to the dashboard but does not create", () => {
    config.create = jasmine.createSpy("create");
    helper.click(sel.btnCancel);
    expect(config.create).not.toHaveBeenCalled();
    expect(loc.last()).toBe(`/go/pipelines`);
  });

  it("Displays errors when save fails", (done) => {
    config.isValid = jasmine.createSpy("isValid").and.returnValue(true);
    const createPromise = createFailedResp(config).
      catch(() => done.fail("shouldn't have gotten here; 400 responses are handled in then()"));

    config.create = jasmine.createSpy("create").and.returnValue(createPromise);

    helper.click(sel.btnPrimary);
    expect(config.create).toHaveBeenCalled();

    createPromise.then(() => {
      setTimeout(() => { // allow the outer promise.then() wrapping createPromise to finish
        expect(helper.q(sel.errorResponse).textContent).toBe("Error: uh-oh!");
        done();
      }, 0);
    });
  });

  it("Save and Run creates a pipeline and goes to the dashboard when successful", (done) => {
    config.isValid = jasmine.createSpy("isValid").and.returnValue(true);
    config.create = jasmine.createSpy("create").and.
      returnValue(createSuccessResp(config).catch(done.fail));

    const runPromise = runSuccessResp(config).catch(done.fail);
    config.run = jasmine.createSpy("run").and.returnValue(runPromise);

    runPromise.finally(() => {
      expect(config.isValid).toHaveBeenCalled();
      expect(config.create).toHaveBeenCalled();
      expect(config.run).toHaveBeenCalled();

      setTimeout(() => { // allow the outer promise.then() wrapping runPromise to finish
        expect(loc.urls.length).toBe(1);
        expect(loc.last()).toBe(`/go/pipelines?new_pipeline_name=${config.name()}`);
        done();
      }, 0);
    });

    helper.click(sel.btnPrimary);
  });

  it("Save and Edit goes saves and enters edit page", (done) => {
    config.isValid = jasmine.createSpy("isValid").and.returnValue(true);
    config.create = jasmine.createSpy("create").and.
      returnValue(createSuccessResp(config).catch(done.fail));

    const pausePromise = pauseSuccessResp(config).catch(done.fail);
    config.pause = jasmine.createSpy("pause").and.returnValue(pausePromise);

    pausePromise.finally(() => {
      expect(config.isValid).toHaveBeenCalled();
      expect(config.create).toHaveBeenCalled();
      expect(config.pause).toHaveBeenCalled();

      setTimeout(() => { // allow the outer promise.then() wrapping pausePromise to finish
        expect(loc.urls.length).toBe(1);
        expect(loc.last()).toBe(Routes.pipelineEditPath("pipelines", config.name(), "general"));
        done();
      }, 0);
    });

    helper.click(sel.btnSecondary);
  });
});

function createSuccessResp(config: PipelineConfig): Promise<ApiResult<string>> {
  return new Promise<ApiResult<string>>((resolve) => {
    resolve(ApiResult.success(JSON.stringify(config.toApiPayload()), 200, null));
  });
}

function createFailedResp(config: PipelineConfig): Promise<ApiResult<string>> {
  return new Promise<ApiResult<string>>((resolve) => {
    resolve(ApiResult.error("", "uh-oh!", 422));
  });
}

function runSuccessResp(config: PipelineConfig): Promise<ApiResult<string>> {
  return new Promise<ApiResult<string>>((resolve) => {
    resolve(ApiResult.success(JSON.stringify({message: `Request to schedule pipeline ${config.name()} accepted`}), 202, null));
  });
}

function pauseSuccessResp(config: PipelineConfig): Promise<ApiResult<string>> {
  return new Promise<ApiResult<string>>((resolve) => {
    resolve(ApiResult.success(JSON.stringify({message: `Pipeline '${config.name()}' paused successfully.`}), 202, null));
  });
}
