/*
 * Copyright 2017 ThoughtWorks, Inc.
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
describe("Dashboard Pipeline Instance Widget", () => {
  const m      = require("mithril");
  const Stream = require("mithril/stream");

  const PipelineInstanceWidget = require("views/dashboard/pipeline_instance_widget");
  const PipelineInstance       = require('models/dashboard/pipeline_instance');

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  const pipelineInstanceJson = {
    "_links":       {
      "self":            {
        "href": "http://localhost:8153/go/api/pipelines/up42/instance/1"
      },
      "doc":             {
        "href": "https://api.go.cd/current/#get-pipeline-instance"
      },
      "history_url":     {
        "href": "http://localhost:8153/go/api/pipelines/up42/history"
      },
      "vsm_url":         {
        "href": "http://localhost:8153/go/pipelines/value_stream_map/up42/1"
      },
      "compare_url":     {
        "href": "http://localhost:8153/go/compare/up42/0/with/1"
      },
      "build_cause_url": {
        "href": "http://localhost:8153/go/pipelines/up42/1/build_cause"
      }
    },
    "label":        "1",
    "scheduled_at":  "2017-11-10T07:25:28.539Z",
    "triggered_by": "changes",
    "_embedded":    {
      "stages": [
        {
          "_links":       {
            "self": {
              "href": "http://localhost:8153/go/api/stages/up42/1/up42_stage/1"
            },
            "doc":  {
              "href": "https://api.go.cd/current/#get-stage-instance"
            }
          },
          "name":         "up42_stage",
          "status":       "Failed",
          "approved_by":  "changes",
          "scheduled_at": "2017-11-10T07:25:28.539Z"
        }
      ]
    }
  };

  const instance = Stream(new PipelineInstance(pipelineInstanceJson));

  beforeEach(() => {
    m.mount(root, {
      view() {
        return m(PipelineInstanceWidget, {
          instance
        });
      }
    });
    m.redraw(true);
  });

  afterEach(() => {
    m.mount(root, null);
    m.redraw();
  });

  it("should render instance label", () => {
    expect($root.find('.instance-label')).toContainText('Instance: 1');
  });

  it("should render triggered by information", () => {
    expect($root.find('.instance-details')).toContainText(`${  pipelineInstanceJson.triggered_by }`);
    expect($root.find('.instance-details')).toContainText(`on ${ new Date(pipelineInstanceJson.scheduled_at).toString()}`);
  });

  it("should render compare link", () => {
    const links       = $root.find('.links a');
    const compareLink = links.get(0);

    expect(compareLink).toContainText('Compare');
    expect(compareLink.href).toEqual(pipelineInstanceJson._links.compare_url.href);
  });

  it("should render vsm link", () => {
    const links       = $root.find('.links a');
    const compareLink = links.get(1);

    expect(compareLink).toContainText('VSM');
    expect(compareLink.href).toEqual(pipelineInstanceJson._links.vsm_url.href);
  });

  it("should render the latest stage", () => {
    expect($root.find('.latest-stage')).toContainText('Failed: up42_stage');
  });

  it("should render stages instance", () => {
    expect($root.find('.stages-instance')).toBeInDOM();
  });
});
