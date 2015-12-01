/*
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

define([
  'mithril', 'lodash', 'string-plus',
  './model_mixins',
  './environment_variables', './tasks', './artifacts', './tabs', './properties'
], function (m, _, s,
             Mixins,
             EnvironmentVariables, Tasks, Artifacts, Tabs, Properties) {

  var Jobs = function (data) {
    Mixins.HasMany.call(this, {factory: Jobs.Job.create, as: 'Job', collection: data, uniqueOn: 'name'});
  };

  Jobs.Job = function (data) {
    this.constructor.modelType = 'job';
    Mixins.HasUUID.call(this);

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

    this.isRunOnAllAgents = function () {
      return this.runInstanceCount() == 'all';
    };

    this.isRunOnOneAgent = function () {
      return s.undefinedOrNull(this.runInstanceCount());
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
      return s.undefinedOrNull(this.timeout());
    };

    this.isTimeoutCustom = function () {
      if (s.isBlank(this.timeout())) {
        return false;
      }
      return s.isPositiveInteger(this.timeout());
    };

    this.validate = function () {
      var errors = new Mixins.Errors();

      if (s.isBlank(this.name())) {
        errors.add('name', Mixins.ErrorMessages.mustBePresent('name'));
      } else {
        if (this.parent()) {
          this.parent().validateUniqueJobName(this, errors);
        }
      }

      if (!(this.isTimeoutNever() || this.isTimeoutDefault() || this.isTimeoutCustom())) {
        errors.add('timeout', Mixins.ErrorMessages.mustBePositiveNumber('timeout'));
      }

      if (!(this.isRunOnAllAgents() || this.isRunOnOneAgent() || this.isRunOnSomeAgents())) {
        errors.add('runInstanceCount', Mixins.ErrorMessages.mustBePositiveNumber('runInstanceCount'));
      }

      return errors;
    };
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
      properties:           Properties.fromJSON(data.properties)
    });
  };

  return Jobs;
});
