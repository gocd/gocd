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
import {SparkRoutes} from "helpers/spark_routes";
import {TestHelper} from "../../../../../webpack/views/pages/spec/test_helper";
import 'jasmine-ajax';
import {TriggerWithOptionsInfo} from "models/dashboard/trigger_with_options_info";
import {TriggerWithOptionsVM} from "views/dashboard/models/trigger_with_options_vm";
import {ModalBody} from "views/dashboard/trigger_with_options/modal_body";
import {Modal} from "views/shared/new_modal";
import Stream from "mithril/stream";
import m from "mithril";

describe("Dashboard Pipeline Trigger With Options Modal Body", () => {

  const json = {
    "variables": [
      {
        "name":   "version",
        "secure": false,
        "value":  "asdf"
      },
      {
        "name":   "foobar",
        "secure": true
      }
    ],
    "materials": [
      {
        "type":        "Git",
        "name":        "https://github.com/ganeshspatil/gocd",
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

  const helper = new TestHelper();

  beforeEach(() => {
    jasmine.Ajax.install();
    stubMaterialSearch(json.materials[0].fingerprint);

    triggerWithOptionsInfo = Stream(TriggerWithOptionsInfo.fromJSON(json));
    vm                     = new TriggerWithOptionsVM();
    vm.initialize(triggerWithOptionsInfo());
    mount(triggerWithOptionsInfo, vm);
  });

  let triggerWithOptionsInfo, vm;
  const pipelineName = 'up42';

  afterEach(() => {
    unmount();
    jasmine.Ajax.uninstall();
  });

  it("should render pipeline trigger with options information", () => {
    expect(helper.q('.pipeline-trigger-with-options')).toBeInDOM();
  });

  it("shoould render tab headings", () => {
    const headings = helper.qa('.pipeline_options-heading li');

    expect(headings.length).toBe(3);
    expect(headings[0]).toContainText('Materials');
    expect(headings[1]).toContainText('Environment variables');
    expect(headings[2]).toContainText('Secure Environment variables');
  });

  it("should render materials section", () => {
    expect(helper.q('.material-for-trigger')).toBeInDOM();
  });

  it("should render environment variables section", () => {
    expect(helper.q('.environment-variables.plain')).toBeInDOM();
  });

  it("should render secure environment variables section", () => {
    expect(helper.q('.environment-variables.secure')).toBeInDOM();
  });

  it("should show select materials tab by default", () => {
    expect(helper.q('.pipeline_options-heading li')).toHaveClass('active');

    const materialsContent = helper.q('.pipeline_options-body .h-tab_content');
    const envContent       = helper.q('.pipeline_options-body .h-tab_content:nth-child(2)');
    const secureEnvContent = helper.q('.pipeline_options-body .h-tab_content:nth-child(3)');

    expect(materialsContent).not.toHaveClass('hidden');
    expect(envContent).toHaveClass('hidden');
    expect(secureEnvContent).toHaveClass('hidden');
  });

  it("should show appropriate content based on tab selected", () => {
    const materialsContent = helper.q('.pipeline_options-body .h-tab_content');
    const envContent       = helper.q('.pipeline_options-body .h-tab_content:nth-child(2)');
    const secureEnvContent = helper.q('.pipeline_options-body .h-tab_content:nth-child(3)');

    expect(helper.q('.pipeline_options-heading li')).toHaveClass('active');

    expect(materialsContent).not.toHaveClass('hidden');
    expect(envContent).toHaveClass('hidden');
    expect(secureEnvContent).toHaveClass('hidden');

    helper.click(helper.qa('.pipeline_options-heading li').item(1));

    expect(helper.qa('.pipeline_options-heading li').item(1)).toHaveClass('active');

    expect(materialsContent).toHaveClass('hidden');
    expect(envContent).not.toHaveClass('hidden');
    expect(secureEnvContent).toHaveClass('hidden');
  });

  it('should not render the environment variables tab and content if no environment variables are present', () => {
    triggerWithOptionsInfo(TriggerWithOptionsInfo.fromJSON({variables: [], materials: json.materials}));
    vm.initialize(triggerWithOptionsInfo());
    m.redraw.sync();
    const headings = helper.qa('.pipeline_options-heading li');

    expect(headings.length).toBe(1);
    expect(headings.item(0)).toContainText('Materials');
  });

  it("should show error if API call failed", () => {
    const errorMessage = Stream("Error occurred while parsing the data.");
    unmount();
    mount(Stream(), new TriggerWithOptionsVM(), errorMessage);
    expect(helper.q('.callout.alert')).toBeInDOM();
  });

  function mount(triggerWithOptionsInfo, vm, errorMessage) {
    const searchVM = {
      [json.materials[0].name]: {
        performSearch:         jasmine.createSpy('performSearch'),
        searchText:            jasmine.createSpy('searchText'),
        searchInProgress:      jasmine.createSpy('searchInProgress'),
        materialSearchResults: jasmine.createSpy('materialSearchResult'),
        selectRevision:        jasmine.createSpy('selectRevision'),
        isRevisionSelected:    jasmine.createSpy('isRevisionSelected')
      }
    };

    helper.mount(() => m(ModalBody, {
      triggerWithOptionsInfo,
      vm: Stream(vm),
      message: errorMessage,
      searchVM
    }));
  }

  function unmount() {
    Modal.destroyAll();
    helper.unmount();
  }

  function stubMaterialSearch(fingerprint) {
    const url = SparkRoutes.pipelineMaterialSearchPath(pipelineName, fingerprint, "");

    jasmine.Ajax.stubRequest(url, undefined, 'GET').andReturn({
      status:          200,
      responseText:    JSON.stringify([]),
      responseHeaders: {'Content-Type': 'application/json'}
    });
  }
});
