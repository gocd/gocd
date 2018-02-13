/*
 * Copyright 2018 ThoughtWorks, Inc.
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

describe("Dashboard Trigger With Options Material Widget", () => {
  const m      = require("mithril");
  const Stream = require("mithril/stream");

  const TriggerWithOptionsVM   = require("views/dashboard/models/trigger_with_options_vm");
  const TriggerWithOptionsInfo = require('models/dashboard/trigger_with_options_info');

  const MaterialForTriggerWidget = require("views/dashboard/trigger_with_options/materials_widget");

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(() => {
    window.destroyDomElementForTest();
  });

  let vm, info, searchVM;
  beforeEach(() => {
    vm   = new TriggerWithOptionsVM();
    info = TriggerWithOptionsInfo.fromJSON(json);
    vm.initialize(info);

    searchVM = {
      [json.materials[0].name]: {
        performSearch:         jasmine.createSpy('performSearch'),
        searchText:            jasmine.createSpy('searchText'),
        searchInProgress:      jasmine.createSpy('searchInProgress'),
        materialSearchResults: jasmine.createSpy('materialSearchResult'),
        selectRevision:        jasmine.createSpy('selectRevision')
      },
      [json.materials[1].name]: {
        performSearch:         jasmine.createSpy('performSearch'),
        searchText:            jasmine.createSpy('searchText'),
        searchInProgress:      jasmine.createSpy('searchInProgress'),
        materialSearchResults: jasmine.createSpy('materialSearchResult'),
        selectRevision:        jasmine.createSpy('selectRevision')
      }
    };


    m.mount(root, {
      view() {
        return m(MaterialForTriggerWidget, {
          materials: info.materials,
          vm:        Stream(vm),
          searchVM
        });
      }
    });
    m.redraw();
  });

  afterEach(() => {
    m.mount(root, null);
    m.redraw();
  });

  it("it should render material select tabs", () => {
    const headings = $root.find('.v-tab_tab-head li');

    expect(headings).toHaveLength(json.materials.length);

    expect(headings.get(0)).toContainText(json.materials[0].revision.last_run_revision);
    expect(headings.get(0)).toContainText(json.materials[0].name);

    expect(headings.get(1)).toContainText(json.materials[1].revision.last_run_revision);
    expect(headings.get(1)).toContainText(json.materials[1].name);
  });

  it("it should render the first material info content", () => {
    const contents = $root.find('.v-tab_container .v-tab_content');

    expect(contents).toHaveLength(1);

    expect(contents.get(0)).toContainText(json.materials[0].name);
  });

  it("should show first material by default", () => {
    expect($root.find('.v-tab_tab-head li').get(0)).toHaveClass('active');
    expect($root.find('#material1').get(0)).not.toHaveClass('hidden');
  });

  it("should show appropriate material content material heading selection", () => {
    expect($root.find('.v-tab_tab-head li').get(0)).toHaveClass('active');
    expect($root.find('#material1').get(0)).not.toHaveClass('hidden');

    $root.find('.v-tab_tab-head li').get(1).click();

    expect($root.find('.v-tab_tab-head li').get(1)).toHaveClass('active');
    expect($root.find('#material1').get(1)).not.toHaveClass('hidden');
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
          "last_run_revision": "a2d23c5505ac571d9512bdf08d6287e47dcb52d5"
        }
      }
    ]
  };
});
