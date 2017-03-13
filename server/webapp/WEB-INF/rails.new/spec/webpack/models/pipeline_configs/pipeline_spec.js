/*
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
describe("Pipeline Model", () => {

  var s = require("string-plus");

  var Pipeline     = require("models/pipeline_configs/pipeline");
  var TrackingTool = require('models/pipeline_configs/tracking_tool');

  var pipeline, timer;
  beforeEach(() => {
    timer    = new Pipeline.Timer({spec: "0 0 22 ? * MON-FRI"});
    pipeline = new Pipeline({
      name:                  "BuildRailsApp",
      enablePipelineLocking: true,
      templateName:          "BuildRPM",
      labelTemplate:         "foo-1.0.${COUNT}-${svn}",
      timer,
      environmentVariables:  ["foo=bar", "boo=baz"],
      parameters:            ["WORKING_DIR=something"],
      materials:             ["svn://svn.example.com/svn/myProject"],
      trackingTool:          new TrackingTool.Generic({
        urlPattern: 'http://example.com/bugzilla?id=${ID}',
        regex:      "bug-(\\d+)"
      })
    });
  });

  it("should initialize pipeline model with name", () => {
    expect(pipeline.name()).toBe("BuildRailsApp");
  });

  it("should initialize pipeline model indicating enablePipelineLocking", () => {
    expect(pipeline.enablePipelineLocking()).toBe(true);
  });

  it("should initialize pipeline model with template", () => {
    expect(pipeline.templateName()).toBe("BuildRPM");
  });

  it("should initialize pipeline model with label template", () => {
    expect(pipeline.labelTemplate()).toBe("foo-1.0.${COUNT}-${svn}");
  });

  it("should initialize pipeline model with timer", () => {
    expect(pipeline.timer()).toBe(timer);
  });

  it("should initialize pipeline model with environmentVariables", () => {
    expect(pipeline.environmentVariables()).toEqual(['foo=bar', 'boo=baz']);
  });

  it("should initialize pipeline model with params", () => {
    expect(pipeline.parameters()).toEqual(["WORKING_DIR=something"]);
  });

  it("should initialize pipeline model with materials", () => {
    expect(pipeline.materials()).toEqual(["svn://svn.example.com/svn/myProject"]);
  });

  it("should initialize pipeline model with trackingTool", () => {
    expect(pipeline.trackingTool().type()).toBe('generic');
  });

  it("should default the pipeline auto scheduling to true", () => {
    expect(pipeline.isFirstStageAutoTriggered()).toBe(true);
  });

  it("should default the pipeline scheduling type to the approval type of first stage", () => {
    var pipeline = Pipeline.fromJSON({
      stages: [{
        name:     'sampleStage',
        approval: {
          type: 'manual'
        },
        jobs:     [{
          name: 'sampleJob'
        }]
      }]
    });
    expect(pipeline.isFirstStageAutoTriggered()).toBe(false);
  });

  describe("validations", () => {
    it("should validate presence of labelTemplate", () => {
      pipeline.labelTemplate("");
      var errors = pipeline.validate();
      expect(errors.errors('labelTemplate')).toEqual(['Label template must be present']);
    });

    it("should validate format of labelTemplate", () => {
      pipeline.labelTemplate("foo");
      var errors = pipeline.validate();
      expect(errors.errors('labelTemplate')).toEqual(["Label should be composed of alphanumeric text, it may contain the build number as ${COUNT}, it may contain a material revision as ${<material-name>} or ${<material-name>[:<length>]}, or use params as #{<param-name>}"]);
    });

    describe('validate association', () => {
      it('should validate materials', () => {
        var pipeline = Pipeline.fromJSON(samplePipelineJSON());

        expect(pipeline.isValid()).toBe(true);

        pipeline.materials().firstMaterial().url('');

        expect(pipeline.isValid()).toBe(false);
        expect(pipeline.materials().firstMaterial().errors().errors('url')).toEqual(['URL must be present']);
      });

      it('should validate environmental variables', () => {
        var pipeline = Pipeline.fromJSON(samplePipelineJSON());

        expect(pipeline.isValid()).toBe(true);

        pipeline.environmentVariables().firstVariable().name('');

        expect(pipeline.isValid()).toBe(false);
        expect(pipeline.environmentVariables().firstVariable().errors().errors('name')).toEqual(['Name must be present']);
      });

      it('should validate parameters', () => {
        var pipeline = Pipeline.fromJSON(samplePipelineJSON());

        expect(pipeline.isValid()).toBe(true);

        pipeline.parameters().firstParameter().name('');

        expect(pipeline.isValid()).toBe(false);
        expect(pipeline.parameters().firstParameter().errors().errors('name')).toEqual(['Name must be present']);
      });

      it('should validate tracking tools', () => {
        var pipeline = Pipeline.fromJSON(samplePipelineJSON());

        expect(pipeline.isValid()).toBe(true);

        pipeline.trackingTool().regex('');

        expect(pipeline.isValid()).toBe(false);
        expect(pipeline.trackingTool().errors().errors('regex')).toEqual(['Regex must be present']);
      });

      it('should validate stages', () => {
        var pipeline = Pipeline.fromJSON({
          /* eslint-disable camelcase */
          label_template: "foo-1.0.${COUNT}-${svn}",
          tracking_tool:  {
            type:       "generic",
            attributes: {
              url_pattern: "http://mingle.example.com ${ID}",
              regex:       "my_project"
            }
          },
          stages:         [{name: 'stage1'}]
          /* eslint-enable camelcase */
        });

        expect(pipeline.isValid()).toBe(true);

        pipeline.stages().firstStage().name('');

        expect(pipeline.isValid()).toBe(false);
        expect(pipeline.stages().firstStage().errors().errors('name')).toEqual(['Name must be present']);
      });
    });
  });

  describe("Serialization/De-serialization to/from JSON", () => {
    var pipeline;

    beforeEach(() => {
      pipeline = Pipeline.fromJSON(samplePipelineJSON());
    });

    it("should serialize to JSON", () => {
      var result = JSON.stringify(pipeline, s.snakeCaser);
      expect(JSON.parse(result)).toEqual(samplePipelineJSON());
    });

    it("should de-serialize from JSON", () => {
      var expectedParamNames          = pipeline.parameters().collectParameterProperty('name');
      var expectedEnvironmentVarNames = pipeline.environmentVariables().collectVariableProperty('name');
      var expectedMaterialNames       = pipeline.materials().collectMaterialProperty('name');

      expect(pipeline.name()).toBe("yourproject");
      expect(pipeline.trackingTool().type()).toBe('generic');
      expect(pipeline.labelTemplate()).toBe("foo-1.0.${COUNT}-${svn}");
      expect(pipeline.timer().spec()).toBe("0 0 22 ? * MON-FRI");
      expect(pipeline.timer().onlyOnChanges()).toBe(true);
      expect(expectedParamNames).toEqual(['COMMAND', 'WORKING_DIR']);
      expect(expectedEnvironmentVarNames).toEqual(['MULTIPLE_LINES', 'COMPLEX']);
      expect(expectedMaterialNames).toEqual(['materialA']);
    });
  });

  describe('update', () => {
    it('should patch to pipeline endpoint', () => {
      jasmine.Ajax.withMock(() => {
        let url = '/go/api/admin/pipelines/' + pipeline.name();

        jasmine.Ajax.stubRequest(url, undefined, 'PUT').andReturn({
          responseText:    JSON.stringify(samplePipelineJSON()),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v3+json'
          }
        });

        var successCallback = jasmine.createSpy().and.callFake(response => {
          expect(response.status).toBe(200);
        });

        pipeline.update(url, successCallback);
        expect(successCallback).toHaveBeenCalled();
      });
    });
  });

  function samplePipelineJSON() {
    /* eslint-disable camelcase */
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
          name:          "materialA",
          auto_update:   false,
          filter:        null,
          invert_filter: false,
          destination:   "dest_folder",
          url:           "http://your-svn/",
          username:      "",
          password:      ""
        }
      }],
      tracking_tool:           {
        type:       "generic",
        attributes: {
          url_pattern: "http://mingle.example.com ${ID}",
          regex:       "my_project"
        }
      },
      stages:                  []
    };
    /* eslint-enable camelcase */
  }
});
