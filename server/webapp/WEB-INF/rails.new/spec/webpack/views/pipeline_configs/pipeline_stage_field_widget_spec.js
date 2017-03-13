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

describe("PipelineStageField Widget", () => {

  var m = require("mithril");
  var _ = require("lodash");

  require('jasmine-jquery');

  var PipelineStageFieldWidget = require("views/pipeline_configs/pipeline_stage_field_widget");
  var Pipelines                = require("models/pipeline_configs/pipelines");
  var Materials                = require("models/pipeline_configs/materials");

  var $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  function mount(material, pipelines) {
    m.mount(root, {
      view() {
        return m(PipelineStageFieldWidget, {
          material,
          pipelines
        });
      }
    });
    m.redraw();
  }

  var unmount = () => {
    m.mount(root, null);
    m.redraw();
  };

  describe('view', () => {
    afterEach(() => {
      unmount();
    });

    it('should render the pipeline stage field', () => {
      var material = Materials.create({
        type:     "dependency",
        pipeline: "p1",
        stage:    "first_stage"
      });

      mount(material, Pipelines);

      expect($root.find("input[name='pipeline-stage']")).toHaveValue('p1 [first_stage]');
      expect('.form-error').not.toBeInDOM();
    });

    it('should render a empty text box in absence of pipeline', () => {
      var material = Materials.create({
        type: "dependency"
      });

      mount(material, Pipelines);

      expect($root.find("input[name='pipeline-stage']")).toHaveValue('');
    });

    it('should assign pipeline_stage value to model', () => {
      var material = Materials.create({
        type: "dependency"
      });

      mount(material, Pipelines);

      $root.find("input[name='pipeline-stage']").val('pipeline [stage]');
      $root.find("input[name='pipeline-stage']").blur();
      m.redraw();

      expect(material.pipeline()).toBe('pipeline');
      expect(material.stage()).toBe('stage');
    });

    it('should validate the format of pipeline stage string', () => {
      var material = Materials.create({
        type:     "dependency",
        pipeline: "p1",
        stage:    "first_stage"
      });

      mount(material, Pipelines);

      $root.find("input[name='pipeline-stage']").val('invalid-input');
      $root.find("input[name='pipeline-stage']").blur();
      m.redraw();

      expect($root.find("input[name='pipeline-stage']")).toHaveValue('invalid-input');
      expect($root.find(".form-error")).toHaveText("'invalid-input' should conform to the pattern 'pipeline [stage]'");

      expect(_.isEmpty(material.pipeline())).toBe(true);
      expect(_.isEmpty(material.stage())).toBe(true);
    });

    it('should hide validation errors on providing valid input', () => {
      var material = Materials.create({
        type: "dependency"
      });

      mount(material, Pipelines);

      $root.find("input[name='pipeline-stage']").val('invalid-input');
      $root.find("input[name='pipeline-stage']").blur();
      m.redraw();

      expect($root.find(".form-error")).toHaveText("'invalid-input' should conform to the pattern 'pipeline [stage]'");

      $root.find("input[name='pipeline-stage']").val('pipeline [stage]');
      $root.find("input[name='pipeline-stage']").blur();
      m.redraw();

      expect($root.find("input[name='pipeline-stage']")).toHaveValue('pipeline [stage]');
      expect(_.isEmpty($root.find(".form-error"))).toBe(true);
    });

    it('should show server side validation errors', () => {
      var material = Materials.create({
        type:     "dependency",
        pipeline: "a",
        stage:    "b",
        errors:   {
          pipeline: ["Pipeline with name 'a' does not exist"],
          stage:    ["Stage with name 'b' does not exist"]
        }
      });

      mount(material, Pipelines);

      $root.find("input[name='pipeline-stage']").val('invalid-input');

      expect($root.find(".form-error")).toHaveText("Pipeline with name 'a' does not exist. Stage with name 'b' does not exist");
    });

    it('should clear server side errors on valid input', () => {
      var material = Materials.create({
        type:     "dependency",
        pipeline: "a",
        stage:    "b",
        errors:   {
          pipeline: ["Pipeline with name 'a' does not exist"],
          stage:    ["Stage with name 'b' does not exist"]
        }
      });

      mount(material, Pipelines);

      $root.find("input[name='pipeline-stage']").val('invalid-input');

      expect($root.find(".form-error")).toHaveText("Pipeline with name 'a' does not exist. Stage with name 'b' does not exist");

      $root.find("input[name='pipeline-stage']").val('pipeline [stage]');
      $root.find("input[name='pipeline-stage']").blur();
      m.redraw();

      expect($root.find("input[name='pipeline-stage']")).toHaveValue('pipeline [stage]');
      expect(_.isEmpty($root.find(".form-error"))).toBe(true);
    });
  });
});
