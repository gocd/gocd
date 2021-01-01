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

import {ApiResult} from "helpers/api_request_builder";
import {asSelector} from "helpers/css_proxies";
import {LocationHandler} from "helpers/location_handler";
import {SparkRoutes} from "helpers/spark_routes";
import m from "mithril";
import {GitMaterialAttributes, Material} from "models/materials/types";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {TestHelper} from "views/pages/spec/test_helper";
import {PipelineActions} from "../actions";
import css from "../actions.scss";
import * as errCss from "../server_errors.scss";

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
const errSel = asSelector<typeof errCss>(errCss);
const helper = new TestHelper();
const loc = new TestLocationHandler();

describe("AddPipeline: Actions Section", () => {
  let config: PipelineConfig;

  beforeEach(() => {
    loc.reset();
    config = new PipelineConfig("hello", [new Material("git", new GitMaterialAttributes())], []).withGroup("foo");
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

    expect(helper.q(errSel.errorResponse, saveActions)).toBeTruthy();
    expect(helper.q(errSel.errorResponse, saveActions).textContent).toBe("");

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
    expect(helper.text(errSel.errorResponse)).toBe("Please fix the validation errors above before proceeding.");
  });

  it("Cancel goes to the dashboard but does not create", () => {
    config.create = jasmine.createSpy("create");
    helper.click(sel.btnCancel);
    expect(config.create).not.toHaveBeenCalled();
    expect(loc.last()).toBe(`/go/pipelines`);
  });

  it("Displays errors when save fails and marks any fields with specific errors", (done) => {
    config.isValid = jasmine.createSpy("isValid").and.returnValue(true);
    const createPromise = createFailedResp(config).
      catch(() => done.fail("shouldn't have gotten here; 400 responses are handled in then()"));

    config.create = jasmine.createSpy("create").and.returnValue(createPromise);

    helper.click(sel.btnPrimary);
    expect(config.create).toHaveBeenCalled();

    createPromise.then(() => {
      setTimeout(() => { // allow the outer promise.then() wrapping createPromise to finish
        m.redraw.sync();
        expect(helper.text(errSel.errorResponse)).toBe("uh-oh!");
        const mat = Array.from(config.materials()).pop()!;
        expect(mat.attributes()!.errors().hasErrors("url")).toBe(true);
        expect(mat.attributes()!.errors().errorsForDisplay("url")).toBe("This url is bogus.");
        done();
      }, 0);
    });
  });

  it("Displays errors for unimplemented fields", (done) => {
    config.isValid = jasmine.createSpy("isValid").and.returnValue(true);
    const createPromise = createFailedRespWithUnboundErrors(config).
      catch(() => done.fail("shouldn't have gotten here; 400 responses are handled in then()"));

    config.create = jasmine.createSpy("create").and.returnValue(createPromise);

    helper.click(sel.btnPrimary);
    expect(config.create).toHaveBeenCalled();

    createPromise.then(() => {
      setTimeout(() => { // allow the outer promise.then() wrapping createPromise to finish
        m.redraw.sync();
        expect(helper.text(errSel.errorResponse)).toBe("uh-oh!: pipelineConfig.materials[0].something: unknown. error.");
        const mat = Array.from(config.materials()).pop()!;
        expect(mat.attributes()!.errors().hasErrors("url")).toBe(true);
        expect(mat.attributes()!.errors().errorsForDisplay("url")).toBe("This url is bogus.");
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
    const createPromise = createSuccessResp(config).catch(done.fail);
    config.create = jasmine.createSpy("create").and.returnValue(createPromise);

    createPromise.finally(() => {
      expect(config.isValid).toHaveBeenCalled();
      expect(config.create).toHaveBeenCalled();

      setTimeout(() => { // allow the outer promise.then() wrapping pausePromise to finish
        expect(loc.urls.length).toBe(1);
        expect(loc.last()).toBe(SparkRoutes.pipelineEditPath("pipelines", config.name(), "general"));
        done();
      }, 0);
    });

    helper.click(sel.btnSecondary);
  });
});

function createSuccessResp(config: PipelineConfig): Promise<ApiResult<string>> {
  return new Promise<ApiResult<string>>((resolve) => {
    resolve(ApiResult.success(JSON.stringify(config.toApiPayload()), 200, new Map()));
  });
}

function createFailedResp(config: PipelineConfig): Promise<ApiResult<string>> {
  return new Promise<ApiResult<string>>((resolve) => {
    resolve(ApiResult.error(JSON.stringify({
      message: "uh-oh!",
      data: {
        materials: [
          {errors: {url: ["This url is bogus"]}}
        ],
        stages: []
      }
    }), "uh-oh!", 422, new Map()));
  });
}

function createFailedRespWithUnboundErrors(config: PipelineConfig): Promise<ApiResult<string>> {
  return new Promise<ApiResult<string>>((resolve) => {
    resolve(ApiResult.error(JSON.stringify({
      message: "uh-oh!",
      data: {
        materials: [
          {errors: {url: ["This url is bogus"], something: ["unknown", "error"]}}
        ],
        stages: []
      }
    }), "uh-oh!", 422, new Map()));
  });
}

function runSuccessResp(config: PipelineConfig): Promise<ApiResult<string>> {
  return new Promise<ApiResult<string>>((resolve) => {
    resolve(ApiResult.success(JSON.stringify({message: `Request to schedule pipeline ${config.name()} accepted`}), 202, new Map()));
  });
}
