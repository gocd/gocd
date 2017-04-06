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

describe("Read Only Job Tabs Widget", () => {
  const $ = require("jquery");
  const m = require("mithril");
  require('jasmine-jquery');

  const TabsConfigWidget = require("views/pipeline_configs/read_only/tabs_config_widget");
  const Jobs             = require("models/pipeline_configs/jobs");

  let $root, root, jobs;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  describe('Render', () => {
    beforeEach(() => {
      rawJobsJSON[0].tabs = [
        {
          "name": "cobertura",
          "path": "target/site/cobertura/index.html"
        },
        {
          "name": "cobertura2",
          "path": "target/site/cobertura/report.html"
        }
      ];

      jobs = Jobs.fromJSON(rawJobsJSON);
      mount();
    });

    afterEach(() => {
      unmount();
    });

    it('should render Tabs heading', () => {
      expect($('h5')).toContainText('Tabs:');
    });

    it('should render the tabs heading in tabular format', () => {
      const headings = $.find('.read-only-table-header');
      expect(headings).toContainText('name');
      expect(headings).toContainText('path');
    });

    it('should render the tabs', () => {
      const row1 = $.find('.read-only-table-row span');
      expect($(row1[0]).text()).toEqual('cobertura');
      expect($(row1[1]).text()).toEqual('target/site/cobertura/index.html');

      expect($(row1[2]).text()).toEqual('cobertura2');
      expect($(row1[3]).text()).toEqual('target/site/cobertura/report.html');
    });
  });

  describe('Empty Message', () => {
    beforeEach(() => {
      rawJobsJSON[0].tabs = [];
      jobs                = Jobs.fromJSON(rawJobsJSON);
      mount();
    });

    afterEach(() => {
      unmount();
    });

    it('should render empty artifacts message when no artifacts have been specified.', () => {
      expect($root).toContainText('No Tabs have been configured.');
    });
  });

  const mount = function () {
    m.mount(root, {
      view () {
        return m(TabsConfigWidget, {tabs: jobs.firstJob().tabs});
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
      "name": "up42_job",
      "tabs": []
    }
  ];
});
