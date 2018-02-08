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

const _ = require('lodash');

const MaterialRevision = function (date, user, comment, lastRunRevision) {
  this.date            = date;
  this.user            = user;
  this.comment         = comment;
  this.lastRunRevision = lastRunRevision;
};

MaterialRevision.fromJSON = (json) => {
  return new MaterialRevision(json.date, json.user, json.comment, json.last_run_revision);
};

//todo: Handle Different type of materials too
//GaneshSPatil should fix this;

const Material = function (name, type, fingerprint, revision) {
  this.name        = name;
  this.type        = type;
  this.fingerprint = fingerprint;
  this.revision    = revision;
};

Material.fromJSON = (json) => {
  const revision = MaterialRevision.fromJSON(json.revision);
  return new Material(json.name, json.type, json.fingerprint, revision);
};

const Materials    = {};
Materials.fromJSON = (json) => {
  return _.map(json, (material) => Material.fromJSON(material));
};

module.exports = Materials;
