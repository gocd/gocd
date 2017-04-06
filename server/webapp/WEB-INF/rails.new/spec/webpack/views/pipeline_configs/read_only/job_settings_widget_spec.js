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

describe("Read Only Job Settings Widget", () => {
  const $      = require("jquery");
  const m      = require("mithril");
  const Stream = require("mithril/stream");
  require('jasmine-jquery');

  const JobSettingsWidget = require("views/pipeline_configs/read_only/job_settings_widget");
  const Jobs              = require("models/pipeline_configs/jobs");

  let $root, root, jobs;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  beforeEach(() => {
    jobs = Jobs.fromJSON(rawJobsJSON);
    mount();
  });

  afterEach(() => {
    unmount();
  });

  it('should render job setting heading', () => {
    expect($('h5')).toContainText('Job Settings:');
  });

  it('should render job name', () => {
    expect($root).toContainText('Job Name');
    expect($root).toContainText(jobs.firstJob().name());
  });

  describe('Resources', () => {
    afterEach(() => {
      rawJobsJSON[0].resources = [];
    });

    it('should render job resources', () => {
      const resources          = ['Linux', 'Chrome'];
      rawJobsJSON[0].resources = resources;
      jobs                     = Jobs.fromJSON(rawJobsJSON);
      mount();

      expect($root).toContainText('Resources');
      expect($root).toContainText(resources.join(', '));
    });

    it('should render not specified message when no resources are specified', () => {
      jobs = Jobs.fromJSON(rawJobsJSON);
      mount();

      expect($root).toContainText('Resources');
      expect($root).toContainText('Not Specified');
    });
  });

  describe('Elastic Profile ID', () => {
    afterEach(() => {
      rawJobsJSON[0]["elastic_profile_id"] = null;
    });
    it('should render elastic profile id related with job', () => {
      const id = 'randomId-123';

      rawJobsJSON[0]["elastic_profile_id"] = id;
      jobs                                 = Jobs.fromJSON(rawJobsJSON);

      mount();

      expect($root).toContainText('Elastic Profile ID');
      expect($root).toContainText(id);
    });

    it('should render not specified message when no elastic profile id is specified', () => {
      jobs = Jobs.fromJSON(rawJobsJSON);
      mount();
      expect($root).toContainText('Elastic Profile ID');
      expect($root).toContainText('Not Specified');
    });
  });

  describe('Timeout', () => {
    afterEach(() => {
      rawJobsJSON[0]["timeout"] = null;
    });
    it('should render job timeout', () => {
      const timeout = 'never';

      rawJobsJSON[0]["timeout"] = timeout;
      jobs                      = Jobs.fromJSON(rawJobsJSON);

      mount();

      expect($root).toContainText('Timeout');
      expect($root).toContainText(timeout);
    });

    it('should render defauult timeout when not specified', () => {
      jobs = Jobs.fromJSON(rawJobsJSON);
      mount();
      expect($root).toContainText('Timeout');
      expect($root).toContainText('Default (Never)');
    });

    it('should render cancel after timeout', () => {
      const timeout = 100;

      rawJobsJSON[0]["timeout"] = timeout;
      jobs                      = Jobs.fromJSON(rawJobsJSON);

      mount();

      expect($root).toContainText('Timeout');
      expect($root).toContainText(`Cancel after ${timeout} minute(s) of inactivity`);
    });
  });

  describe('Run instances', () => {
    afterEach(() => {
      rawJobsJSON[0]["run_instance_count"] = null;
    });

    it('should render job running on one agent when no job run instance count is provided', () => {
      const runInstance = 'Run on one agent';

      jobs = Jobs.fromJSON(rawJobsJSON);
      mount();

      expect($root).toContainText('Run Type');
      expect($root).toContainText(runInstance);
    });

    it('should render job running on all agents', () => {
      const runInstance                    = 'Run on all agents';
      rawJobsJSON[0]["run_instance_count"] = 'all';
      jobs                                 = Jobs.fromJSON(rawJobsJSON);
      mount();

      expect($root).toContainText('Run Type');
      expect($root).toContainText(runInstance);
    });

    it('should render specified job instances', () => {
      const runInstanceCount               = 100;
      const runInstance                    = `Run ${runInstanceCount} instances of job`;
      rawJobsJSON[0]["run_instance_count"] = runInstanceCount;
      jobs                                 = Jobs.fromJSON(rawJobsJSON);
      mount();

      expect($root).toContainText('Run Type');
      expect($root).toContainText(runInstance);
    });

  });

  const mount = function () {
    m.mount(root, {
      view () {
        return m(JobSettingsWidget, {job: Stream(jobs.firstJob())});
      }
    });
    m.redraw();
  };

  const unmount = function () {
    m.mount(root, null);
    m.redraw();
  };

  const rawJobsJSON = [
    {
      "name":               "up42_job",
      "run_instance_count": null,
      "timeout":            null,
      "elastic_profile_id": null,
      "resources":          [],
    }
  ];
});
