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
import {TestHelper} from "../../../../../webpack/views/pages/spec/test_helper";
import {MaterialsForTriggerWidget} from "views/dashboard/trigger_with_options/materials_widget";
import {TriggerWithOptionsInfo} from "models/dashboard/trigger_with_options_info";
import {TriggerWithOptionsVM} from "views/dashboard/models/trigger_with_options_vm";
import Stream from "mithril/stream";
import m from "mithril";
import _ from "lodash";

describe("Dashboard Trigger With Options Material Widget", () => {

  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

  let vm, info;
  beforeEach(() => {
    vm   = new TriggerWithOptionsVM();
    info = TriggerWithOptionsInfo.fromJSON(json);
    vm.initialize(info);

    _.each(info.materials, (material) => {
      material.performSearch         = jasmine.createSpy('performSearch');
      material.searchText            = jasmine.createSpy('searchText');
      material.searchInProgress      = jasmine.createSpy('searchInProgress');
      material.materialSearchResults = jasmine.createSpy('materialSearchResult');
      material.selectRevision        = jasmine.createSpy('selectRevision');
      material.isRevisionSelected    = jasmine.createSpy('isRevisionSelected');
    });

    helper.mount(() => m(MaterialsForTriggerWidget, {
      materials: info.materials,
      vm:        Stream(vm)
    }));
  });

  it("should show latest if material revision is not present", () => {
    const headings = helper.qa('.v-tab_tab-head li')[1];

    expect(headings).toContainText("latest");
  });

  it("it should render material select tabs", () => {
    const headings = helper.textAll('.v-tab_tab-head li');

    expect(headings).toHaveLength(json.materials.length);

    expect(headings[0]).toContain(json.materials[0].revision.last_run_revision);
    expect(headings[0]).toContain(json.materials[0].name);

    expect(headings[1]).toContain("latest");
    expect(headings[1]).toContain(json.materials[1].name);
  });

  it("it should render the first material info content", () => {
    const contents = helper.qa('.v-tab_container .v-tab_content');

    expect(contents).toHaveLength(1);

    expect(contents[0]).toContainText(json.materials[0].name);
  });

  it("should show first material by default", () => {
    expect(helper.q('.v-tab_tab-head li')).toHaveClass('active');
    expect(helper.q('.v-tab_tab-head .active .material-name')).toHaveText('material1');
  });

  it("should show appropriate material content material heading selection", () => {
    expect(helper.q('.v-tab_tab-head li')).toHaveClass('active');
    expect(helper.q('.v-tab_tab-head .active .material-name')).toHaveText('material1');

    helper.click(helper.qa('.v-tab_tab-head li')[1]);

    expect(helper.qa('.v-tab_tab-head li').item(1)).toHaveClass('active');
    expect(helper.q('.v-tab_tab-head .active .material-name')).toHaveText('material2');
  });

  const json = {
    "variables": [],
    "materials": [
      {
        "type":        "Git",
        "name":        "material1",
        "fingerprint": "3dcc10e7943de637211a4742342fe456ffbe832577bb377173007499434fd819",
        "revision":    {
          "date":              "2018-02-08T04:32:11Z",
          "user":              "Ganesh S Patil <ganeshpl@thoughtworks.com>",
          "comment":           "Refactor Pipeline Widget (#4311)\n\n* Extract out PipelineHeaderWidget and PipelineOperationsWidget into seperate msx files",
          "last_run_revision": "a2d23c5505ac571d9512bdf08d6287e47dcb52d5"
        }
      },

      {
        "type":        "Git",
        "name":        "material2",
        "fingerprint": "3dcc10e7943de637211a4742342fe456ffbe832577bb377173007499434fd819",
        "revision":    {
          "date":              "2018-02-08T04:32:11Z",
          "user":              "Ganesh S Patil <ganeshpl@thoughtworks.com>",
          "comment":           "Refactor Pipeline Widget (#4311)\n\n* Extract out PipelineHeaderWidget and PipelineOperationsWidget into seperate msx files",
          "last_run_revision": null
        }
      }
    ]
  };
});
