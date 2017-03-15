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
      jobs = Jobs.fromJSON(rawJobsJSON);
      mount();
    });

    afterEach(() => {
      unmount();
    });

    it('should render job setting heading', () => {
      expect($('h5')).toContainText('Artifacts:');
    });

    it('should render the artifacts heading in tabular format', () => {
      const headings = $.find('table>tr>th');
      expect(headings[0]).toContainText('source');
      expect(headings[1]).toContainText('destination');
      expect(headings[2]).toContainText('type');
    });

    it('should render the artifacts', () => {
      const row1 = $($.find('table>tr')[1]).children();
      expect($(row1[0])).toContainText('target');
      expect($(row1[1])).toContainText('result');
      expect($(row1[2])).toContainText('build');

      const row2 = $($.find('table>tr')[2]).children();
      expect($(row2[0])).toContainText('test');
      expect($(row2[1])).toContainText('res1');
      expect($(row2[2])).toContainText('test');
    });
  });

  describe('Empty Message', () => {
    beforeEach(() => {
      rawJobsJSON[0].artifacts = [];
      jobs                  = Jobs.fromJSON(rawJobsJSON);
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
      "artifacts": [
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
      ]
    }
  ];
});
