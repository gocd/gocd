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
const _ = require("lodash");

function ReposListVM(model) {
  const repos = Stream([]);

  this.fetchReposData = () => {
    this.loading(true);
    model.all().then((data) => {
      repos(data._embedded.config_repos.map((r) => new ConfigRepoVM(r, model)));
    }).always(() => this.loading(false));

    return this;
  };

  this.loading = Stream(false);
  this.repos = repos;
}

function ConfigRepoVM(data, model) {
  this.editModel = Stream(null);
  this.id = Stream();
  this.type = Stream();
  this.attributes = Stream();
  this.configuration = Stream();
  this.etag = Stream(null);

  this.initialize = (data) => {
    this.id(data.id);
    this.type(data.material.type);
    this.attributes(data.material.attributes);
    this.configuration(data.configuration);
  };

  this.initialize(data);

  this.editMode = () => !!this.editModel();

  this.enterEditMode = () => {
    model.get(this.etag(), this.id()).then((data, _status, xhr) => {
      this.etag(parseEtag(xhr));

      if (304 === xhr.status) { this.initialize(data); }

      this.editModel(
        _.reduce(this.attributes(), (memo, v, k) => {
          memo[k] = Stream(v);
          return memo;
        }, {})
      );
    });
  };


  this.exitEditMode = () => this.editModel(null);

  this.save = () => {
    const payload = _.assign({}, data, {
      material: {
        type: this.type(),
        attributes: this.editModel()
      }
    });

    model.update(this.etag(), payload).then((data) => {
      this.initialize(data);
      this.exitEditMode();
    });
  };
}

function parseEtag(req) { return (req.getResponseHeader("ETag") || "").replace(/--(gzip|deflate)/, ""); }
module.exports = ReposListVM;
