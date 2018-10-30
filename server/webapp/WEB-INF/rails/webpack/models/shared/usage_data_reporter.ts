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

import * as _ from 'lodash';
import {DataReporting} from "./data_sharing/data_reporting";
import {EncryptedData, EncryptionKeys, UsageData} from "./data_sharing/usage_data";

import AjaxHelper from "helpers/ajax_helper";

const USAGE_DATA_LAST_REPORTED_TIME_KEY = "last_usage_data_reporting_check_time";

const fetchEncryptionKeysFromDataSharingServer = (url: string) => {
  return AjaxHelper.GET({url});
};

const reportToGoCDDataSharingServer = (url: string, payload: EncryptedData) => {
  return AjaxHelper.POST({url, payload, contentType: 'application/octet-stream'});
};

const canTryToReportingUsageData = (): boolean => {
  let lastReportedTime: string | null = localStorage.getItem(USAGE_DATA_LAST_REPORTED_TIME_KEY);
  if (_.isEmpty(lastReportedTime)) {
    return true;
  }

  lastReportedTime   = JSON.parse(lastReportedTime as string);
  const lastUpdateAt = new Date(lastReportedTime as string);
  const halfHourAgo  = new Date(_.now() - 30 * 60 * 1000);
  return halfHourAgo > lastUpdateAt;
};

const markReportingCheckDone = (): void => {
  localStorage.setItem(USAGE_DATA_LAST_REPORTED_TIME_KEY, `${new Date().getTime()}`);
};

export class UsageDataReporter {
  static report = async () => {
    if (!canTryToReportingUsageData()) {
      return;
    }

    const reportingInfo: DataReporting = await DataReporting.get();

    try {
      if (reportingInfo.canReport()) {
        await DataReporting.startReporting();
        const encryptionKeys: EncryptionKeys    = await fetchEncryptionKeysFromDataSharingServer(reportingInfo.dataSharingGetEncryptionKeysUrl());
        const encryptedUsageData: EncryptedData = await UsageData.getEncrypted(encryptionKeys);

        await reportToGoCDDataSharingServer(reportingInfo.dataSharingServerUrl(), encryptedUsageData);
        await DataReporting.completeReporting();
      }
    } finally {
      markReportingCheckDone();
    }
  }
}
