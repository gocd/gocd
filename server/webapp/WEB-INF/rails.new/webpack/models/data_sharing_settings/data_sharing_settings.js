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

const Stream      = require('mithril/stream');
const AjaxHelper  = require('helpers/ajax_helper');
const SparkRoutes = require("helpers/spark_routes");

const DataSharingSettings = function (data, etag) {
  const settings = this;

  let _etag              = etag;
  let _originallyAllowed = data._embedded.allow;
  settings.allowed       = Stream(data._embedded.allow);
  settings.updatedBy     = Stream(data._embedded.updated_by);

  settings.toggleConsent = () => {
    settings.allowed(!settings.allowed());
  };

  settings.resetConsent = () => {
    settings.allowed(_originallyAllowed);
  };

  settings.save = () => {
    return AjaxHelper.PATCH({
      url:        SparkRoutes.metricsDataSharingSettingsPath(),
      apiVersion: DataSharingSettings.API_VERSION,
      payload:    {allow: settings.allowed()},
      etag:       _etag
    }).then((data, _textStatus, jqXHR) => {
      _etag              = jqXHR.getResponseHeader('etag');
      _originallyAllowed = data._embedded.allow;

      settings.allowed(data._embedded.allow);
      settings.updatedBy(data._embedded.updated_by);
    });
  };
};

DataSharingSettings.fromJSON = function (json, jqXHR) {
  return new DataSharingSettings(json, jqXHR.getResponseHeader('etag'));
};

DataSharingSettings.API_VERSION = 'v1';

DataSharingSettings.get = () => {
  return AjaxHelper.GET({
    url:        SparkRoutes.metricsDataSharingSettingsPath(),
    apiVersion: DataSharingSettings.API_VERSION,
    type:       DataSharingSettings
  });
};

module.exports = DataSharingSettings;
