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
import {DependencyMaterialAttributes, Material} from "models/materials/types";
import {TestHelper} from "views/pages/spec/test_helper";
import {DependencyFields, SuggestionCache} from "../non_scm_material_fields";

describe("AddPipeline: Non-SCM Material Fields", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("DependencyFields structure", () => {
    const material = new Material("dependency", new DependencyMaterialAttributes());
    helper.mount(() => <DependencyFields material={material} cache={new DummyCache()}/>);

    assertLabelledInputsPresent({
      "upstream-pipeline": "Upstream Pipeline*",
      "upstream-stage":    "Upstream Stage*",
      "material-name":     "Material Name",
    });
  });

  function assertLabelledInputsPresent(idsToLabels: {[key: string]: string}) {
    const keys = Object.keys(idsToLabels);
    expect(keys.length > 0).toBe(true);

    for (const id of keys) {
      expect(helper.byTestId(`form-field-label-${id}`)).toBeTruthy();
      expect(helper.byTestId(`form-field-label-${id}`).textContent!.startsWith(idsToLabels[id])).toBe(true);
      expect(helper.byTestId(`form-field-input-${id}`)).toBeTruthy();
    }
  }
});

class DummyCache implements SuggestionCache {
  ready() { return true; }
  // tslint:disable-next-line
  prime(onSuccess: () => void, onError?: () => void) {}
  contents() { return []; }
  pipelines() { return []; }
  stages(pipeline: string) { return []; }
  failureReason() { return undefined; }
  failed() { return false; }
  // tslint:disable-next-line
  invalidate() {}
}
