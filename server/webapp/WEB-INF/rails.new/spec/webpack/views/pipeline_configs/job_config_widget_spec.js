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
describe("JobsConfig Widget", () => {

  const m      = require('mithril');
  const Stream = require('mithril/stream');

  require('jasmine-jquery');

  const JobsConfigWidget = require("views/pipeline_configs/jobs_config_widget");
  const Jobs             = require("models/pipeline_configs/jobs");
  const PluginInfos      = require("models/shared/plugin_infos");
  const ElasticProfiles  = require('models/elastic_profiles/elastic_profiles');

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);
  let jobs;
  let elasticProfiles;

  beforeEach(() => {
    jobs            = Stream(Jobs.fromJSON(data.jobs));
    elasticProfiles = Stream(new ElasticProfiles());
  });

  afterEach(() => {
    unmount();
  });

  it('should disable elastic profile id text box if job is set to run on all agents', () => {
    mount(jobs);
    viewJob();
    const elasticProfileInputBox = $root.find("input[data-prop-name='elasticProfileId']");
    expect(elasticProfileInputBox).toBeDisabled();
  });

  it('should disable checkbox for run on all agents if job is set to elastic agent profile', () => {
    jobs().firstJob().elasticProfileId('docker-test');
    mount(jobs);
    viewJob();
    const checkbox = $root.find("input[type=radio]")[4];
    expect(checkbox).toBeDisabled();
  });

  function viewJob() {
    $root.find(".job-definition .accordion-item>a")[0].click();
    m.redraw();
  }

  const unmount = () => {
    m.mount(root, null);
    m.redraw();
  };

  const mount = () => {
    m.mount(root, {
      view() {
        return m(JobsConfigWidget, {
          jobs,
          key:         jobs().uuid(),
          elasticProfiles,
          pluginInfos: Stream(PluginInfos.fromJSON([taskPlugin]))
        });
      }
    });
    m.redraw();
  };


  const taskPlugin = {
    "id":            "script-executor",
    "version":       "1",
    "type":          "task",
    "status": {
      "state": "active"
    },
    "about":         {
      "name":                     "Script Executor",
      "version":                  "0.3.0",
      "target_go_version":        "16.1.0",
      "description":              "Thoughtworks Go plugin to run scripts",
      "target_operating_systems": [],
      "vendor":                   {
        "name": "Srinivas Upadhya",
        "url":  "https://github.com/srinivasupadhya"
      }
    },
    "display_name":  "Script Executor",
    "task_settings": {
      "configurations": [{"key": "script", "metadata": {"secure": false, "required": true}},
        {"key": "shtype", "metadata": {"secure": false, "required": true}}
      ],
      "view":           {"template": "<div />"}
    }
  };

  const data = {
    "jobs": [
      {
        "name":                  "up42_job",
        "run_instance_count":    "all",
        "timeout":               null,
        "environment_variables": [],
        "resources":             [],
        "tasks":                 [
          {
            "type":       "exec",
            "attributes": {
              "run_if":            [],
              "on_cancel":         null,
              "command":           "ls",
              "working_directory": null
            }
          }
        ],
        "tabs":                  [],
        "artifacts":             [],
        "properties":            null
      }
    ]
  };
});
