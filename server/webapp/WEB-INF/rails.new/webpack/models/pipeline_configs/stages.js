/*
 * Copyright 2017 ThoughtWorks, Inc.
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

const Stream               = require('mithril/stream');
const s                    = require('string-plus');
const Mixins               = require('models/mixins/model_mixins');
const Jobs                 = require('models/pipeline_configs/jobs');
const EnvironmentVariables = require('models/pipeline_configs/environment_variables');
const Approval             = require('models/pipeline_configs/approval');
const Validatable          = require('models/mixins/validatable_mixin');

const Stages = function (data) {
  Mixins.HasMany.call(this, {factory: Stages.Stage.create, as: 'Stage', collection: data, uniqueOn: 'name'});
};

Stages.Stage = function (data) {
  this.constructor.modelType = 'stage';
  Mixins.HasUUID.call(this);
  Validatable.call(this, data);

  this.parent = Mixins.GetterSetter();

  this.name                  = Stream(s.defaultToIfBlank(data.name, ''));
  this.fetchMaterials        = Stream(data.fetchMaterials);
  this.cleanWorkingDirectory = Stream(data.cleanWorkingDirectory);
  this.neverCleanupArtifacts = Stream(data.neverCleanupArtifacts);
  this.environmentVariables  = s.collectionToJSON(Stream(s.defaultToIfBlank(data.environmentVariables, new EnvironmentVariables())));
  this.jobs                  = s.collectionToJSON(Stream(s.defaultToIfBlank(data.jobs, new Jobs())));
  this.approval              = Stream(s.defaultToIfBlank(data.approval, new Approval({})));

  this.validatePresenceOf('name');
  this.validateUniquenessOf('name');
  this.validateAssociated('environmentVariables');
  this.validateAssociated('jobs');
};

Stages.Stage.create = (data) => new Stages.Stage(data);

Mixins.fromJSONCollection({
  parentType: Stages,
  childType:  Stages.Stage,
  via:        'addStage'
});

Stages.Stage.fromJSON = (data) => new Stages.Stage({
  name:                  data.name,
  fetchMaterials:        data.fetch_materials,
  cleanWorkingDirectory: data.clean_working_directory,
  neverCleanupArtifacts: data.never_cleanup_artifacts,
  environmentVariables:  EnvironmentVariables.fromJSON(data.environment_variables),
  jobs:                  Jobs.fromJSON(data.jobs),
  approval:              Approval.fromJSON(data.approval || {}),
  errors:                data.errors
});

module.exports = Stages;
