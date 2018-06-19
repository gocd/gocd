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

const Stream = require('mithril/stream');

const AjaxHelper  = require('helpers/ajax_helper');
const SparkRoutes = require("helpers/spark_routes");

const DataReporting = function (initialData, etag) {
  const reporting = this;

  let _etag  = etag;
  const data = initialData._embedded;

  reporting.serverId             = () => data.server_id;
  reporting.lastReportedAt       = Stream(new Date(data.last_reported_at));
  reporting.dataSharingServerUrl = () => data.data_sharing_server_url;


  reporting.save = () => {
    return AjaxHelper.PATCH({
      url:        SparkRoutes.DataReportingPath(),
      apiVersion: DataReporting.API_VERSION,
      payload:    {'last_reported_at': reporting.lastReportedAt().getTime()},
      etag:       _etag
    }).then((data, _textStatus, jqXHR) => {
      _etag = jqXHR.getResponseHeader('etag');
      reporting.lastReportedAt(new Date(data._embedded.last_reported_at));
    });
  };

};

DataReporting.fromJSON = function (json, jqXHR) {
  return new DataReporting(json, jqXHR.getResponseHeader('etag'));
};

DataReporting.API_VERSION = 'v1';

DataReporting.get = () => {
  return AjaxHelper.GET({
    url:        SparkRoutes.DataReportingPath(),
    apiVersion: DataReporting.API_VERSION,
    type:       DataReporting
  });
};

module.exports = DataReporting;
