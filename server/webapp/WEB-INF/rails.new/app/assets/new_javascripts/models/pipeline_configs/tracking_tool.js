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

define(['mithril', 'lodash', 'string-plus', 'models/model_mixins', 'models/validatable_mixin'], function (m, _, s, Mixins, Validatable) {

  var UrlPatternValidator = function () {
    this.validate = function (entity) {
      if (!s.include(entity.urlPattern(), '${ID}')) {
        entity.errors().add('urlPattern', Validatable.ErrorMessages.mustContainString("urlPattern", '${ID}'));
      }
    };
  };

  var TrackingTool = function (type) {
    this.constructor.modelType = 'trackingTool';
    Mixins.HasUUID.call(this);

    this.type = m.prop(type);

    var self = this;

    this.toJSON = function () {
      return {
        type:       self.type(),
        attributes: self._attributesToJSON()
      };
    };

    this._attributesToJSON = function () {
      throw new Error("Subclass responsibility!");
    };
  };

  TrackingTool.Generic = function (data) {
    TrackingTool.call(this, "generic");
    Validatable.call(this, data);
    this.urlPattern = m.prop(s.defaultToIfBlank(data.urlPattern, ''));
    this.regex      = m.prop(s.defaultToIfBlank(data.regex, ''));

    this.validatePresenceOf('urlPattern');
    this.validateUrlPattern('urlPattern');
    this.validateWith('urlPattern', UrlPatternValidator);
    this.validatePresenceOf('regex');

    this._attributesToJSON = function () {
      return {
        urlPattern: this.urlPattern(),
        regex:      this.regex()
      };
    };
  };

  TrackingTool.Generic.fromJSON = function (data) {
    return new TrackingTool.Generic({
      urlPattern: data.url_pattern,
      regex:      data.regex
    });
  };

  TrackingTool.Mingle = function (data) {
    TrackingTool.call(this, "mingle");
    Validatable.call(this, data);
    this.baseUrl               = m.prop(s.defaultToIfBlank(data.baseUrl, ''));
    this.projectIdentifier     = m.prop(s.defaultToIfBlank(data.projectIdentifier, ''));
    this.mqlGroupingConditions = m.prop(s.defaultToIfBlank(data.mqlGroupingConditions, ''));

    this.validatePresenceOf('baseUrl');
    this.validateUrlPattern('baseUrl');
    this.validatePresenceOf('projectIdentifier');
    this.validatePresenceOf('mqlGroupingConditions');

    this._attributesToJSON = function () {
      return {
        baseUrl:               this.baseUrl(),
        projectIdentifier:     this.projectIdentifier(),
        mqlGroupingConditions: this.mqlGroupingConditions()
      };
    };

  };

  TrackingTool.Mingle.fromJSON = function (data) {
    return new TrackingTool.Mingle({
      baseUrl:               data.base_url,
      projectIdentifier:     data.project_identifier,
      mqlGroupingConditions: data.mql_grouping_conditions
    });
  };

  TrackingTool.Types = {
    generic: {type: TrackingTool.Generic, description: "Generic"},
    mingle:  {type: TrackingTool.Mingle, description: "Mingle"}
  };

  TrackingTool.create = function (type) {
    return new TrackingTool.Types[type].type({});
  };

  TrackingTool.fromJSON = function (data) {
    if (!_.isEmpty(data)) {
      return TrackingTool.Types[data.type].type.fromJSON(data.attributes || {});
    }
  };
  return TrackingTool;
});
