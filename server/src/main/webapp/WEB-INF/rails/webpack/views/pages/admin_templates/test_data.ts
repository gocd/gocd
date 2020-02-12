/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import {Template} from "models/admin_templates/templates";

function getEnvironmentVariables(type: "pipeline" | "stage" | "job") {
  return [
    {
      name: `${type}-env-var`,
      secure: false,
      value: "blah"
    },
    {
      name: `secure-${type}-env-var`,
      secure: true,
      encrypted_value: "AES:foo:bar"
    }
  ];
}

export function massiveTemplate(): Template {
  return {
    name: "build-gradle-linux",
    stages: [{
      name: "build-non-server",
      fetch_materials: true,
      clean_working_directory: false,
      never_cleanup_artifacts: false,
      approval: {
        type: "success",
        allow_only_on_success: false,
        authorization: {
          roles: [],
          users: []
        }
      },
      environment_variables: getEnvironmentVariables("pipeline"),
      jobs: [{
        name: "FastTests",
        run_instance_count: 3,
        timeout: null,
        elastic_profile_id: "some-docker-profile",
        environment_variables: [],
        resources: [],
        tasks: [{
          type: "exec",
          attributes: {
            run_if: ["passed"],
            command: "java",
            arguments: ["-version"]
          }
        }, {
          type: "exec",
          attributes: {
            run_if: ["passed"],
            command: ".ci/with-java",
            arguments: ["./gradlew", "allTests", "allTestsJunitHtmlReport", "--continue"]
          }
        }],
        tabs: [{
          name: "JUnitResults",
          path: "JUnitResults/allTests/index.html"
        }],
        artifacts: [{
          type: "build",
          source: "target/reports/tests/allTests",
          destination: "JUnitResults"
        }, {
          type: "build",
          source: "server/target/heap-dumps",
          destination: ""
        }]
      }, {
        name: "Jasmine",
        run_instance_count: null,
        timeout: null,
        environment_variables: [],
        resources: ["linux", "firefox"],
        tasks: [{
          type: "exec",
          attributes: {
            run_if: ["passed"],
            command: "java",
            arguments: ["-version"]
          }
        }, {
          type: "exec",
          attributes: {
            run_if: ["passed"],
            command: "./.ci/with-java",
            arguments: ["./gradlew", "jasmine"]
          }
        }],
        tabs: [],
        artifacts: [{
          type: "test",
          source: "server/target/karma_reports/*.xml",
          destination: "karma"
        }, {
          type: "build",
          source: "server/target/karma_reports/Firefox*/*.*",
          destination: "karma-ff"
        }]
      }]
    }, {
      name: "build-server",
      fetch_materials: true,
      clean_working_directory: false,
      never_cleanup_artifacts: false,
      approval: {
        type: "success",
        allow_only_on_success: false,
        authorization: {
          roles: [],
          users: []
        }
      },
      environment_variables: [],
      jobs: [{
        name: "ServerIntegrationTests",
        run_instance_count: 5,
        timeout: null,
        elastic_profile_id: "ecs-gocd-dev-build-5gb-mem",
        environment_variables: [],
        resources: [],
        tasks: [{
          type: "exec",
          attributes: {
            run_if: ["passed"],
            command: "java",
            arguments: ["-version"]
          }
        }, {
          type: "exec",
          attributes: {
            run_if: ["passed"],
            command: "./.ci/with-java",
            arguments: ["./gradlew", "server:integrationTest"]
          }
        }],
        tabs: [{
          name: "SlowTest",
          path: "JUnitResults/integrationTest/index.html"
        }],
        artifacts: [{
          type: "test",
          source: "server/target/test-results/test/*.xml",
          destination: "test-reports"
        }, {
          type: "build",
          source: "server/target/reports/tests/**/*.*",
          destination: "JUnitResults"
        }]
      }, {
        name: "Rspec",
        run_instance_count: 2,
        timeout: null,
        elastic_profile_id: "ecs-gocd-dev-build-highmem-centos-7",
        environment_variables: [],
        resources: [],
        tasks: [{
          type: "exec",
          attributes: {
            run_if: ["passed"],
            command: "java",
            arguments: ["-version"]
          }
        }, {
          type: "exec",
          attributes: {
            run_if: ["passed"],
            command: "./.ci/with-java",
            arguments: ["./gradlew", "parallelRspec"]
          }
        }],
        tabs: [],
        artifacts: [{
          type: "test",
          source: "server/target/rspec-results/*.xml",
          destination: "test-reports"
        }]
      }]
    }]
  };
}
