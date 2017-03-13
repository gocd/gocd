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

const Stream      = require('mithril/stream');
const _           = require('lodash');
const s           = require('string-plus');
const Mixins      = require('models/mixins/model_mixins');
const Validatable = require('models/mixins/validatable_mixin');

const Artifacts = function (data) {
  Mixins.HasMany.call(this, {factory: Artifacts.Artifact.create, as: 'Artifact', collection: data});
};

Artifacts.Artifact = function (data) {
  this.constructor.modelType = 'artifact';
  Mixins.HasUUID.call(this);
  Validatable.call(this, data);

  this.parent = Mixins.GetterSetter();

  this.type        = Stream(s.defaultToIfBlank(data.type, 'build'));
  this.source      = Stream(s.defaultToIfBlank(data.source, ''));
  this.destination = Stream(s.defaultToIfBlank(data.destination, ''));

  this.isBlank = function () {
    return s.isBlank(this.source()) && s.isBlank(this.destination());
  };

  this.validatePresenceOf('source', {
    condition(property) {
      return (!s.isBlank(property.destination()));
    }
  });
};

Artifacts.Artifact.create = (data) => new Artifacts.Artifact(data);

Mixins.fromJSONCollection({
  parentType: Artifacts,
  childType:  Artifacts.Artifact,
  via:        'addArtifact'
});

Artifacts.Artifact.fromJSON = (data) => new Artifacts.Artifact(_.pick(data, ['type', 'source', 'destination']));

module.exports = Artifacts;
