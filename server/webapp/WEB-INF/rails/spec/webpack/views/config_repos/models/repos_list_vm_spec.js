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

const _           = require("lodash");
const Dfr         = require("jquery").Deferred;
const ReposListVM = require("views/config_repos/models/repos_list_vm");

describe("Config Repo List VM", () => {
  it("load() fetches data and creates config repo view models", () => {
    const model = mockModel();
    const response = fetchAllResponse(singleRepo("repo-01"), singleRepo("repo-02"));
    model.all.and.returnValue(promise(response));

    const vm = new ReposListVM(model);
    vm.load();

    expect(vm.repos().length).toBe(2);
    expect(_.map(vm.repos(), (r) => r.id()).sort()).toEqual(["repo-01", "repo-02"]);
  });

  it("handles error when load() fails", () => {
    const model = mockModel();
    model.all.and.returnValue(promise(null, "boom!"));

    const vm = new ReposListVM(model);
    expect(vm.errors().length).toBe(0);
    vm.load();

    expect(vm.errors()).toEqual(["boom!"]);
    expect(vm.repos().length).toBe(0);
  });
});

// utility functions

function fetchAllResponse(_argv /* variable args */) {
  const repos = arguments.length ? [].slice.call(arguments) : [];

  return {
    _embedded: { config_repos: repos} // eslint-disable-line camelcase
  };
}

function singleRepo(id="repo-01") {
  return {
    /* eslint-disable camelcase */
    id,
    plugin_id: "test.configrepo.plugin",
    material: {
      type: "hg",
      attributes: {
        name: "my-hg-repo", url: "https://bitnugget.org/unicorns", auto_update: true
      }
    },
    configuration: []
    /* eslint-enable camelcase */
  };
}

function mockModel() {
  return {
    all: jasmine.createSpy("all()"),
    get: jasmine.createSpy("get()"),
    create: jasmine.createSpy("create()"),
    update: jasmine.createSpy("update()"),
    delete: jasmine.createSpy("delete()")
  };
}

function promise(fulfill, reject) {
  return Dfr(function execute() {
    if (fulfill) {
      this.resolve("function" === typeof fulfill ? fulfill() : fulfill);
    } else {
      this.reject("function" === typeof reject ? reject() : reject);
    }
  }).promise();
}
