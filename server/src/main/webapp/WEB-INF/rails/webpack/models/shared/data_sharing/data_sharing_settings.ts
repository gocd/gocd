/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import {SparkRoutes} from "helpers/spark_routes";
import {timeFormatter as TimeFormatter} from "helpers/time_formatter";
import Stream from "mithril/stream";

const AjaxHelper = require("helpers/ajax_helper").AjaxHelper;

interface DataSharingInfo {
  "allow": boolean;
  "updated_by": string;
  "updated_on": string;
}

export interface DataSharingJSON {
  _embedded: DataSharingInfo;
}

export class DataSharingSettings {

  static API_VERSION = 'v1';
  public allowed: Stream<boolean>;
  public updatedBy: Stream<string>;
  public updatedOn: Stream<string>;
  private etag: string;
  private originallyAllowed: boolean;

  constructor(data: DataSharingJSON, etag: string) {
    this.etag              = etag;
    this.originallyAllowed = data._embedded.allow;

    this.allowed   = Stream(data._embedded.allow);
    this.updatedBy = Stream(data._embedded.updated_by);
    this.updatedOn = Stream(TimeFormatter.formatInDate(data._embedded.updated_on));
  }

  static fromJSON(json: DataSharingJSON, jqXHR: any) {
    return new DataSharingSettings(json, jqXHR.getResponseHeader('etag'));
  }

  static get = () => {
    return AjaxHelper.GET({
      url:        SparkRoutes.DataSharingSettingsPath(),
      apiVersion: DataSharingSettings.API_VERSION,
      type:       DataSharingSettings
    });
  }

  hasEverChangedByAdmin = (): boolean => {
    return this.updatedBy() !== 'Default';
  }

  toggleConsent = (): void => {
    this.allowed(!this.allowed());
  }

  resetConsent = (): void => {
    this.allowed(this.originallyAllowed);
  }

  save = () => {
    return AjaxHelper.PATCH({
      url:        SparkRoutes.DataSharingSettingsPath(),
      apiVersion: DataSharingSettings.API_VERSION,
      payload:    {allow: this.allowed()},
      etag:       this.etag
    }).then((data: DataSharingJSON, textStatus: string, jqXHR: any) => {
      this.etag              = jqXHR.getResponseHeader('etag');
      this.originallyAllowed = data._embedded.allow;

      this.allowed(data._embedded.allow);
      this.updatedBy(data._embedded.updated_by);
    });
  }
}
