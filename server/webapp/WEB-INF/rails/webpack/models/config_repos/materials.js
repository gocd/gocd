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
const Field       = require("models/config_repos/field");
const Validatable = require("models/mixins/validatable_mixin");

const Materials = {
  get(type, data) {
    switch(type) {
      case "git":
        return new GitMaterial(data);
      case "hg":
        return new HgMaterial(data);
      case "svn":
        return new SvnMaterial(data);
      case "p4":
        return new P4Material(data);
      case "tfs":
        return new TfsMaterial(data);
      case "package":
        return new PackageMaterial(data);
      default:
        throw new Error(`Unknown material type: ${type}`);
    }
  }
};

function Common(data) {
  Validatable.call(this, { errors: {} });

  this.keys = (this.keys || []);

  this.initialize = (data) => {
    data = _.get(data, "material.attributes", {});

    _.each(this.keys, (key) => {
      this[key].init(data[key]);

      if (this[key].opts("required")) {
        this.validatePresenceOf(key);
      }

      if (this[key].opts("format")) {
        this.validateFormatOf(key, {format: this[key].opts("format")});
      }
    });
  };

  this.toJSON = () => _.reduce(this.keys, (memo, k) => {
    memo[k] = this[k]();
    return memo;
  }, {});

  this.clone = () => new this.constructor(data);

  this.initialize(data);
}

function GitMaterial(data) {
  Field.call(this, "name", {display: "Material name", required: false});
  Field.call(this, "url");
  Field.call(this, "branch", {default: "master"});
  Field.call(this, "auto_update", {display: "Auto-update changes", type: "boolean", default: true});

  Common.call(this, data);
}

function HgMaterial (data) {
  Field.call(this, "name", {display: "Material name", required: false});
  Field.call(this, "url");
  Field.call(this, "auto_update", {display: "Auto-update changes", type: "boolean", default: true});

  Common.call(this, data);
}

function SvnMaterial (data) {
  Field.call(this, "name", {display: "Material name", required: false});
  Field.call(this, "url");
  Field.call(this, "username", {required: false});
  Field.call(this, "password", {type: "secret", required: false});
  Field.call(this, "encrypted_password", {display: "Encrypted password", type: "secret", required: false});
  Field.call(this, "auto_update", {display: "Auto-update changes", type: "boolean", default: true});
  Field.call(this, "check_externals", {display: "Check externals", type: "boolean", default: true});

  Common.call(this, data);
}

function P4Material (data) {
  Field.call(this, "name", {display: "Material name", required: false});
  Field.call(this, "port");
  Field.call(this, "use_tickets", {display: "Use tickets", type: "boolean", default: false});
  Field.call(this, "view");
  Field.call(this, "auto_update", {display: "Auto-update changes", type: "boolean", default: true});
  Field.call(this, "username", {required: false});
  Field.call(this, "password", {type: "secret", required: false});
  Field.call(this, "encrypted_password", {display: "Encrypted Password", type: "secret", required: false});

  Common.call(this, data);
}

function TfsMaterial (data) {
  Field.call(this, "name", {display: "Material name", required: false});
  Field.call(this, "url");
  Field.call(this, "project_path", {display: "Project path"});
  Field.call(this, "domain");
  Field.call(this, "auto_update", {display: "Auto-update changes", type: "boolean", default: true});
  Field.call(this, "username", {required: false});
  Field.call(this, "password", {type: "secret", required: false});
  Field.call(this, "encrypted_password", {display: "Encrypted password", type: "secret", required: false});

  Common.call(this, data);
}

function PackageMaterial (data) {
  Field.call(this, "ref");

  Common.call(this, data);
}

Materials.Field = Field;
Materials.Common = Common;

module.exports = Materials;
