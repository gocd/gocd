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

import {Stream} from 'mithril/stream';
import SparkRoutes from '../../../helpers/spark_routes';

const stream     = require('mithril/stream');
const AjaxHelper = require('helpers/ajax_helper');

interface DataReportingInfo {
  server_id: string,
  last_reported_at: number,
  data_sharing_server_url: string,
  data_sharing_get_encryption_keys_url: string,
  can_report: boolean
}

export interface DataReportingJSON {
  _embedded: DataReportingInfo
}

export class DataReporting {
  private _data: DataReportingInfo;
  public lastReportedAt: Stream<Date>;
  public serverId: () => string;
  public dataSharingServerUrl: () => string;
  public dataSharingGetEncryptionKeysUrl: () => string;
  public canReport: () => boolean;

  constructor(initialData: DataReportingJSON) {
    this._data          = initialData._embedded;
    this.lastReportedAt = stream(new Date(this._data.last_reported_at));

    this.serverId                        = () => this._data.server_id;
    this.dataSharingServerUrl            = () => this._data.data_sharing_server_url;
    this.dataSharingGetEncryptionKeysUrl = () => this._data.data_sharing_get_encryption_keys_url;
    this.canReport                       = () => this._data.can_report;
  }

  static fromJSON = function (json: DataReportingJSON): DataReporting {
    return new DataReporting(json);
  };

  static API_VERSION: string = 'v2';

  static get = () => {
    return AjaxHelper.GET({
      url:        SparkRoutes.DataReportingInfoPath(),
      apiVersion: DataReporting.API_VERSION,
      type:       DataReporting
    });
  };

  static startReporting = () => {
    return AjaxHelper.POST({
      url:        SparkRoutes.DataReportingStartReportingPath(),
      apiVersion: DataReporting.API_VERSION
    });
  };

  static completeReporting = () => {
    return AjaxHelper.POST({
      url:        SparkRoutes.DataReportingCompleteReportingPath(),
      apiVersion: DataReporting.API_VERSION
    });
  };

}
