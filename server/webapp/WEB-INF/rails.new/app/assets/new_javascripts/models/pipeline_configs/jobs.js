/*
 * Copyright 2016 ThoughtWorks, Inc.
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

define([
  'mithril', 'lodash', 'string-plus', 'models/model_mixins', 'models/pipeline_configs/environment_variables',
  'models/pipeline_configs/tasks', 'models/pipeline_configs/artifacts', 'models/pipeline_configs/tabs',
  'models/pipeline_configs/properties', 'models/validatable_mixin'
], function (m, _, s, Mixins, EnvironmentVariables, Tasks, Artifacts, Tabs, Properties, Validatable) {

  var Jobs = function (data) {
    Mixins.HasMany.call(this, {factory: Jobs.Job.create, as: 'Job', collection: data, uniqueOn: 'name'});
  };

  var TimeoutValidator = function () {
    this.validate = function (entity) {
      if (!(entity.isTimeoutNever() || entity.isTimeoutDefault() || entity.isTimeoutCustom())) {
        entity.errors().add('timeout', Validatable.ErrorMessages.mustBePositiveNumber('timeout'));
      }
    };
  };

  var RunInstanceCountValidator = function () {
    this.validate = function (entity) {
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

    this.name                 = m.prop(s.defaultToIfBlank(data.name, ''));
    this.runInstanceCount     = m.prop(data.runInstanceCount);
    this.timeout              = m.prop(data.timeout);
    this.resources            = s.withNewJSONImpl(m.prop(s.defaultToIfBlank(data.resources, '')), s.stringToArray);
    this.environmentVariables = s.collectionToJSON(m.prop(s.defaultToIfBlank(data.environmentVariables, new EnvironmentVariables())));
    this.tasks                = s.collectionToJSON(m.prop(s.defaultToIfBlank(data.tasks, new Tasks())));
    this.artifacts            = s.collectionToJSON(m.prop(s.defaultToIfBlank(data.artifacts, new Artifacts())));
    this.tabs                 = s.collectionToJSON(m.prop(s.defaultToIfBlank(data.tabs, new Tabs())));
    this.properties           = s.collectionToJSON(m.prop(s.defaultToIfBlank(data.properties, new Properties())));
    this.elasticProfileId     = m.prop(s.defaultToIfBlank(data.elasticProfileId, null));

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
  };

  Jobs.Job.create = function (data) {
    return new Jobs.Job(data);
  };

  Mixins.fromJSONCollection({
    parentType: Jobs,
    childType:  Jobs.Job,
    via:        'addJob'
  });

  Jobs.Job.fromJSON = function (data) {
    return new Jobs.Job({
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
  };

  return Jobs;
});
