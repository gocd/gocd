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

var $                     = require('jquery');
var m                     = require('mithril');
var ElasticProfilesWidget = require('views/elastic_profiles/elastic_profiles_widget');
var PluginInfos           = require('models/pipeline_configs/plugin_infos');
var VersionUpdater        = require('models/shared/version_updater');
require('foundation-sites');

$(function () {
  $(document).foundation();
  new VersionUpdater().update();

  var onSuccess = function () {
    m.mount($("#elastic-profiles").get(0), ElasticProfilesWidget);
  };

  var onFailure = function () {
    $("#elastic-profiles").html($('<div class="alert callout">')
      .append('<h5>There was a problem fetching the elastic profiles</h5>')
      .append('<p>Refresh <a href="javascript: window.location.reload()">this page</a> in some time, and if the problem persists, check the server logs.</p>')
    );
  };

  Promise.all([PluginInfos.init('elastic-agent')]).then(onSuccess, onFailure);
});
