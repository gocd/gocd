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

require(['jquery', 'mithril',
  'views/template_configs/templates_config_widget',
  'views/template_configs/template_config_widget',
  'views/template_configs/new_template_config_widget',
  'models/elastic_profiles/elastic_profiles',
  'models/shared/users', 'models/shared/roles',
  'foundation.util.mediaQuery', 'foundation.dropdownMenu', 'foundation.responsiveToggle', 'foundation.dropdown'
], function ($, m,
             TemplatesConfigWidget, TemplateConfigWidget, NewTemplateConfigWidget,
             ElasticProfiles, Users, Roles) {

  $(function () {
    var templatesConfigElem = $('#templates');
    var allUserNames        = JSON.parse(templatesConfigElem.attr('data-user-names'));
    var allRoleNames        = JSON.parse(templatesConfigElem.attr('data-role-names'));
    var isUserAdmin         = JSON.parse(templatesConfigElem.attr('is-current-user-an-admin'));

    Users.initializeWith(allUserNames);
    Roles.initializeWith(allRoleNames);

    m.sync([ElasticProfiles.all()]).then(function (args) {

      m.route.mode        = "hash";
      var elasticProfiles = m.prop(args[0]);

      m.route(templatesConfigElem.get(0), '', {
        '':               m.component(TemplatesConfigWidget, {isUserAdmin: isUserAdmin}),
        '/:templateName': m.component(TemplateConfigWidget, {elasticProfiles: elasticProfiles, isUserAdmin: isUserAdmin}),
        '/:new': m.component(NewTemplateConfigWidget, {elasticProfiles: elasticProfiles})
      });

      $(document).foundation();
    });

  });
});
