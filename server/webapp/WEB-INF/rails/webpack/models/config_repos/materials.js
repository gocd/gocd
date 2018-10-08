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
const fmt         = Validatable.DefaultOptions.forId;

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
      default:
        throw new Error(`Unknown material type: ${type}`);
    }
  }
};

// Instances

function GitMaterial(data) {
  BaseFields.call(this);

  Field.call(this, "url");
  Field.call(this, "branch", {default: "master"});

  Common.call(this, data);
}

function HgMaterial (data) {
  BaseFields.call(this);

  Field.call(this, "url");

  Common.call(this, data);
}

function SvnMaterial (data) {
  BaseFields.call(this);

  Field.call(this, "url");
  Field.call(this, "check_externals", {display: "Check externals", type: "boolean", default: true});

  AuthFields.call(this);

  Common.call(this, data);
}

function P4Material (data) {
  BaseFields.call(this);

  Field.call(this, "port");
  Field.call(this, "use_tickets", {display: "Use tickets", type: "boolean", default: false});
  Field.call(this, "view");

  AuthFields.call(this);

  Common.call(this, data);
}

function TfsMaterial (data) {
  BaseFields.call(this);

  Field.call(this, "url");
  Field.call(this, "project_path", {display: "Project path"});
  Field.call(this, "domain");

  AuthFields.call(this);

  Common.call(this, data);
}

// Mixins

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
        const format = this[key].opts("format");
        this.validateFormatOf(key, format instanceof RegExp ? { format } : format);
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

function BaseFields() {
  Field.call(this, "name", {display: "Material name", required: false, format: fmt("Name")});
  Field.call(this, "auto_update", {display: "Auto-update changes", type: "boolean", default: true, readOnly: true});
}

function AuthFields() {
  Field.call(this, "username", {required: false});
  Field.call(this, "password", {type: "secret", required: false});
  Field.call(this, "encrypted_password", {display: "Encrypted password", type: "secret", required: false});
}

Materials.Field = Field;
Materials.Common = Common;

module.exports = Materials;
