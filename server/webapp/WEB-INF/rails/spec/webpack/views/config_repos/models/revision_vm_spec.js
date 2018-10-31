/*
 * Copyright 2018 ThoughtWorks, Inc.
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

const RevisionVM = require("views/config_repos/models/revision_vm");

import SparkRoutes from "helpers/spark_routes";

const ID = "this-repo-sucks-001";

describe("Revision View Model", () => {
  it("reload() loads the last parsed result", (done) => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.configRepoLastParsedResultPath(ID), undefined, "GET").andReturn({
        responseText: JSON.stringify({ revision: "1234", success: false, error: "0xBEEFCODE" }),
        status: 200,
        responseHeaders: {
          "Content-Type": "application/vnd.go.cd.v1+json"
        }
      });

      const vm = new RevisionVM(ID);
      vm.reload().then(() => {
        expect(vm.revision()).toBe("1234");
        expect(vm.success()).toBe(false);
        expect(vm.error()).toBe("0xBEEFCODE");
        done();
      }, () => done.fail("Request should have succeeded"));
    });
  });

  it("reload() captures server errors", (done) => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.configRepoLastParsedResultPath(ID), undefined, "GET").andReturn({
        responseText: JSON.stringify({ message: "oh noes!" }),
        status: 400,
        responseHeaders: {
          "Content-Type": "application/vnd.go.cd.v1+json"
        }
      });

      const vm = new RevisionVM(ID);
      vm.reload().then(() => done.fail("Request should have failed"), () => {
        expect(vm.serverErrors()).toBe("oh noes!");
        expect(vm.revision()).toBe(null);
        expect(vm.success()).toBe(false);
        expect(vm.error()).toBe(null);
        done();
      });
    });
  });

  it("forceUpdate() starts a status poller via monitorProgress()", (done) => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.configRepoTriggerUpdatePath(ID), undefined, "POST").andReturn({
        responseText: JSON.stringify({ message: "OK" }),
        status: 201,
        responseHeaders: {
          "Content-Type": "application/vnd.go.cd.v1+json"
        }
      });

      const vm = new RevisionVM(ID);
      spyOn(vm, "monitorProgress");

      vm.forceUpdate().then(() => {
        expect(vm.monitorProgress).toHaveBeenCalled();
        done();
      }, () => done.fail("Request should have succeeded"));
    });
  });

  it("forceUpdate() captures server errors", (done) => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest(SparkRoutes.configRepoTriggerUpdatePath(ID), undefined, "POST").andReturn({
        responseText: JSON.stringify({ message: "you dolt! you already triggered me!" }),
        status: 409,
        responseHeaders: {
          "Content-Type": "application/vnd.go.cd.v1+json"
        }
      });

      const vm = new RevisionVM(ID);
      spyOn(vm, "monitorProgress");

      vm.forceUpdate().then(() => done.fail("Request should have failed"), () => {
        expect(vm.serverErrors()).toBe("you dolt! you already triggered me!");
        expect(vm.monitorProgress).not.toHaveBeenCalled();
        done();
      });
    });
  });
});
