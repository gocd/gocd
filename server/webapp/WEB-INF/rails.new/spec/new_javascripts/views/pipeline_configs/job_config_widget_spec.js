/*
 * Copyright 2016 ThoughtWorks, Inc.
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

define([
  "jquery", "mithril", "views/pipeline_configs/jobs_config_widget", "models/pipeline_configs/jobs", 'models/elastic_profiles/elastic_profiles'
], function ($, m, JobsConfigWidget, Jobs, ElasticProfiles) {
  describe("JobsConfig Widget", function () {
    var $root = $('#mithril-mount-point'), root = $root.get(0);
    var jobs;
    var elasticProfiles;

    beforeEach(function () {
      jobs = m.prop(Jobs.fromJSON(data.jobs));
      elasticProfiles = m.prop(new ElasticProfiles());
    });

    afterEach(function () {
      unmount();
    });

    it('should disable elastic profile id text box if job is set to run on all agents', function () {
      mount(jobs);
      viewJob();
      var elasticProfileInputBox = $root.find("input[data-prop-name='elasticProfileId']");
      expect(elasticProfileInputBox).toBeDisabled();
    });

    it('should disable checkbox for run on all agents if job is set to elastic agent profile', function () {
      jobs().firstJob().elasticProfileId('docker-test');
      mount(jobs);
      viewJob();
      var checkbox = $root.find("input[type=radio]")[4];
      expect(checkbox).toBeDisabled();
    });

    function viewJob() {
      $root.find(".job-definition .accordion-item>a")[0].click();
      m.redraw(true);
    }

    var unmount = function () {
      m.mount(root, null);
      m.redraw(true);
    };

    function mount() {
      m.mount(root,
        m.component(JobsConfigWidget, {jobs: jobs, key: jobs().uuid(), elasticProfiles: elasticProfiles})
      );
      m.redraw(true);
    }

    var data = {
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
});
