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

define(['mithril', 'lodash', 'string-plus', './model_mixins', './errors'], function (m, _, s, Mixins, Errors) {
  var URL_REGEX = /^http(s)?:\/\/.+/;

  var TrackingTool = function (type, data) {
    this.constructor.modelType = 'trackingTool';
    Mixins.HasUUID.call(this);

    this.type = m.prop(type);
    this.errors     = m.prop(s.defaultToIfBlank(data.errors, new Errors()));
    var self = this;

    this.toJSON = function () {
      return {
        type:       self.type(),
        attributes: self._attributesToJSON(),
        errors:     self.errors().errors()
      };
    };

    this._attributesToJSON = function () {
      throw new Error("Subclass responsibility!");
    };
  };

  TrackingTool.Generic = function (data) {
    TrackingTool.call(this, "generic", data);
    this.urlPattern = m.prop(s.defaultToIfBlank(data.urlPattern, ''));
    this.regex      = m.prop(s.defaultToIfBlank(data.regex, ''));

    this.validate = function () {
      var errors = new Errors();

      if (s.isBlank(this.urlPattern())) {
        errors.add('urlPattern', Mixins.ErrorMessages.mustBePresent('URL pattern'));
      } else {
        if (!this.urlPattern().match(URL_REGEX)) {
          errors.add("urlPattern", Mixins.ErrorMessages.mustBeAUrl("urlPattern"));
        }

        if (!s.include(this.urlPattern(), '${ID}')) {
          errors.add("urlPattern", Mixins.ErrorMessages.mustContainString("urlPattern", '${ID}'));
        }
      }


      if (s.isBlank(this.regex())) {
        errors.add('regex', Mixins.ErrorMessages.mustBePresent('regular expression'));
      }

      return errors;
    };

    this._attributesToJSON = function () {
      return {
        urlPattern: this.urlPattern(),
        regex:      this.regex()
      };
    };
  };

  TrackingTool.Generic.fromJSON = function (data) {
    var attributes = data.attributes;
    return new TrackingTool.Generic({
      urlPattern: attributes.url_pattern,
      regex:      attributes.regex,
      errors:     Errors.fromJson(data)
    });
  };

  TrackingTool.Mingle = function (data) {
    TrackingTool.call(this, "mingle", data);
    this.baseUrl               = m.prop(s.defaultToIfBlank(data.baseUrl, ''));
    this.projectIdentifier     = m.prop(s.defaultToIfBlank(data.projectIdentifier, ''));
    this.mqlGroupingConditions = m.prop(s.defaultToIfBlank(data.mqlGroupingConditions, ''));

    this.validate = function () {
      var errors = new Errors();

      if (s.isBlank(this.baseUrl())) {
        errors.add('baseUrl', Mixins.ErrorMessages.mustBePresent('Base URL'));
      } else if (!this.baseUrl().match(URL_REGEX)) {
        errors.add("baseUrl", Mixins.ErrorMessages.mustBeAUrl("baseUrl"));
      }

      if (s.isBlank(this.projectIdentifier())) {
        errors.add('projectIdentifier', Mixins.ErrorMessages.mustBePresent('projectIdentifier'));
      }

      if (s.isBlank(this.mqlGroupingConditions())) {
        errors.add('mqlGroupingConditions', Mixins.ErrorMessages.mustBePresent('mqlGroupingConditions'));
      }

      return errors;
    };

    this._attributesToJSON = function () {
      return {
        baseUrl:               this.baseUrl(),
        projectIdentifier:     this.projectIdentifier(),
        mqlGroupingConditions: this.mqlGroupingConditions()
      };
    };

  };

  TrackingTool.Mingle.fromJSON = function (data) {
    var attributes = data.attributes;
    return new TrackingTool.Mingle({
      baseUrl:               attributes.base_url,
      projectIdentifier:     attributes.project_identifier,
      mqlGroupingConditions: attributes.mql_grouping_conditions,
      errors:                Errors.fromJson(data)
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
      return TrackingTool.Types[data.type].type.fromJSON(data || {});
    }
  };
  return TrackingTool;
});
