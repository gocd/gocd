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
describe("Dashboard Pipeline Trigger With Options Modal Body", () => {
  const m                           = require("mithril");
  const Stream                      = require("mithril/stream");
  const Modal                       = require('views/shared/new_modal');
  const TriggerWithOptionsModalBody = require("views/dashboard/pipeline_trigger_with_options_modal_body");

  const TriggerWithOptionsVM   = require('views/dashboard/models/trigger_with_options_vm');
  const TriggerWithOptionsInfo = require("models/dashboard/trigger_with_options_info");

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  let triggerWithOptionsInfo, vm;

  beforeEach(mount);

  afterEach(unmount);

  it("should render pipeline trigger with options information", () => {
    expect($root.find('.pipeline-trigger-with-options')).toBeInDOM();
  });

  it("shoould render tab headings", () => {
    const headings = $root.find('.pipeline_options-heading li');

    expect(headings.length).toBe(3);
    expect(headings.get(0)).toContainText('Materials');
    expect(headings.get(1)).toContainText('Environment variables');
    expect(headings.get(2)).toContainText('Secure Environment variables');
  });

  it("should render materials section", () => {
    expect($root.find('.material-for-trigger')).toBeInDOM();
  });

  xit("should render environment variables section", () => {

  });

  xit("should render secure environment variables section", () => {
    
  });

  it("should show select materials tab by default", () => {
    expect($root.find('.pipeline_options-heading li').get(0)).toHaveClass('active');

    const materialsContent = $root.find('.pipeline_options-body .h-tab_content:first');
    const envContent       = $root.find('.pipeline_options-body .h-tab_content:nth-child(2)');
    const secureEnvContent = $root.find('.pipeline_options-body .h-tab_content:nth-child(3)');

    expect(materialsContent).not.toHaveClass('hidden');
    expect(envContent).toHaveClass('hidden');
    expect(secureEnvContent).toHaveClass('hidden');
  });

  it("should show appropriate content based on tab selected", () => {
    const materialsContent = $root.find('.pipeline_options-body .h-tab_content:first');
    const envContent       = $root.find('.pipeline_options-body .h-tab_content:nth-child(2)');
    const secureEnvContent = $root.find('.pipeline_options-body .h-tab_content:nth-child(3)');

    expect($root.find('.pipeline_options-heading li').get(0)).toHaveClass('active');

    expect(materialsContent).not.toHaveClass('hidden');
    expect(envContent).toHaveClass('hidden');
    expect(secureEnvContent).toHaveClass('hidden');

    $root.find('.pipeline_options-heading li').get(1).click();

    expect($root.find('.pipeline_options-heading li').get(1)).toHaveClass('active');

    expect(materialsContent).toHaveClass('hidden');
    expect(envContent).not.toHaveClass('hidden');
    expect(secureEnvContent).toHaveClass('hidden');
  });

  function mount() {
    triggerWithOptionsInfo = TriggerWithOptionsInfo.fromJSON(json);
    vm                     = new TriggerWithOptionsVM();
    vm.initialize(triggerWithOptionsInfo);

    m.mount(root, {
      view() {
        return m(TriggerWithOptionsModalBody, {
          triggerWithOptionsInfo: Stream(triggerWithOptionsInfo),
          vm:                     Stream(vm)
        });
      }
    });
    m.redraw(true);
  }

  function unmount() {
    Modal.destroyAll();
    m.mount(root, null);
    m.redraw();
  }

  const json = {
    "environment_variables":        [
      {
        "name":  "version",
        "value": "asdf"
      },
      {
        "name":  "foobar",
        "value": "asdf"
      }
    ],
    "secure_environment_variables": [
      {
        "name":  "secure1",
        "value": "****"
      },
      {
        "name":  "highly secure",
        "value": "****"
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
});
