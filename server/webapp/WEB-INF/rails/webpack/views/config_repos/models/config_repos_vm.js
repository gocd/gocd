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

const Stream = require('mithril/stream');
const Materials = require('models/config_repos/materials');

function ReposListVM(model) {
  const repos = Stream([]);
  const self = this;

  this.availMaterials = [{ id: "git", text: "Git" }, { id: "hg", text: "Mercurial" }, { id: "svn", text: "Svn" }, { id: "p4", text: "Perforce" }, { id: "tfs", text: "Tfs" }, { id: "package", text: "Package" }];
  this.typeToAdd = Stream("git");
  this.addModel = Stream(null);
  this.addMode = () => !!this.addModel();

  this.enterAddMode = () => {
    const payload = {
      material: {
        type: this.typeToAdd()
      }
    };

    this.addModel(new ConfigRepoVM(payload, model, self));
  };

  this.exitAddMode = () => this.addModel(null);

  this.fetchReposData = () => {
    this.loading(true);
    model.all().then((data) => {
      repos(data._embedded.config_repos.map((r) => new ConfigRepoVM(r, model, self)));
    }).always(() => self.loading(false));

    return this;
  };

  this.loading = Stream(false);
  this.repos = repos;

  this.removeRepo = (repo) => repos().splice(repos().indexOf(repo), 1);
  this.addRepo = (repo) => repos().push(repo);
}

function ConfigRepoVM(data, model, parent) {
  this.editModel = Stream(null);
  this.id = Stream();
  this.pluginId = Stream();
  this.type = Stream();
  this.attributes = Stream();
  this.configuration = Stream();
  this.etag = Stream(null);

  this.initialize = (data) => {
    this.id(data.id);
    this.pluginId(data.plugin_id);
    this.type(data.material.type);
    this.attributes(Materials.get(this.type(), data));
    this.configuration(data.configuration || []);
  };

  this.initialize(data);

  this.editMode = () => !!this.editModel();

  this.enterEditMode = () => {
    model.get(this.etag(), this.id()).then((data, _status, xhr) => {
      this.etag(parseEtag(xhr));

      if (304 !== xhr.status) { this.initialize(data); }

      this.editModel(this.attributes().clone());
    });
  };

  this.exitEditMode = () => this.editModel(null);

  this.create = () => {
    const payload = {
      id: this.id(),
      plugin_id: this.pluginId(), // eslint-disable-line camelcase
      material: {
        type: this.type(),
        attributes: this.attributes()
      },
      configuration: this.configuration()
    };

    return model.create(payload).then(() => parent.addRepo(this));
  };

  this.saveUpdate = () => {
    const payload = {
      id: this.id(),
      plugin_id: this.pluginId(), // eslint-disable-line camelcase
      material: {
        type: this.type(),
        attributes: this.editModel()
      },
      configuration: this.configuration()
    };

    return model.update(this.etag(), payload).then((data) => {
      this.initialize(data);
      this.exitEditMode();
    });
  };

  this.remove = () => {
    return model.delete(this.id()).then(() => parent.removeRepo(this));
  };
}


function parseEtag(req) { return (req.getResponseHeader("ETag") || "").replace(/--(gzip|deflate)/, ""); }
module.exports = ReposListVM;
