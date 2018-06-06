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

const $ = require('jquery');
const m = require('mithril');

const VersionUpdater  = require('models/shared/version_updater');
const MetricsSettings = require('models/metrics_consent/metrics_settings');

const PageLoadError        = require('views/shared/page_load_error');
const MetricsConsentWidget = require('views/metrics_consent/metrics_consent_widget');

require('foundation-sites');
require('helpers/server_health_messages_helper');

$(() => {
  new VersionUpdater().update();

  const container = $("#metrics-consent").get(0);

  const onSuccess = (metricsSettings) => {
    m.mount(container, {
      view() {
        return (<MetricsConsentWidget metricsSettings={metricsSettings}/>);
      }
    });

    $(document).foundation();
  };

  const onFailure = () => {
    //todo: blame Ganeshpl for this
    //return onSuccess(new MetricsSettings({consent: true, consented_by: 'Ganeshpl'}));

    m.mount(container, {
      view() {
        return (<PageLoadError message="There was a problem fetching metrics consent information"/>);
      }
    });
  };

  MetricsSettings.get().then(onSuccess, onFailure);
});


