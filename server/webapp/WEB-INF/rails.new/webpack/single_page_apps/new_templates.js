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

var $                       = require('jquery');
var m                       = require('mithril');
var Stream                  = require('mithril/stream');
var TemplatesConfigWidget   = require('views/template_configs/templates_config_widget');
var TemplateConfigWidget    = require('views/template_configs/template_config_widget');
var NewTemplateConfigWidget = require('views/template_configs/new_template_config_widget')
var ElasticProfiles         = require('models/elastic_profiles/elastic_profiles');
var Users                   = require('models/shared/users');
var Roles                   = require('models/shared/roles');
require('foundation-sites');

$(function () {
  var templatesConfigElem = $('#templates');
  var allUserNames        = JSON.parse(templatesConfigElem.attr('data-user-names'));
  var allRoleNames        = JSON.parse(templatesConfigElem.attr('data-role-names'));
  var isUserAdmin         = JSON.parse(templatesConfigElem.attr('is-current-user-an-admin'));

  Users.initializeWith(allUserNames);
  Roles.initializeWith(allRoleNames);

  ElasticProfiles.all().then(function (args) {

    m.route.prefix("#");
    var elasticProfiles = Stream(args[0]);

    m.route(templatesConfigElem.get(0), '', {
      '':               {
        view: function () {
          return m(TemplatesConfigWidget, {isUserAdmin: isUserAdmin});
        }
      },
      '/:templateName': {
        view: function () {
          return m(TemplateConfigWidget, {
            elasticProfiles: elasticProfiles,
            isUserAdmin:     isUserAdmin
          });
        }
      }
      //'/:new':          {
      //  view: function () {
      //    return m(NewTemplateConfigWidget, {elasticProfiles: elasticProfiles});
      //  }
      //}
    });

    $(document).foundation();
  });

});
