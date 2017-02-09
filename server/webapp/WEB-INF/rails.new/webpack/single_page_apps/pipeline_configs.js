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

var $                    = require('jquery');
var m                    = require('mithril');
var Stream               = require('mithril/stream');
var PluggableTasks       = require('models/pipeline_configs/pluggable_tasks');
var Resources            = require('models/pipeline_configs/resources');
var Users                = require('models/pipeline_configs/users');
var Roles                = require('models/pipeline_configs/roles');
var PipelineConfigWidget = require('views/pipeline_configs/pipeline_config_widget');
var PluginInfos          = require('models/pipeline_configs/plugin_infos');
var PluggableSCMs        = require('models/pipeline_configs/pluggable_scms');
var SCMs                 = require('models/pipeline_configs/scms');
var ElasticProfiles      = require('models/elastic_profiles/elastic_profiles');
var VersionUpdater       = require('models/shared/version_updater');
require('foundation-sites');

$(function () {
  var pipelineConfigElem = $('#pipeline-config');
  var url                = pipelineConfigElem.attr('data-pipeline-api-url');
  var allResourceNames   = JSON.parse(pipelineConfigElem.attr('data-resource-names'));
  var allUserNames       = JSON.parse(pipelineConfigElem.attr('data-user-names'));
  var allRoleNames       = JSON.parse(pipelineConfigElem.attr('data-role-names'));

  Resources.initializeWith(allResourceNames);
  Users.initializeWith(allUserNames);
  Roles.initializeWith(allRoleNames);
  new VersionUpdater().update();
  Promise.all([PluginInfos.init(), SCMs.init(), ElasticProfiles.all()]).then(function (args) {

    PluggableTasks.init();
    PluggableSCMs.init();

    m.mount(pipelineConfigElem.get(0), PipelineConfigWidget({url: Stream(url), elasticProfiles: Stream(args[2])}));
    $(document).foundation();
  });

});
