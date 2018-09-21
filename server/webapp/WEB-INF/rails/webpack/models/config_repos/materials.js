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

const stream = require("mithril/stream");
const _ = require("lodash");

const Materials = {
  get(type, data) {
    switch(type) {
      case "git":
        return new GitMaterial(data);
      case "hg":
        return new HgMaterial(data);
    }
  }
};

function GitMaterial (data) {
  const DEFAULTS = {name: "", url: "", auto_update: true, branch: "master"}; // eslint-disable-line camelcase
  const attrs = _.assign(DEFAULTS, _.get(data, "material.attributes", {}));

  this.name = stream(attrs.name);
  this.url = stream(attrs.url);
  this.branch = stream(attrs.branch);
  this.auto_update = stream(attrs.auto_update); // eslint-disable-line camelcase
}

function HgMaterial (data) {
  const DEFAULTS = {name: "", url: "", auto_update: true}; // eslint-disable-line camelcase
  const attrs = _.assign(DEFAULTS, _.get(data, "material.attributes", {}));

  this.name = stream(attrs.name);
  this.url = stream(attrs.url);
  this.auto_update = stream(attrs.auto_update); // eslint-disable-line camelcase
}


module.exports = Materials;
