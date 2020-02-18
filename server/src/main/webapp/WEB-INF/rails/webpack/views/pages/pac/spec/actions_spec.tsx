/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import {ApiResult, ObjectWithEtag} from "helpers/api_request_builder";
import {asSelector} from "helpers/css_proxies";
import {LocationHandler} from "helpers/location_handler";
import {SparkRoutes} from "helpers/spark_routes";
import m from "mithril";
import stream from "mithril/stream";
import {ConfigReposCRUD} from "models/config_repos/config_repos_crud";
import {ConfigRepo} from "models/config_repos/types";
import {GitMaterialAttributes, Material} from "models/materials/types";
import {TestHelper} from "views/pages/spec/test_helper";
import {PacActions} from "../actions";
import css from "../styles.scss";

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

const sel    = asSelector<typeof css>(css);
const helper = new TestHelper();
const loc    = new TestLocationHandler();

describe("AddPaC: Actions Section", () => {
  let configRepo: ConfigRepo;

  beforeEach(() => {
    loc.reset();
    configRepo = new ConfigRepo("hello", ConfigRepo.YAML_PLUGIN_ID, new Material("git", new GitMaterialAttributes()));
    helper.mount(() => {
      return <PacActions configRepo={stream(configRepo)} loc={loc}/>;
    });
  });

  afterEach(helper.unmount.bind(helper));

  it("Generates element hierarchy and child elements", () => {
    const top = helper.q(sel.actions);
    expect(top).toBeTruthy();

    expect(helper.q(sel.btnCancel, top)).toBeTruthy();
    expect(helper.q(sel.btnCancel, top).textContent).toBe("Cancel");

    const saveActions = helper.q(sel.finishBtnWrapper, top);
    expect(saveActions).toBeTruthy();

    expect(helper.q(sel.errorResponse, saveActions)).toBeTruthy();
    expect(helper.q(sel.errorResponse, saveActions).textContent).toBe("");

    expect(helper.q(sel.btnPrimary, saveActions)).toBeTruthy();
    expect(helper.q(sel.btnPrimary, saveActions).textContent).toBe("Finish");
  });

  it("Does not create a config repo unless the model validates", () => {
    spyOn(ConfigReposCRUD, "create").and.returnValue(new Promise((resolve) => resolve()));
    configRepo.isValid = jasmine.createSpy("isValid").and.returnValue(false);
    helper.click(sel.btnPrimary);

    expect(configRepo.isValid).toHaveBeenCalled();
    expect(ConfigReposCRUD.create).not.toHaveBeenCalled();
    expect(helper.text(sel.errorResponse)).toBe("Please fix the validation errors above before proceeding.");
  });

  it("Cancel goes to the dashboard but does not create", () => {
    spyOn(ConfigReposCRUD, "create").and.returnValue(new Promise((resolve) => resolve()));
    helper.click(sel.btnCancel);
    expect(ConfigReposCRUD.create).not.toHaveBeenCalled();
    expect(loc.last()).toBe(`/go/pipelines`);
  });

  it("Finish creates a config repo and goes to the pac page with focus on that config repo when successful", (done) => {
    configRepo.isValid  = jasmine.createSpy("isValid").and.returnValue(true);
    const promise       = new Promise<ApiResult<ObjectWithEtag<ConfigRepo>>>((resolve) => {
      resolve(ApiResult.success("", 200, new Map()).map(() => {
        return {object: configRepo, etag: "some-value"};
      }));
    });

    promise.catch(done.fail);

    spyOn(ConfigReposCRUD, "create").and.returnValue(promise);

    promise.finally(() => {
      expect(configRepo.isValid).toHaveBeenCalled();
      expect(ConfigReposCRUD.create).toHaveBeenCalled();

      setTimeout(() => { // allow the outer promise.then() wrapping runPromise to finish
        expect(loc.urls.length).toBe(1);
        expect(loc.last()).toBe(SparkRoutes.ConfigRepoViewPath(configRepo.id()));
        done();
      }, 0);
    });

    helper.click(sel.btnPrimary);
  });
});
