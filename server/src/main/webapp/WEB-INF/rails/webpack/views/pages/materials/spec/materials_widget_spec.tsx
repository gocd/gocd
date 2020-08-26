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
import {docsUrl} from "gen/gocd_version";
import m from "mithril";
import Stream from "mithril/stream";
import {Materials, MaterialWithFingerprintJSON} from "models/materials/materials";
import {TestHelper} from "views/pages/spec/test_helper";
import {MaterialsWidget} from "../materials_widget";

describe('MaterialsWidgetSpec', () => {
  const helper = new TestHelper();
  let materials: Materials;

  afterEach((done) => helper.unmount(done));
  beforeEach(() => {
    materials = new Materials();
  });

  it('should display help text when no materials have been defined', () => {
    mount();

    const flashElement = helper.byTestId("flash-message-info");
    expect(flashElement).toBeInDOM();
    expect(flashElement.textContent).toContain('Either no pipelines have been set up or you are not authorized to view the same.');
    expect(helper.q('a', flashElement)).toHaveAttr('href', docsUrl('configuration/dev_authorization.html#specifying-permissions-for-pipeline-groups'));

    const helpElement = helper.byTestId("materials-help");
    expect(helpElement).toBeInDOM();
    expect(helpElement.textContent).toBe('A material is a cause for a pipeline to run. The GoCD Server continuously polls configured materials and when a new change or commit is found, the corresponding pipelines are run or "triggered". Learn More');
    expect(helper.q('a', helpElement)).toHaveAttr('href', docsUrl('introduction/concepts_in_go.html#materials'));
  });

  function mount() {
    helper.mount(() => <MaterialsWidget materials={Stream(materials)} shouldShowPackageOrScmLink={false}
                                        onEdit={jasmine.createSpy("onEdit")} showModifications={jasmine.createSpy("showModifications")}
                                        showUsages={jasmine.createSpy("showUsages")}/>);
  }
});

export function git() {
  return {
    type:        "git",
    fingerprint: "some-fingerprint",
    attributes:  {
      name:          "some-name",
      auto_update:   true,
      url:           "git@github.com:sample_repo/example.git",
      branch:        "master",
      shallow_clone: false
    }
  } as MaterialWithFingerprintJSON;
}
