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
  "jquery", "mithril", "lodash", "views/pipeline_configs/pipeline_stage_field_widget", "models/pipeline_configs/pipelines", "models/pipeline_configs/materials"
], function ($, m, _, PipelineStageFieldWidget, Pipelines, Materials) {
  describe("PipelineStageField Widget", function () {
    var $root = $('#mithril-mount-point'), root = $root.get(0);

    function mount(material, pipelines) {
      m.mount(root,
        m.component(PipelineStageFieldWidget, {material: material, pipelines: pipelines})
      );
      m.redraw(true);
    }

    describe('view', function () {
      it('should render the pipeline stage field', function () {
        var material = Materials.create({
          type:     "dependency",
          pipeline: "p1",
          stage:    "first_stage"
        });

        mount(material, Pipelines);

        expect($root.find("input[name='pipeline-stage']").val()).toBe('p1 [first_stage]');
        expect(_.isEmpty($root.find(".form-error"))).toBe(true);
      });

      it('should render a empty text box in absence of pipeline', function () {
        var material = Materials.create({
          type: "dependency"
        });

        mount(material, Pipelines);

        expect(_.isEmpty($root.find("input[name='pipeline-stage']").val())).toBe(true);
      });

      it('should assign pipeline_stage value to model', function () {
        var material = Materials.create({
          type: "dependency"
        });

        mount(material, Pipelines);

        $root.find("input[name='pipeline-stage']").val('pipeline [stage]');
        $root.find("input[name='pipeline-stage']").blur();
        m.redraw(true);

        expect(material.pipeline()).toBe('pipeline');
        expect(material.stage()).toBe('stage');
      });

      it('should validate the format of pipeline stage string', function () {
        var material = Materials.create({
          type:     "dependency",
          pipeline: "p1",
          stage:    "first_stage"
        });

        mount(material, Pipelines);

        $root.find("input[name='pipeline-stage']").val('invalid-input');
        $root.find("input[name='pipeline-stage']").blur();
        m.redraw(true);

        expect($root.find("input[name='pipeline-stage']").val()).toBe('invalid-input');
        expect($root.find(".form-error").text()).toBe("'invalid-input' should conform to the pattern 'pipeline [stage]'");

        expect(_.isEmpty(material.pipeline())).toBe(true);
        expect(_.isEmpty(material.stage())).toBe(true);
      });

      it('should hide validation errors on providing valid input', function () {
        var material = Materials.create({
          type: "dependency"
        });

        mount(material, Pipelines);

        $root.find("input[name='pipeline-stage']").val('invalid-input');
        $root.find("input[name='pipeline-stage']").blur();
        m.redraw(true);

        expect($root.find(".form-error").text()).toBe("'invalid-input' should conform to the pattern 'pipeline [stage]'");

        $root.find("input[name='pipeline-stage']").val('pipeline [stage]');
        $root.find("input[name='pipeline-stage']").blur();
        m.redraw(true);

        expect($root.find("input[name='pipeline-stage']").val()).toBe('pipeline [stage]');
        expect(_.isEmpty($root.find(".form-error"))).toBe(true);
      });

      it('should show server side validation errors', function () {
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

        expect($root.find(".form-error").text()).toBe("Pipeline with name 'a' does not exist. Stage with name 'b' does not exist");
      });

      it('should clear server side errors on valid input', function () {
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

        expect($root.find(".form-error").text()).toBe("Pipeline with name 'a' does not exist. Stage with name 'b' does not exist");

        $root.find("input[name='pipeline-stage']").val('pipeline [stage]');
        $root.find("input[name='pipeline-stage']").blur();
        m.redraw(true);

        expect($root.find("input[name='pipeline-stage']").val()).toBe('pipeline [stage]');
        expect(_.isEmpty($root.find(".form-error"))).toBe(true);
      });
    });
  });
});
