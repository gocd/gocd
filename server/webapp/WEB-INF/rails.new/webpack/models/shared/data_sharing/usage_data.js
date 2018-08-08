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

const AjaxHelper  = require('helpers/ajax_helper');
const SparkRoutes = require("helpers/spark_routes");

const UsageData = function (data) {
  this.message   = () => data;
  this.represent = () => JSON.stringify(this.message(), null, 4);
};

UsageData.fromJSON = function (json) {
  return new UsageData(json);
};

UsageData.API_VERSION = 'v2';

UsageData.get = () => {
  return AjaxHelper.GET({
    url:        SparkRoutes.DataSharingUsageDataPath(),
    apiVersion: UsageData.API_VERSION,
    type:       UsageData
  });
};

UsageData.getEncrypted = (data) => {
  return AjaxHelper.POST({
    url:        SparkRoutes.DataSharingUsageDataEncryptedPath(),
    payload:    data,
    apiVersion: UsageData.API_VERSION
  });
};

module.exports = UsageData;
