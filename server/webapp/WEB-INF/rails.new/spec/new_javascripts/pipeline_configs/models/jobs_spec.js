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

define(['lodash', "pipeline_configs/models/jobs", "string-plus"], function (_, Jobs, s) {
  var jobs, job;
  beforeEach(function () {
    jobs = new Jobs();
    job  = jobs.createJob({
      name:                 "UnitTest",
      runOnAllAgents:       true,
      runInstanceCount:     true,
      timeout:              10,
      environmentVariables: ["foo=bar", "boo=baz"],
      resources:            ['java', 'firefox'],
      tasks:                ['ant', 'mvn'],
      artifacts:            ['pkgs', 'logs'],
      tabs:                 ['twist', 'junit']
    });
  });

  describe("Job Model", function () {
    it("should initialize job model with name", function () {
      expect(job.name()).toBe("UnitTest");
    });

    it("should initialize job model with runOnAllAgents", function () {
      expect(job.runOnAllAgents()).toBe(true);
    });

    it("should initialize job model with runInstanceCount", function () {
      expect(job.runInstanceCount()).toBe(true);
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

    describe("validations", function () {
      it("should not allow blank job names", function () {
        var errors       = job.validate();
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

        var errorsOnOriginal = job.validate();
        expect(errorsOnOriginal.errors('name')).toEqual(['Name is a duplicate']);

        var errorsOnDuplicate = duplicateVariable.validate();
        expect(errorsOnDuplicate.errors('name')).toEqual(['Name is a duplicate']);
      });

    });

    describe("Serialization from/to JSON", function () {
      beforeEach(function () {
        job = Jobs.Job.fromJSON(sampleJobJSON());
      });

      it("should de-serialize from JSON", function () {
        expect(job.name()).toBe("UnitTest");
        expect(job.runOnAllAgents()).toBe(true);
        expect(job.runInstanceCount()).toBe(null);
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

      function sampleJobJSON() {
        return {
          name:                  "UnitTest",
          run_on_all_agents:     true,
          run_instance_count:    null,
          timeout:               20,
          resources:             ["jdk5", "tomcat"],
          environment_variables: [
            {
              name:   "MULTIPLE_LINES",
              value:  "multiplelines",
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
                target:      "clean",
                working_dir: "dir",
                build_file:  ""
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
      }
    });
  });

});
