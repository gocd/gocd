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

const $                    = require('jquery');
const m                    = require('mithril');
const Stream               = require('mithril/stream');
const Resources            = require('models/pipeline_configs/resources');
const Users                = require('models/pipeline_configs/users');
const Roles                = require('models/pipeline_configs/roles');
const Pipelines            = require('models/pipeline_configs/pipelines');
const PipelineConfigWidget = require('views/pipeline_configs/pipeline_config_widget');
const PluginInfos          = require('models/shared/plugin_infos');
const SCMs                 = require('models/pipeline_configs/scms');
const ElasticProfiles      = require('models/elastic_profiles/elastic_profiles');
const VersionUpdater       = require('models/shared/version_updater');
require('foundation-sites');

$(() => {
  const pipelineConfigElem = $('#pipeline-config');
  const url                = pipelineConfigElem.attr('data-pipeline-api-url');
  const allResourceNames   = JSON.parse(pipelineConfigElem.attr('data-resource-names'));
  const allUserNames       = JSON.parse(pipelineConfigElem.attr('data-user-names'));
  const allRoleNames       = JSON.parse(pipelineConfigElem.attr('data-role-names'));

  Resources.initializeWith(allResourceNames);
  Users.initializeWith(allUserNames);
  Roles.initializeWith(allRoleNames);
  new VersionUpdater().update();
  Promise.all([PluginInfos.all(), SCMs.init(), ElasticProfiles.all(), Pipelines.all()]).then((args) => {
    m.mount(pipelineConfigElem.get(0), PipelineConfigWidget({
      url:             Stream(url),
      elasticProfiles: Stream(args[2]),
      pluginInfos:     Stream(args[0]),
      pipelines:       Stream(args[3])
    }));
    $(document).foundation();
  });

});
