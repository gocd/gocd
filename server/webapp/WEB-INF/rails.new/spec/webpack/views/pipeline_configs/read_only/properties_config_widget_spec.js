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

describe("Read Only Job Properties Config Widget", () => {
  const $ = require("jquery");
  const m = require("mithril");
  require('jasmine-jquery');

  const JobPropertiesWidget = require("views/pipeline_configs/read_only/properties_config_widget");
  const Jobs                = require("models/pipeline_configs/jobs");

  let $root, root, jobs;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  describe('Render', () => {
    beforeEach(() => {
      rawJobsJSON[0].properties = [
        {
          "name":   "coverage.class",
          "source": "target/emma/coverage.xml",
          "xpath":  "substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')"
        }
      ];

      jobs = Jobs.fromJSON(rawJobsJSON);
      mount();
    });

    afterEach(() => {
      unmount();
    });

    it('should render Properties heading', () => {
      expect($('h5')).toContainText('Properties:');
    });

    it('should render the properties heading in tabular format', () => {
      const headings = $.find('table>tr>th');
      expect(headings[0]).toContainText('name');
      expect(headings[1]).toContainText('source');
      expect(headings[2]).toContainText('xpath');
    });

    it('should render the properties', () => {
      const row1 = $($.find('table>tr')[1]).children();
      expect($(row1[0])).toContainText('coverage.class');
      expect($(row1[1])).toContainText('target/emma/coverage.xml');
      expect($(row1[2])).toContainText("substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')");
    });
  });

  describe('Empty Message', () => {
    beforeEach(() => {
      rawJobsJSON[0].properties = [];
      jobs                      = Jobs.fromJSON(rawJobsJSON);
      mount();
    });

    afterEach(() => {
      unmount();
    });

    it('should render empty properties message when no properties have been specified.', () => {
      expect($root).toContainText('No Properties have been configured.');
    });
  });

  const mount = function () {
    m.mount(root, {
      view () {
        return m(JobPropertiesWidget, {properties: jobs.firstJob().properties});
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
      "name":       "up42_job",
      "properties": []
    }
  ];
});
