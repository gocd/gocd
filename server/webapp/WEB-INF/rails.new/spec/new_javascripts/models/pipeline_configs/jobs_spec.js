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

define(['lodash', "models/pipeline_configs/jobs", "string-plus"], function (_, Jobs, s) {
  var jobs, job;
  beforeEach(function () {
    jobs = new Jobs();
    job  = jobs.createJob({
      name:                 "UnitTest",
      runInstanceCount:     10,
      timeout:              10,
      environmentVariables: ["foo=bar", "boo=baz"],
      resources:            ['java', 'firefox'],
      tasks:                ['ant', 'mvn'],
      artifacts:            ['pkgs', 'logs'],
      tabs:                 ['twist', 'junit'],
      elasticProfileId:     'docker.unit-test'
    });
  });

  describe("Job Model", function () {
    it("should initialize job model with name", function () {
      expect(job.name()).toBe("UnitTest");
    });

    it("should initialize job model with runInstanceCount", function () {
      expect(job.runInstanceCount()).toBe(10);
    });

    it("should initialize job model with timeout", function () {
      expect(job.timeout()).toBe(10);
    });

    it("should initialize job model with environmentVariables", function () {
      expect(job.environmentVariables()).toEqual(['foo=bar', 'boo=baz']);
    });

    it("should initialize job model with resources", function () {
      expect(job.resources()).toEqual(['java', 'firefox']);
    });

    it("should initialize job model with tasks", function () {
      expect(job.tasks()).toEqual(['ant', 'mvn']);
    });

    it("should initialize job model with artifacts", function () {
      expect(job.artifacts()).toEqual(['pkgs', 'logs']);
    });

    it("should initialize job model with tabs", function () {
      expect(job.tabs()).toEqual(['twist', 'junit']);
    });

    it("should initialize job model with elasticProfileId", function () {
      expect(job.elasticProfileId()).toEqual('docker.unit-test');
    });

    describe("validations", function () {
      it("should not allow blank job names", function () {
        var errors = job.validate();
        expect(errors._isEmpty()).toBe(true);

        job.name("");
        var duplicateJob = jobs.createJob({name: job.name()});

        errors                = job.validate();
        var errorsOnDuplicate = duplicateJob.validate();

        expect(errors.errors('name')).toEqual(['Name must be present']);
        expect(errorsOnDuplicate.errors('name')).toEqual(['Name must be present']);
      });

      it("should not allow jobs with duplicate names", function () {
        var errorsOnOriginal = job.validate();
        expect(errorsOnOriginal._isEmpty()).toBe(true);

        var duplicateVariable = jobs.createJob({
          name: "UnitTest"
        });

        errorsOnOriginal = job.validate();
        expect(errorsOnOriginal.errors('name')).toEqual(['Name is a duplicate']);

        var errorsOnDuplicate = duplicateVariable.validate();
        expect(errorsOnDuplicate.errors('name')).toEqual(['Name is a duplicate']);
      });

      describe('validate associations', function () {
        it('should validate environmental variables', function () {
          var job = Jobs.Job.fromJSON(sampleJobJSON());

          expect(job.isValid()).toBe(true);

          job.environmentVariables().firstVariable().name('');

          expect(job.isValid()).toBe(false);
          expect(job.environmentVariables().firstVariable().errors().errors('name')).toEqual(['Name must be present']);
        });

        it('should validate tasks', function () {
          var job = Jobs.Job.fromJSON({
            name: "UnitTest",
            tasks: [
              {
                type: "exec",
                attributes: {
                  command: 'bash'
                }
              }
            ]
          });

          expect(job.isValid()).toBe(true);

          job.tasks().firstTask().command('');

          expect(job.isValid()).toBe(false);
          expect(job.tasks().firstTask().errors().errors('command')).toEqual(['Command must be present']);
        });
      });

      it('should validate artifacts', function () {
        var job = Jobs.Job.fromJSON(sampleJobJSON());

        expect(job.isValid()).toBe(true);

        job.artifacts().firstArtifact().source('');

        expect(job.isValid()).toBe(false);
        expect(job.artifacts().firstArtifact().errors().errors('source')).toEqual(['Source must be present']);
      });

      it('should validate tabs', function () {
        var job = Jobs.Job.fromJSON(sampleJobJSON());

        expect(job.isValid()).toBe(true);

        job.tabs().firstTab().name('');

        expect(job.isValid()).toBe(false);
        expect(job.tabs().firstTab().errors().errors('name')).toEqual(['Name must be present']);
      });

      it('should validate properties', function () {
        var job = Jobs.Job.fromJSON(sampleJobJSON());

        expect(job.isValid()).toBe(true);

        job.properties().firstProperty().name('');

        expect(job.isValid()).toBe(false);
        expect(job.properties().firstProperty().errors().errors('name')).toEqual(['Name must be present']);
      });
    });

    describe("Serialization from/to JSON", function () {
      beforeEach(function () {
        job = Jobs.Job.fromJSON(sampleJobJSON());
      });

      it("should de-serialize from JSON", function () {
        expect(job.name()).toBe("UnitTest");
        expect(job.runInstanceCount()).toBe(10);
        expect(job.timeout()).toBe(20);
        expect(job.resources()).toEqual(['jdk5', 'tomcat']);

        var expectedEnvironmentVarNames = job.environmentVariables().collectVariableProperty('name');
        var expectedTasks               = job.tasks().collectTaskProperty('type');
        var expectedArtifacts           = job.artifacts().collectArtifactProperty('source');
        var expectedTabs                = job.tabs().collectTabProperty('name');
        var expectedProperties          = job.properties().collectPropertyProperty('name');

        expect(expectedEnvironmentVarNames).toEqual(['MULTIPLE_LINES', 'COMPLEX']);
        expect(expectedTasks).toEqual(['ant']);
        expect(expectedArtifacts).toEqual(['target/dist.jar']);
        expect(expectedTabs).toEqual(['coverage']);
        expect(expectedProperties).toEqual(['coverage.class']);
      });

      it("should serialize to JSON", function () {
        expect(JSON.parse(JSON.stringify(job, s.snakeCaser))).toEqual(sampleJobJSON());
      });

      it("should serialize a comma separated resource string to array", function () {
        job.resources('foo,bar,baz');
        expect(JSON.parse(JSON.stringify(job, s.snakeCaser))['resources']).toEqual(['foo', 'bar', 'baz']);
      });

      it("should validate timeout", function () {
        job.timeout('never');
        expect(job.validate().hasErrors()).toBe(false);

        job.timeout(null);
        expect(job.validate().hasErrors()).toBe(false);

        job.timeout(undefined);
        expect(job.validate().hasErrors()).toBe(false);

        job.timeout('10');
        expect(job.validate().hasErrors()).toBe(false);

        job.timeout(10);
        expect(job.validate().hasErrors()).toBe(false);

        job.timeout('');
        expect(job.validate().errors('timeout')).toEqual(['Timeout must be a positive integer']);

        job.timeout('snafu');
        expect(job.validate().errors('timeout')).toEqual(['Timeout must be a positive integer']);

        job.timeout('123.xy');
        expect(job.validate().errors('timeout')).toEqual(['Timeout must be a positive integer']);

        job.timeout('xy.123');
        expect(job.validate().errors('timeout')).toEqual(['Timeout must be a positive integer']);

        job.timeout('-123');
        expect(job.validate().errors('timeout')).toEqual(['Timeout must be a positive integer']);
      });

      it("should validate runInstanceCount", function () {
        job.runInstanceCount('all');
        expect(job.validate().hasErrors()).toBe(false);

        job.runInstanceCount(null);
        expect(job.validate().hasErrors()).toBe(false);

        job.runInstanceCount(undefined);
        expect(job.validate().hasErrors()).toBe(false);

        job.runInstanceCount('10');
        expect(job.validate().hasErrors()).toBe(false);

        job.runInstanceCount(10);
        expect(job.validate().hasErrors()).toBe(false);

        job.runInstanceCount('');
        expect(job.validate().errors('runInstanceCount')).toEqual(['Run instance count must be a positive integer']);

        job.runInstanceCount('snafu');
        expect(job.validate().errors('runInstanceCount')).toEqual(['Run instance count must be a positive integer']);

        job.runInstanceCount('-123');
        expect(job.validate().errors('runInstanceCount')).toEqual(['Run instance count must be a positive integer']);

        job.runInstanceCount('123.xy');
        expect(job.validate().errors('runInstanceCount')).toEqual(['Run instance count must be a positive integer']);

        job.runInstanceCount('xy.123');
        expect(job.validate().errors('runInstanceCount')).toEqual(['Run instance count must be a positive integer']);
      });
    });

    function sampleJobJSON() {
      /* eslint-disable camelcase */
      return {
        name:                  "UnitTest",
        run_instance_count:    10,
        timeout:               20,
        resources:             ["jdk5", "tomcat"],
        elastic_profile_id:    null,
        environment_variables: [
          {
            name:   "MULTIPLE_LINES",
            encrypted_value:  "multiplelines",
            secure: true
          },
          {
            name:   "COMPLEX",
            value:  "This has very <complex> data",
            secure: false
          }
        ],
        tasks:                 [
          {
            type:       "ant",
            attributes: {
              target:            "clean",
              working_directory: "dir",
              build_file:        "",
              run_if:            ['any'],
              on_cancel:         null
            }
          }
        ],
        artifacts:             [
          {
            type:        "build",
            source:      "target/dist.jar",
            destination: "pkg"
          }
        ],
        tabs:                  [
          {
            name: "coverage",
            path: "Jcoverage/index.html"
          }
        ],
        properties:            [
          {
            name:   "coverage.class",
            source: "target/emma/coverage.xml",
            xpath:  "substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')"
          }
        ]
      };
      /* eslint-enable camelcase */
    }
  });
});
