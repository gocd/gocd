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
Routes.apiv1ShowMetricsSettingsPath  = () => {
  return '/go/api/metrics/settings';
};
Routes.apiv1PatchMetricsSettingsPath = () => {
  return '/go/api/metrics/settings';
};


const MetricsSettings = function (data) {
  const settings = this;

  let _originalConsent = data.consent;
  settings.consent     = Stream(data.consent);
  settings.consentedBy = Stream(data.consented_by);

  settings.toggleConsent = () => {
    settings.consent(!settings.consent())
  };

  settings.resetConsent = () => {
    settings.consent(_originalConsent)
  };

  settings.save = () => {
    return AjaxHelper.PATCH({
      url:        Routes.apiv1PatchMetricsSettingsPath(),
      apiVersion: MetricsSettings.API_VERSION,
      payload:    {consent: settings.consent()}
    }).then((data) => {
      settings.consent(data.consent);
      settings.consentedBy(data.consented_by);
      _originalConsent = data.consent;
    });
  }
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
