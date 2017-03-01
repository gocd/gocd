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

var $                       = require("jquery");
var m                       = require("mithril");
var Stream                  = require("mithril/stream");
var PipelineSelectionWidget = require("views/template_configs/pipeline_selection_widget");
var Template                = require("models/template_configs/template");

var nonTemplatePipelines = ["up42"];

describe("PipelineSelectionWidget", function () {
  var root;
  beforeEach(() => {
    [, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);


  describe('Pipeline List', function () {
    beforeEach(function () {
      jasmine.Ajax.install();
      jasmine.Ajax.stubRequest('/go/api/admin/internal/non_template_pipelines', undefined, 'GET').andReturn({
        responseText: JSON.stringify(nonTemplatePipelines),
        status:       200
      });
      m.mount(root, {
        view: function () {
          return m(PipelineSelectionWidget, {template: Stream(Template.fromJSON({}))});
        }
      });

      m.redraw();
    });

    afterEach(function () {
      jasmine.Ajax.uninstall();
      m.mount(root, null);
      m.redraw(true);
    });

    it('should show the select pipeline woidget', function () {
      expect($('.select-pipeline')).toBeInDOM();
    });

    it('should list the pipelines to extract the template', function () {
      expect($('.select-pipeline label')).toContainText("Select a pipeline to extract new Template");
      expect($('.select-pipeline select option').length).toBe(nonTemplatePipelines.length);
      expect($('.select-pipeline select option')).toContainText(nonTemplatePipelines[0]);
    });
  });

  describe('No Pipeline', function () {
    beforeEach(function () {
      jasmine.Ajax.install();
      jasmine.Ajax.stubRequest('/go/api/admin/internal/non_template_pipelines', undefined, 'GET').andReturn({
        responseText: JSON.stringify([]),
        status:       200
      });
      m.mount(root, {
        view: function () {
          return m(PipelineSelectionWidget, {template: Stream(Template.fromJSON({}))});
        }
      });
      m.redraw(true);
    });

    afterEach(function () {
      jasmine.Ajax.uninstall();
      m.mount(root, null);
      m.redraw(true);
    });

    it('should not render select pipeline menu', function () {
      expect($('.select-pipeline label')).not.toBeInDOM()
      expect($('.select-pipeline select')).not.toBeInDOM();
    });

    it('should show the message when no pipelines are available', function () {
      var message = "There are no Pipelines available to extract a new Template.";
      expect($('div.callout.info')).toContainText(message);
    });
  });

});
