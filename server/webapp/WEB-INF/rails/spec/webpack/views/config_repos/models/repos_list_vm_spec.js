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
const Stream      = require("mithril/stream");
const Dfr         = require("jquery").Deferred;
const ReposListVM = require("views/config_repos/models/repos_list_vm");
const Routes      = require("gen/js-routes");

describe("Config Repo List VM", () => {
  it("load() fetches data and creates config repo view models", () => {
    const model = mockModel();
    const response = fetchAllResponse(singleRepo("repo-01"), singleRepo("repo-02"));
    model.all.and.returnValue(promise([response]));

    const vm = new ReposListVM(model);
    vm.load();

    expect(vm.repos().length).toBe(2);
    expect(_.map(vm.repos(), (r) => r.id()).sort()).toEqual(["repo-01", "repo-02"]);
  });

  it("handles error when load() fails", () => {
    const model = mockModel();
    model.all.and.returnValue(promise(null, ["boom!"]));

    const vm = new ReposListVM(model);
    expect(vm.errors().length).toBe(0);
    vm.load();

    expect(vm.errors()).toEqual(["boom!"]);
    expect(vm.repos().length).toBe(0);
  });

  it("enterAddMode() creates a temporary repo", () => {
    const vm = new ReposListVM(null);
    expect(vm.addMode()).toBe(false);
    expect(vm.addModel()).toBe(null);

    vm.typeToAdd("hg");
    vm.pluginChoices([{id: "a"}]);
    vm.editModel({});

    vm.enterAddMode();

    expect(vm.addMode()).toBe(true);
    const repo = vm.addModel();

    expect(repo.type()).toBe("hg");
    expect(repo.pluginId()).toBe("a");
    expect(vm.editModel()).toBe(null); // addMode() is exclusive; should clear editModel()
  });

  it("exitAddMode() clears temporary repo", () => {
    const vm = new ReposListVM(null);
    vm.addModel({});

    expect(vm.addMode()).toBe(true);

    vm.exitAddMode();

    expect(vm.addMode()).toBe(false);
    expect(vm.addModel()).toBe(null);
  });

  it("createRepo() adds a repo", () => {
    const model = mockModel();
    model.create.and.returnValue(promise([{id: 1, material: {type: "git", attributes: {}}}, "1234"], null));

    const vm = new ReposListVM(model);

    expect(vm.repos().length).toBe(0);

    const addModel = cheapRepo(1);
    addModel.allowSave.and.returnValue(true);
    vm.createRepo(addModel);
    expect(vm.repos().length).toBe(1);
    const repo = vm.repos()[0];

    expect(repo.id()).toBe(1);
    expect(repo.type()).toBe("git");
    expect(repo.etag()).toBe("1234");
  });

  it("createRepo() doesn't add entry on validation failure", () => {
    const model = mockModel();
    const vm = new ReposListVM(model);
    expect(vm.repos().length).toBe(0);

    const addModel = cheapRepo(1);
    addModel.allowSave.and.returnValue(false);

    vm.createRepo(addModel);
    expect(model.create).not.toHaveBeenCalled();
    expect(vm.repos().length).toBe(0);
  });

  it("exitEditMode() clears the temporary repo in edit mode", () => {
    const vm = new ReposListVM(null);
    const repo = cheapRepo(1);
    vm.editModel(repo);

    expect(vm.editMode(repo)).toBe(true);

    vm.exitEditMode();
    expect(vm.editMode(repo)).toBe(false);
    expect(vm.editModel()).toBe(null);
  });

  it("enterEditMode() should create temporary repo from existing repo", () => {
    const model = mockModel();
    const vm = new ReposListVM(model);
    const repo = cheapRepo(1);

    model.get.and.returnValue(promise([{id: 1}, "123", 200], null));

    vm.addModel({}); // should be cleared after entering edit mode

    expect(vm.editMode(repo)).toBe(false);
    vm.enterEditMode(repo);

    expect(vm.addModel()).toBe(null);
    expect(model.get).toHaveBeenCalledWith(null, 1);
    expect(repo.etag()).toBe("123");
    expect(repo.initialize).toHaveBeenCalledWith({id: 1});
    expect(vm.editModel()).not.toBe(repo);
    expect(vm.editModel().id()).toBe(repo.id());
  });

  it("updateRepo() updates entry with response from server", () => {
    const model = mockModel();
    model.update.and.returnValue(promise([{id: 1}, "1234"], null));

    const vm = new ReposListVM(model);
    vm.repos([cheapRepo(0), cheapRepo(1), cheapRepo(2)]);

    const clone = vm.repos()[1].clone();
    clone.allowSave.and.returnValue(true);

    vm.updateRepo(clone);
    expect(clone.revisionResult.reload).toHaveBeenCalled();

    expect(clone.etag()).toBe("1234");
    expect(clone.initialize).toHaveBeenCalledWith({id: 1});
    expect(vm.repos()[1]).toBe(clone);
  });

  it("updateRepo() doesn't update entry on validation failure", () => {
    const model = mockModel();
    const vm = new ReposListVM(model);
    vm.repos([cheapRepo(0), cheapRepo(1), cheapRepo(2)]);

    const clone = vm.repos()[1].clone();
    clone.allowSave.and.returnValue(false);

    vm.updateRepo(clone);
    expect(model.update).not.toHaveBeenCalled();
  });

  it("removeRepo() removes the corresponding entry", () => {
    const model = mockModel();
    model.delete.and.returnValue(promise([], null));

    const vm = new ReposListVM(model);
    vm.repos([cheapRepo(1), cheapRepo(2), cheapRepo(3)]);

    vm.removeRepo(vm.repos()[1]);

    expect(model.delete).toHaveBeenCalledWith(2);
    expect(_.map(vm.repos(), (r) => r.id())).toEqual([1, 3]);
  });

  it("removeRepo() does not remove a repo when response fails", () => {
    const model = mockModel();
    model.delete.and.returnValue(promise(null, ["no"]));

    const vm = new ReposListVM(model);
    vm.repos([cheapRepo(1), cheapRepo(2), cheapRepo(3)]);

    vm.removeRepo(vm.repos()[1]);

    expect(_.map(vm.repos(), (r) => r.id())).toEqual([1, 2, 3]);
  });

  it("loadPlugins() populates pluginChoices from the server", () => {
    jasmine.Ajax.withMock(() => {
      const url = `${Routes.apiv4AdminPluginInfoIndexPath()}?type=configrepo`;

      jasmine.Ajax.stubRequest(url, undefined, "GET").andReturn({
        responseText: pluginInfosRespBody(),
        status: 200,
        responseHeaders: { "Content-Type": "application/vnd.go.cd.v4+json" }
      });

      const vm = new ReposListVM(mockModel());
      vm.loadPlugins();

      expect(jasmine.Ajax.requests.mostRecent().url).toBe(url);
      expect(vm.errors().length).toBe(0);
      expect(vm.pluginChoices()).toEqual([
        {id: "test1.config.plugin", text: "Test 1 Configuration Plugin"},
        {id: "test2.config.plugin", text: "Test 2 Configuration Plugin"}
      ]);
    });
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

function cheapRepo(id) {
  return {
    id() { return id; },
    initialize: jasmine.createSpy("init"),
    clone() { return cheapRepo(id); },
    etag: Stream(null),
    serverErrors: Stream(null),
    allowSave: jasmine.createSpy("allowSave()"),
    revisionResult: { reload: jasmine.createSpy("revision.reload()") }
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
      this.resolve.apply(this, "function" === typeof fulfill ? fulfill() : fulfill);
    } else {
      this.reject.apply(this, "function" === typeof reject ? reject() : reject);
    }
  }).promise();
}

function pluginInfosRespBody() {
  return JSON.stringify({
    _embedded: {
      plugin_info: [ // eslint-disable-line camelcase
        {
          id: "test1.config.plugin",
          status: {state: "active"},
          about: {name: "Test 1 Configuration Plugin"},
          extensions: [{type: "configrepo"}]
        },
        {
          id: "test2.config.plugin",
          status: {state: "active"},
          about: {name: "Test 2 Configuration Plugin"},
          extensions: [{type: "configrepo"}]
        }
      ]
    }
  });
}
