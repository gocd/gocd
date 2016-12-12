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

define(['mithril', 'lodash', 'string-plus', 'helpers/mrequest', 'js-routes', 'models/model_mixins'],
  function (m, _, s, mrequest, Routes, Mixins) {
    var Templates = function (data) {
      Mixins.HasMany.call(this, {
        factory:    Templates.Template.fromJSON,
        as:         'Template',
        collection: data,
        uniqueOn:   'name'
      });
    };

    Templates.Template = function (data) {
      this.parent    = Mixins.GetterSetter();
      this.name      = m.prop(s.defaultToIfBlank(data.name, ''));
      this.url       = m.prop(s.defaultToIfBlank(data.url, ''));
      this.pipelines = m.prop(s.defaultToIfBlank(data.pipelines, []));

      this.delete = function () {
        return m.request({
          method: 'DELETE',
          url:    Routes.apiv3AdminTemplatePath({template_name: data.name}),
          config: mrequest.xhrConfig.v3
        });
      };
    };

    Templates.Template.fromJSON = function (data) {
      return new Templates.Template({
        name:      data.name,
        url:       data._links.self.href,
        pipelines: _.map(data._embedded.pipelines, function (pipeline) {
          return new function () {
            this.name = m.prop(pipeline.name);
            this.url  = m.prop(pipeline._links.self.href);
          };
        })
      });
    };

    Templates.all = function () {
      var unwrap = function (response) {
        return Templates.fromJSON(response._embedded.templates);
      };

      return m.request({
        method:        'GET',
        url:           Routes.apiv3AdminTemplatesPath(),
        config:        mrequest.xhrConfig.v3,
        unwrapSuccess: unwrap,
        unwrapError:   mrequest.unwrapErrorExtractMessage
      });
    };

    Mixins.fromJSONCollection({
      parentType: Templates,
      childType:  Templates.Template,
      via:        'addTemplate'
    });

    return Templates;
  });