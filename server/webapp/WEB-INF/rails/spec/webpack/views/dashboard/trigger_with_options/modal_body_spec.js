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

import SparkRoutes from "helpers/spark_routes";
import {TestHelper} from "../../../../../webpack/views/pages/artifact_stores/spec/test_helper";

describe("Dashboard Pipeline Trigger With Options Modal Body", () => {
  const m                           = require("mithril");
  const Stream                      = require("mithril/stream");
  const Modal                       = require('views/shared/new_modal');
  const TriggerWithOptionsModalBody = require("views/dashboard/trigger_with_options/modal_body");
  require('jasmine-ajax');

  const TriggerWithOptionsVM   = require('views/dashboard/models/trigger_with_options_vm');
  const TriggerWithOptionsInfo = require("models/dashboard/trigger_with_options_info");

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
    expect(helper.find('.pipeline-trigger-with-options')).toBeInDOM();
  });

  it("shoould render tab headings", () => {
    const headings = helper.find('.pipeline_options-heading li');

    expect(headings.length).toBe(3);
    expect(headings.get(0)).toContainText('Materials');
    expect(headings.get(1)).toContainText('Environment variables');
    expect(headings.get(2)).toContainText('Secure Environment variables');
  });

  it("should render materials section", () => {
    expect(helper.find('.material-for-trigger')).toBeInDOM();
  });

  it("should render environment variables section", () => {
    expect(helper.find('.environment-variables.plain')).toBeInDOM();
  });

  it("should render secure environment variables section", () => {
    expect(helper.find('.environment-variables.secure')).toBeInDOM();
  });

  it("should show select materials tab by default", () => {
    expect(helper.find('.pipeline_options-heading li').get(0)).toHaveClass('active');

    const materialsContent = helper.find('.pipeline_options-body .h-tab_content:first');
    const envContent       = helper.find('.pipeline_options-body .h-tab_content:nth-child(2)');
    const secureEnvContent = helper.find('.pipeline_options-body .h-tab_content:nth-child(3)');

    expect(materialsContent).not.toHaveClass('hidden');
    expect(envContent).toHaveClass('hidden');
    expect(secureEnvContent).toHaveClass('hidden');
  });

  it("should show appropriate content based on tab selected", () => {
    const materialsContent = helper.find('.pipeline_options-body .h-tab_content:first');
    const envContent       = helper.find('.pipeline_options-body .h-tab_content:nth-child(2)');
    const secureEnvContent = helper.find('.pipeline_options-body .h-tab_content:nth-child(3)');

    expect(helper.find('.pipeline_options-heading li').get(0)).toHaveClass('active');

    expect(materialsContent).not.toHaveClass('hidden');
    expect(envContent).toHaveClass('hidden');
    expect(secureEnvContent).toHaveClass('hidden');

    helper.find('.pipeline_options-heading li').get(1).click();

    expect(helper.find('.pipeline_options-heading li').get(1)).toHaveClass('active');

    expect(materialsContent).toHaveClass('hidden');
    expect(envContent).not.toHaveClass('hidden');
    expect(secureEnvContent).toHaveClass('hidden');
  });

  it('should not render the environment variables tab and content if no environment variables are present', () => {
    triggerWithOptionsInfo(TriggerWithOptionsInfo.fromJSON({variables: [], materials: json.materials}));
    vm.initialize(triggerWithOptionsInfo());
    m.redraw();
    const headings = helper.find('.pipeline_options-heading li');

    expect(headings.length).toBe(1);
    expect(headings.get(0)).toContainText('Materials');
  });

  it("should show error if API call failed", () => {
    const errorMessage = Stream("Error occurred while parsing the data.");
    unmount();
    mount(Stream(), new TriggerWithOptionsVM(), errorMessage);
    expect(helper.find('.callout.alert')).toBeInDOM();
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

    helper.mount(() => m(TriggerWithOptionsModalBody, {
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
