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

const AjaxHelper = require('helpers/ajax_helper');

const UsageData           = require('models/shared/data_sharing/usage_data');
const DataReporting       = require('models/shared/data_sharing/data_reporting');
const DataSharingSettings = require('models/shared/data_sharing/data_sharing_settings');

const USAGE_DATA_LAST_REPORTED_TIME_KEY = "usage_data_last_reported_time";

const isUsageDataReportedToday = function (lastReportedTime) {
  if (lastReportedTime === null) {
    return false;
  }

  const now          = new Date();
  const lastReported = new Date(+lastReportedTime);

  return (now.getDate() === lastReported.getDate() &&
    now.getMonth() === lastReported.getMonth() &&
    now.getFullYear() === lastReported.getFullYear());
};

const reportToGoCDDataSharingServer = function (url, data) {
  return AjaxHelper.POST({url, data});
};

const canReportUsageData = () => {
  const lastReportedTime = localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY);
  return !isUsageDataReportedToday(lastReportedTime);
};

const markReportingDone = (reportedTime) => {
  localStorage.setItem(USAGE_DATA_LAST_REPORTED_TIME_KEY, `${reportedTime}`);
};

const UsageDataReporter = function () {
  this.report = async () => {
    if (!canReportUsageData()) {
      return;
    }

    const settings = await DataSharingSettings.get();
    if (!settings.allowed()) {
      return markReportingDone(new Date().getTime());
    }

    const dataReporting = await DataReporting.get();

    const lastReportedTime = dataReporting.lastReportedAt().getTime();
    if (isUsageDataReportedToday(lastReportedTime)) {
      return markReportingDone(lastReportedTime);
    }

    const usageData              = await UsageData.get();
    const latestDataReportedTime = new Date();
    await reportToGoCDDataSharingServer(dataReporting.dataSharingServerUrl(), JSON.parse(usageData.represent()));

    dataReporting.lastReportedAt(latestDataReportedTime);

    await dataReporting.save();
    markReportingDone(latestDataReportedTime.getTime());
  };
};

module.exports = UsageDataReporter;
