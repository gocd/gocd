/*
 * Copyright 2015 ThoughtWorks, Inc.
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

define(['lodash', "pipeline_configs/models/pipeline", "string-plus"], function (_, Pipeline, s) {
  describe("Pipeline Model", function () {
    var pipeline, timer;
    beforeEach(function () {
      timer    = new Pipeline.Timer({spec: "0 0 22 ? * MON-FRI"});
      pipeline = new Pipeline({
        name:                  "BuildRailsApp",
        enablePipelineLocking: true,
        templateName:          "BuildRPM",
        labelTemplate:         "foo-1.0.${COUNT}-${svn}",
        timer:                 timer,
        environmentVariables:  ["foo=bar", "boo=baz"],
        parameters:            ["WORKING_DIR=something"],
        materials:             ["svn://svn.example.com/svn/myProject"],
        trackingTool:          "mingle"
      });
    });

    it("should initialize pipeline model with name", function () {
      expect(pipeline.name()).toBe("BuildRailsApp");
    });

    it("should initialize pipeline model indicating enablePipelineLocking", function () {
      expect(pipeline.enablePipelineLocking()).toBe(true);
    });

    it("should initialize pipeline model with template", function () {
      expect(pipeline.templateName()).toBe("BuildRPM");
    });

    it("should initialize pipeline model with label template", function () {
      expect(pipeline.labelTemplate()).toBe("foo-1.0.${COUNT}-${svn}");
    });

    it("should initialize pipeline model with timer", function () {
      expect(pipeline.timer()).toBe(timer);
    });

    it("should initialize pipeline model with environmentVariables", function () {
      expect(pipeline.environmentVariables()).toEqual(['foo=bar', 'boo=baz']);
    });

    it("should initialize pipeline model with params", function () {
      expect(pipeline.parameters()).toEqual(["WORKING_DIR=something"]);
    });

    it("should initialize pipeline model with materials", function () {
      expect(pipeline.materials()).toEqual(["svn://svn.example.com/svn/myProject"]);
    });

    it("should initialize pipeline model with trackingTool", function () {
      expect(pipeline.trackingTool()).toBe('mingle');
    });

    describe("validations", function () {
      it("should validate presence of labelTemplate", function () {
        pipeline.labelTemplate("");
        var errors = pipeline.validate();
        expect(errors.errors('labelTemplate')).toEqual(['Label template must be present']);
      });

      it("should validate format of labelTemplate", function () {
        pipeline.labelTemplate("foo");
        var errors = pipeline.validate();
        expect(errors.errors('labelTemplate')).toEqual(["Label should be composed of alphanumeric text, it may contain the build number as ${COUNT}, it may contain a material revision as ${<material-name>} or ${<material-name>[:<length>]}, or use params as #{<param-name>}"]);
      });
    });

    describe("Serialization/De-serialization to/from JSON", function () {
      var pipeline;

      beforeEach(function () {
        pipeline = Pipeline.fromJSON(samplePipelineJSON());
      });

      it("should serialize to JSON", function () {
        var result = JSON.stringify(pipeline, s.snakeCaser);
        expect(JSON.parse(result)).toEqual(samplePipelineJSON());
      });

      it("should de-serialize from JSON", function () {
        var expectedParamNames          = pipeline.parameters().collectParameterProperty('name');
        var expectedEnvironmentVarNames = pipeline.environmentVariables().collectVariableProperty('name');
        var expectedMaterialNames       = pipeline.materials().collectMaterialProperty('name');

        expect(pipeline.name()).toBe("yourproject");
        expect(pipeline.trackingTool().type()).toBe('mingle');
        expect(pipeline.labelTemplate()).toBe("foo-1.0.${COUNT}-${svn}");
        expect(pipeline.timer().spec()).toBe("0 0 22 ? * MON-FRI");
        expect(pipeline.timer().onlyOnChanges()).toBe(true);
        expect(expectedParamNames).toEqual(['COMMAND', 'WORKING_DIR']);
        expect(expectedEnvironmentVarNames).toEqual(['MULTIPLE_LINES', 'COMPLEX']);
        expect(expectedMaterialNames).toEqual(['materialA']);
      });

      function samplePipelineJSON() {
        return {
          name:                    "yourproject",
          enable_pipeline_locking: true,
          template_name:           "",
          label_template:          "foo-1.0.${COUNT}-${svn}",
          timer:                   {
            spec:            "0 0 22 ? * MON-FRI",
            only_on_changes: true
          },
          environment_variables:   [
            {
              name:   "MULTIPLE_LINES",
              value:  "multiplelines",
              secure: false
            }, {
              name:   "COMPLEX",
              value:  "This has very <complex> data",
              secure: !1
            }
          ],
          parameters:              [
            {
              name:  "COMMAND",
              value: "echo"
            },
            {
              name:  "WORKING_DIR",
              value: "/repo/branch"
            }
          ],
          materials:               [{
            type:       "svn",
            attributes: {
              name:        "materialA",
              auto_update: false,
              "filter":    {
                "ignore": []
              },
              destination: "dest_folder",
              url:         "http://your-svn/",
              username:    "",
              password:    ""
            }
          }],
          tracking_tool:           {
            type:       "mingle",
            attributes: {
              base_url:            "http://mingle.example.com",
              project_identifier:  "my_project",
              grouping_conditions: "status > 'In Dev'"
            }
          },
          stages:                  []
        };
      }
    });
  });
});
