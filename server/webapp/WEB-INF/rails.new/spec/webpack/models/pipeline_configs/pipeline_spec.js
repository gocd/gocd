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

describe("Pipeline Model", () => {

  const s = require("string-plus");

  const Pipeline     = require("models/pipeline_configs/pipeline");
  const TrackingTool = require('models/pipeline_configs/tracking_tool');

  let pipeline, timer;

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
    const pipeline = Pipeline.fromJSON({
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
      const errors = pipeline.validate();
      expect(errors.errors('labelTemplate')).toEqual(['Label template must be present']);
    });

    it("should validate format of labelTemplate", () => {
      pipeline.labelTemplate("foo");
      const errors = pipeline.validate();
      expect(errors.errors('labelTemplate')).toEqual(["Label should be composed of alphanumeric text, it may contain the build number as ${COUNT}, it may contain a material revision as ${<material-name>} or ${<material-name>[:<length>]}, or use params as #{<param-name>}"]);
    });

    describe('validate association', () => {
      it('should validate materials', () => {
        const pipeline = Pipeline.fromJSON(samplePipelineJSON());

        expect(pipeline.isValid()).toBe(true);

        pipeline.materials().firstMaterial().url('');

        expect(pipeline.isValid()).toBe(false);
        expect(pipeline.materials().firstMaterial().errors().errors('url')).toEqual(['URL must be present']);
      });

      it('should validate environmental variables', () => {
        const pipeline = Pipeline.fromJSON(samplePipelineJSON());

        expect(pipeline.isValid()).toBe(true);

        pipeline.environmentVariables().firstVariable().name('');

        expect(pipeline.isValid()).toBe(false);
        expect(pipeline.environmentVariables().firstVariable().errors().errors('name')).toEqual(['Name must be present']);
      });

      it('should validate parameters', () => {
        const pipeline = Pipeline.fromJSON(samplePipelineJSON());

        expect(pipeline.isValid()).toBe(true);

        pipeline.parameters().firstParameter().name('');

        expect(pipeline.isValid()).toBe(false);
        expect(pipeline.parameters().firstParameter().errors().errors('name')).toEqual(['Name must be present']);
      });

      it('should validate tracking tools', () => {
        const pipeline = Pipeline.fromJSON(samplePipelineJSON());

        expect(pipeline.isValid()).toBe(true);

        pipeline.trackingTool().regex('');

        expect(pipeline.isValid()).toBe(false);
        expect(pipeline.trackingTool().errors().errors('regex')).toEqual(['Regex must be present']);
      });

      it('should validate stages', () => {
        const pipeline = Pipeline.fromJSON({
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
    let pipeline;

    beforeEach(() => {
      pipeline = Pipeline.fromJSON(samplePipelineJSON());
    });

    it("should serialize to JSON", () => {
      const result = JSON.stringify(pipeline, s.snakeCaser);
      expect(JSON.parse(result)).toEqual(samplePipelineJSON());
    });

    it("should de-serialize from JSON", () => {
      const expectedParamNames          = pipeline.parameters().collectParameterProperty('name');
      const expectedEnvironmentVarNames = pipeline.environmentVariables().collectVariableProperty('name');
      const expectedMaterialNames       = pipeline.materials().collectMaterialProperty('name');

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
        const url = `/go/api/admin/pipelines/${pipeline.name()}`;

        jasmine.Ajax.stubRequest(url, undefined, 'PUT').andReturn({
          responseText:    JSON.stringify(samplePipelineJSON()),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v3+json'
          }
        });

        const successCallback = jasmine.createSpy().and.callFake(({status}) => {
          expect(status).toBe(200);
        });

        pipeline.update(url, successCallback);
        expect(successCallback).toHaveBeenCalled();
      });
    });
  });

  describe('Pipeline VM', () => {
    let vm;

    beforeEach(() => {
      vm = new Pipeline.vm();
    });

    it('should initalize save state to empty', () => {
      expect(vm.saveState()).toBe('');
      expect(vm.pageSaveSpinner()).toBe('');
      expect(vm.pageSaveState()).toBe('');
    });

    it('should change the page state while updating', () => {
      vm.updating();

      expect(vm.saveState()).toBe('in-progress disabled');
      expect(vm.pageSaveSpinner()).toBe('page-spinner');
      expect(vm.pageSaveState()).toBe('page-save-in-progress');
    });

    it('should change the page status to sucess when updating page results in sucess', () => {
      vm.saveSuccess();

      expect(vm.saveState()).toBe('success');
      expect(vm.pageSaveSpinner()).toBe('');
      expect(vm.pageSaveState()).toBe('');
    });

    it('should change the page status to failure when updating page results in failure', () => {
      vm.saveFailed({});

      expect(vm.saveState()).toBe('alert');
      expect(vm.pageSaveSpinner()).toBe('');
      expect(vm.pageSaveState()).toBe('');
    });

    it('should populate errors', () => {
      expect(vm.errors()).toEqual([]);
      const failureMessage = 'Save failed!';
      const entityError    = 'This is an entity related error!';
      vm.saveFailed({
        message: failureMessage,
        data:    {
          errors: [entityError]
        }
      });

      expect(vm.errors()).toEqual([failureMessage, entityError]);
    });

    it('should reset the page to default state', () => {
      vm.saveState('success');
      expect(vm.saveState()).toBe('success');
      vm.defaultState();
      expect(vm.saveState()).toBe('');
    });

    it('should clear all errors', () => {
      const err = 'Save failed!';
      vm.saveFailed({message: err});
      expect(vm.errors()).toEqual([err]);
      vm.clearErrors();
      expect(vm.errors()).toEqual([]);
    });

    it('should tell whether there are errors', () => {
      expect(vm.hasErrors()).toBe(false);

      const err = 'Save failed!';
      vm.saveFailed({message: err});
      expect(vm.errors()).toEqual([err]);
      expect(vm.hasErrors()).toBe(true);
    });

    it('should populate generic global error while populating client side errors', () => {
      expect(vm.hasErrors()).toBe(false);
      vm.markClientSideErrors();
      expect(vm.hasErrors()).toBe(true);
      expect(vm.errors()[0]).toEqual('There are errors on the page, fix them and save');
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
