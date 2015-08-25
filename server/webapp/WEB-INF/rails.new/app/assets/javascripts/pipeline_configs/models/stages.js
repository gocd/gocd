/*
 * Copyright 2015 ThoughtWorks, Inc.
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

define(['mithril', 'lodash', 'string-plus', './model_mixins', './jobs', './environment_variables'], function (m, _, s, Mixins, Jobs, EnvironmentVariables) {

  var Stages = function (data) {
    Mixins.HasMany.call(this, {factory: Stages.Stage.create, as: 'Stage', collection: data, uniqueOn: 'name'});
  };

  Stages.Stage = function (data) {
    this.constructor.modelType = 'stage';
    Mixins.HasUUID.call(this);

    this.parent = Mixins.GetterSetter();

    this.name                  = m.prop(s.defaultToIfBlank(data.name, ''));
    this.fetchMaterials        = m.prop(data.fetchMaterials);
    this.cleanWorkingDirectory = m.prop(data.cleanWorkingDirectory);
    this.neverCleanArtifacts   = m.prop(data.neverCleanArtifacts);
    this.environmentVariables  = s.overrideToJSON(m.prop(s.defaultToIfBlank(data.environmentVariables, new EnvironmentVariables())));
    this.jobs                  = s.overrideToJSON(m.prop(s.defaultToIfBlank(data.jobs, new Jobs())));

    this.validate = function () {
      var errors = new Mixins.Errors();

      if (s.isBlank(this.name())) {
        errors.add('name', Mixins.ErrorMessages.mustBePresent('name'));
      } else {
        this.parent().validateUniqueStageName(this, errors);
      }


      return errors;
    };
  };

  Stages.Stage.create = function (data) {
    return new Stages.Stage(data);
  };

  Mixins.fromJSONCollection({
    parentType: Stages,
    childType:  Stages.Stage,
    via:        'addStage'
  });

  Stages.Stage.fromJSON = function (data) {
    return new Stages.Stage({
      name:                  data.name,
      fetchMaterials:        data.fetch_materials,
      cleanWorkingDirectory: data.clean_working_directory,
      neverCleanArtifacts:   data.never_clean_artifacts,
      environmentVariables:  EnvironmentVariables.fromJSON(data.environment_variables),
      jobs:                  Jobs.fromJSON(data.jobs)
    });
  };

  return Stages;
});
