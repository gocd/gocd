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

const Stream       = require("mithril/stream");
const _            = require("lodash");
const Dfr          = require("jquery").Deferred;
const PluginInfos  = require("models/shared/plugin_infos");
const ConfigRepoVM = require("views/config_repos/models/config_repo_vm");

function ReposListVM(model) {
  const repos = Stream([]);

  CreateSupport.call(this, model, repos);
  UpdateSupport.call(this, model, repos);
  DeleteSupport.call(this, model, repos);

  const self = this;
  const addError = (msg) => { // fixme: add better error handling. this is cheap & not production quality.
    if (!contains(self.errors(), msg)) {
      self.errors().push(msg);
    }
  };

  this.load = () => {
    this.errors([]);
    this.loading(true);
    model.all().then((data) => {
      repos(data._embedded.config_repos.map((r) => new ConfigRepoVM(r)));
    }, addError).always(() => this.loading(false));

    return this;
  };

  this.loadPlugins = () => {
    this.errors([]);
    return PluginInfos.all(null, {type: "configrepo"}).then((infos) => {
      const all = [];
      infos.eachPluginInfo((p) => { all.push({ id: p.id(), text: p.about().name() }); });
      this.pluginChoices(all);
    }, addError);
  };

  this.pluginChoices = Stream([]);
  this.loading = Stream(false);

  this.errors = Stream([]);
  this.repos = repos;
}

// Mixins

const MATERIAL_SELECTIONS = [
  { id: "git", text: "Git" },
  { id: "hg", text: "Mercurial" },
  { id: "svn", text: "Subversion" },
  { id: "p4", text: "Perforce" },
  { id: "tfs", text: "Team Foundation Server" }
];

function CreateSupport(model, repos) {
  this.availMaterials = MATERIAL_SELECTIONS;
  this.typeToAdd = Stream(this.availMaterials[0].id);

  this.addModel = Stream(null);

  this.addMode = () => !!this.addModel();
  this.enterAddMode = () => {
    this.editModel(null);

    const payload = {
      plugin_id: this.pluginChoices()[0].id, // eslint-disable-line camelcase
      material: {
        type: this.typeToAdd()
      }
    };

    this.addModel(new ConfigRepoVM(payload));
  };

  this.exitAddMode = () => this.addModel(null);

  this.createRepo = withValidation((repo) => model.create(repo).then((data, etag) => {
    const repo = new ConfigRepoVM(data);
    if (etag) { repo.etag(etag); }
    repos().push(repo);
  }));
}

function UpdateSupport(model, repos) {
  this.editModel = Stream(null);
  this.editMode = (repo) => !!this.editModel() && this.editModel().id() === repo.id();

  this.enterEditMode = (repo) => {
    this.addModel(null);

    model.get(repo.etag(), repo.id()).then((data, etag, status) => {
      if (etag) { repo.etag(etag); }
      if (304 !== status && data) { repo.initialize(data); }

      this.editModel(repo.clone());
    });
  };

  this.exitEditMode = () => this.editModel(null);

  this.updateRepo = withValidation((repo) => model.update(repo.etag(), repo).then((data, etag) => {
    if (etag) { repo.etag(etag); }
    repo.initialize(data);

    const idx = _.findIndex(repos(), (r) => r.id() === repo.id()); // should not fail
    repos().splice(idx, 1, repo);
  }));
}

function DeleteSupport(model, repos) {
  this.removeRepo = (repo) => model.delete(repo.id()).then(() => repos().splice(repos().indexOf(repo), 1));
}

// Utility functions

function withValidation(proceed) {
  return (repo) => repo.allowSave() ? proceed(repo) : Dfr(function fail() { this.reject(); }).promise();
}

function contains(arr, el) { return !!~arr.indexOf(el); }

// expose for tests
_.assign(ReposListVM, {CreateSupport, UpdateSupport, DeleteSupport});

module.exports = ReposListVM;
