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
const _                    = require('lodash');
const s                    = require('string-plus');
const Mixins               = require('models/mixins/model_mixins');
const EnvironmentVariables = require('models/pipeline_configs/environment_variables');
const Tasks                = require('models/pipeline_configs/tasks');
const Artifacts            = require('models/pipeline_configs/artifacts');
const Tabs                 = require('models/pipeline_configs/tabs');
const Properties           = require('models/pipeline_configs/properties');
const Validatable          = require('models/mixins/validatable_mixin');

const Jobs = function (data) {
  Mixins.HasMany.call(this, {factory: Jobs.Job.create, as: 'Job', collection: data, uniqueOn: 'name'});
};

const TimeoutValidator = function () {
  this.validate = (entity) => {
    if (!(entity.isTimeoutNever() || entity.isTimeoutDefault() || entity.isTimeoutCustom())) {
      entity.errors().add('timeout', Validatable.ErrorMessages.mustBePositiveNumber('timeout'));
    }
  };
};

const RunInstanceCountValidator = function () {
  this.validate = (entity) => {
    if (!(entity.isRunOnAllAgents() || entity.isRunOnOneAgent() || entity.isRunOnSomeAgents())) {
      entity.errors().add('runInstanceCount', Validatable.ErrorMessages.mustBePositiveNumber('runInstanceCount'));
    }
  };
};

Jobs.Job = function (data) {
  this.constructor.modelType = 'job';
  Mixins.HasUUID.call(this);
  Validatable.call(this, data);

  this.parent = Mixins.GetterSetter();

  this.name                 = Stream(s.defaultToIfBlank(data.name, ''));
  this.runInstanceCount     = Stream(data.runInstanceCount);
  this.timeout              = Stream(data.timeout);
  this.resources            = s.withNewJSONImpl(Stream(s.defaultToIfBlank(data.resources, '')), s.stringToArray);
  this.environmentVariables = s.collectionToJSON(Stream(s.defaultToIfBlank(data.environmentVariables, new EnvironmentVariables())));
  this.tasks                = s.collectionToJSON(Stream(s.defaultToIfBlank(data.tasks, new Tasks())));
  this.artifacts            = s.collectionToJSON(Stream(s.defaultToIfBlank(data.artifacts, new Artifacts())));
  this.tabs                 = s.collectionToJSON(Stream(s.defaultToIfBlank(data.tabs, new Tabs())));
  this.properties           = s.collectionToJSON(Stream(s.defaultToIfBlank(data.properties, new Properties())));
  const _elasticProfileId   = Stream(s.defaultToIfBlank(data.elasticProfileId, null));
  this.elasticProfileId     = function (...args) {
    if (args.length === 1) {
      // setter
      if (args[0] === 'null' || args[0] === 'undefined') {
        return _elasticProfileId(null);
      }
      return _elasticProfileId(args[0]);
    } else {
      // getter
      return _elasticProfileId();
    }
  };

  this.elasticProfileId.toJSON = _elasticProfileId.toJSON.bind(_elasticProfileId);

  this.isBlank = function () {
    return s.isBlank(this.name()) &&
      s.isBlank(this.resources()) || s.isBlank(this.elasticProfileId()) &&
      this.environmentVariables().everyVariable((variable) => variable.isBlank()) &&
      this.tasks().everyTask((task) => task.isEmpty()) &&
      this.tabs().everyTab((tab) => tab.isBlank()) &&
      this.properties().everyProperty((property) => property.isBlank()) &&
      this.artifacts().everyArtifact((artifact) => artifact.isBlank());
  };

  this.isRunOnAllAgents = function () {
    return this.runInstanceCount() === 'all';
  };

  this.isRunOnOneAgent = function () {
    return _.isNil(this.runInstanceCount());
  };

  this.isRunOnSomeAgents = function () {
    if (s.isBlank(this.runInstanceCount())) {
      return false;
    }
    return s.isPositiveInteger(this.runInstanceCount());
  };

  this.isTimeoutNever = function () {
    return this.timeout() === 'never';
  };

  this.isTimeoutDefault = function () {
    return _.isNil(this.timeout());
  };

  this.isTimeoutCustom = function () {
    if (s.isBlank(this.timeout())) {
      return false;
    }
    return s.isPositiveInteger(this.timeout());
  };

  this.validatePresenceOf('name');
  this.validateUniquenessOf('name');
  this.validateWith('timeout', TimeoutValidator);
  this.validateWith('runInstanceCount', RunInstanceCountValidator);
  this.validateAssociated('environmentVariables');
  this.validateAssociated('tasks');
  this.validateAssociated('artifacts');
  this.validateAssociated('tabs');
  this.validateAssociated('properties');
  this.requiresElasticAgent = function () {
    return !_.isNil(this.elasticProfileId());
  };
};

Jobs.Job.create = (data) => new Jobs.Job(data);

Mixins.fromJSONCollection({
  parentType: Jobs,
  childType:  Jobs.Job,
  via:        'addJob'
});

Jobs.Job.fromJSON = (data) => new Jobs.Job({
  name:                 data.name,
  runInstanceCount:     data.run_instance_count,
  timeout:              data.timeout,
  resources:            data.resources,
  environmentVariables: EnvironmentVariables.fromJSON(data.environment_variables),
  tasks:                Tasks.fromJSON(data.tasks),
  artifacts:            Artifacts.fromJSON(data.artifacts),
  tabs:                 Tabs.fromJSON(data.tabs),
  properties:           Properties.fromJSON(data.properties),
  elasticProfileId:     data.elastic_profile_id,
  errors:               data.errors
});

module.exports = Jobs;
