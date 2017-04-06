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

describe("Read Only Job Artifacts Widget", () => {
  const $ = require("jquery");
  const m = require("mithril");
  require('jasmine-jquery');

  const JobArtifactsWidget = require("views/pipeline_configs/read_only/artifacts_widget");
  const Jobs               = require("models/pipeline_configs/jobs");

  let $root, root, jobs;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  describe('Render', () => {
    beforeEach(() => {
      rawJobsJSON[0].artifacts = [
        {
          "source":      "target",
          "destination": "result",
          "type":        "build"
        },
        {
          "source":      "test",
          "destination": "res1",
          "type":        "test"
        }
      ];

      jobs = Jobs.fromJSON(rawJobsJSON);
      mount();
    });

    afterEach(() => {
      unmount();
    });

    it('should render Artifacts heading', () => {
      expect($('h5')).toContainText('Artifacts:');
    });

    it('should render the artifacts heading in tabular format', () => {
      const headings = $.find('.read-only-table-header');
      expect(headings).toContainText('source');
      expect(headings).toContainText('destination');
      expect(headings).toContainText('type');
    });

    it('should render the artifacts', () => {
      const content = $.find('.read-only-table-row span');
      expect($(content[0]).text()).toBe('target');
      expect($(content[1]).text()).toBe('result');
      expect($(content[2]).text()).toBe('build');

      expect($(content[3]).text()).toBe('test');
      expect($(content[4]).text()).toBe('res1');
      expect($(content[5]).text()).toBe('test');
    });
  });

  describe('Empty Message', () => {
    beforeEach(() => {
      rawJobsJSON[0].artifacts = [];
      jobs                     = Jobs.fromJSON(rawJobsJSON);
      mount();
    });

    afterEach(() => {
      unmount();
    });

    it('should render empty artifacts message when no artifacts have been specified.', () => {
      expect($root).toContainText('No Artifacts have been configured.');
    });
  });

  const mount = function () {
    m.mount(root, {
      view () {
        return m(JobArtifactsWidget, {artifacts: jobs.firstJob().artifacts});
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
      "name":      "up42_job",
      "artifacts": []
    }
  ];
});
