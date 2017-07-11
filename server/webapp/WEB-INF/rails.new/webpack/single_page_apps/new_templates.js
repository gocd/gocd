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

const $                       = require('jquery');
const m                       = require('mithril');
const Stream                  = require('mithril/stream');
const TemplatesConfigWidget   = require('views/template_configs/templates_config_widget');
const TemplateConfigWidget    = require('views/template_configs/template_config_widget');
const NewTemplateConfigWidget = require('views/template_configs/new_template_config_widget');
const ElasticProfiles         = require('models/elastic_profiles/elastic_profiles');
const PluginInfos             = require('models/shared/plugin_infos');
const Users                   = require('models/shared/users');
const Roles                   = require('models/shared/roles');

require('foundation-sites');

$(() => {
  const templatesConfigElem = $('#templates');
  const allUserNames        = JSON.parse(templatesConfigElem.attr('data-user-names'));
  const allRoleNames        = JSON.parse(templatesConfigElem.attr('data-role-names'));
  const isUserAdmin         = JSON.parse(templatesConfigElem.attr('is-current-user-an-admin'));

  Users.initializeWith(allUserNames);
  Roles.initializeWith(allRoleNames);

  Promise.all([PluginInfos.all(), ElasticProfiles.all()]).then((args) => {
    m.route.prefix("#");

    const pluginInfos     = Stream(args[0]);
    const elasticProfiles = Stream(args[1]);

    m.route(templatesConfigElem.get(0), '', {
      '':               {
        view () {
          return m(TemplatesConfigWidget, {isUserAdmin});
        }
      },
      '/create/new':    {
        view () {
          return m(NewTemplateConfigWidget, {
            elasticProfiles,
            pluginInfos
          });
        }
      },
      '/:templateName': {
        view () {
          return m(TemplateConfigWidget, {
            elasticProfiles,
            pluginInfos,
            isUserAdmin
          });
        }
      }
    });

    $(document).foundation();
  });

});
