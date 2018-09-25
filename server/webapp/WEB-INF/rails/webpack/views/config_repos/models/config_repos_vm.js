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

const Stream    = require("mithril/stream");
const _         = require("lodash");
const Materials = require("models/config_repos/materials");


function ReposListVM(model) {
  const repos = Stream([]);

  CreateSupport.call(this, model, repos);
  UpdateSupport.call(this, model, repos);
  DeleteSupport.call(this, model, repos);

  this.load = () => {
    this.loading(true);
    model.all().then((data) => {
      repos(data._embedded.config_repos.map((r) => new ConfigRepoVM(r)));
    }).always(() => this.loading(false));

    return this;
  };

  this.loading = Stream(false);
  this.repos = repos;
}

function ConfigRepoVM(data) {
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

  this.toJSON = () => {
    return {
      id: this.id(),
      plugin_id: this.pluginId(), // eslint-disable-line camelcase
      material: {
        type: this.type(),
        attributes: this.attributes().toJSON()
      },
      configuration: _.cloneDeep(this.configuration())
    };
  };

  this.clone = () => {
    const cloned = new ConfigRepoVM(this.toJSON());
    cloned.etag(this.etag());
    return cloned;
  };
}

// Mixins

const MATERIAL_SELECTIONS = [
  { id: "git", text: "Git" },
  { id: "hg", text: "Mercurial" },
  { id: "svn", text: "Subversion" },
  { id: "p4", text: "Perforce" },
  { id: "tfs", text: "Team Foundation Server" },
  { id: "package", text: "Package" }
];

function CreateSupport(model, repos) {
  this.availMaterials = MATERIAL_SELECTIONS;
  this.typeToAdd = Stream(this.availMaterials[0].id);

  this.addModel = Stream(null);

  this.addMode = () => !!this.addModel();
  this.enterAddMode = () => {
    this.editModel(null);

    const payload = {
      material: {
        type: this.typeToAdd()
      }
    };

    this.addModel(new ConfigRepoVM(payload));
  };

  this.exitAddMode = () => this.addModel(null);
  this.createRepo = () => model.create(this.addModel()).then((data, _s, xhr) => {
    const repo = new ConfigRepoVM(data);
    repo.etag(parseEtag(xhr));
    repos().push(repo);
  });
}

function UpdateSupport(model, repos) {
  this.editModel = Stream(null);
  this.editMode = (repo) => !!this.editModel() && this.editModel().id() === repo.id();

  this.enterEditMode = (repo) => {
    this.addModel(null);

    repo = repo.clone();
    model.get(repo.etag(), repo.id()).then((data, _status, xhr) => {
      repo.etag(parseEtag(xhr));

      if (304 !== xhr.status) { repo.initialize(data); }

      this.editModel(repo);
    });
  };

  this.exitEditMode = () => this.editModel(null);

  this.updateRepo = (repo) => {
    return model.update(repo.etag(), repo).then((data) => {
      repo.initialize(data);

      const idxToReplace = _.findIndex(repos(), (r) => r.id() === repo.id()); // should always be found
      repos().splice(idxToReplace, 1, repo);
    });
  };
}

function DeleteSupport(model, repos) {
  this.removeRepo = (repo) => model.delete(repo.id()).then(() => repos().splice(repos().indexOf(repo), 1));
}

// Utility functions

function parseEtag(req) { return (req.getResponseHeader("ETag") || "").replace(/--(gzip|deflate)/, ""); }

module.exports = ReposListVM;
