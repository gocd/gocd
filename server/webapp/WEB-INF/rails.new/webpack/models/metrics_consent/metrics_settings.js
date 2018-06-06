/*
 * Copyright 2018 ThoughtWorks, Inc.
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

const Routes     = require('gen/js-routes');
const AjaxHelper = require('helpers/ajax_helper');
const Stream     = require('mithril/stream');

//todo: Blame Ganeshpl for this
Routes.apiv1ShowMetricsSettingsPath = () => {
  return '/go/api/metrics/settings';
};

const MetricsSettings = function (data) {
  const settings       = this;
  settings.consent     = Stream(data.consent);
  settings.consentedBy = Stream(data.consented_by);
};

MetricsSettings.fromJSON = function (json) {
  return new MetricsSettings(json);
};

MetricsSettings.API_VERSION = 'v1';

MetricsSettings.get = () => {
  return AjaxHelper.GET({
    url:        Routes.apiv1ShowMetricsSettingsPath(),
    apiVersion: MetricsSettings.API_VERSION,
    type:       MetricsSettings
  });
};

module.exports = MetricsSettings;
