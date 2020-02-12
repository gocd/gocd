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
import {SparkRoutes} from 'helpers/spark_routes';

const AjaxHelper = require("helpers/ajax_helper").AjaxHelper;

export interface EncryptionKeys {
  signature: string;
  subordinate_public_key: string;
}

export interface EncryptedData {
  aes_encrypted_data: string;
  rsa_encrypted_aes_key: string;
}

interface ElasticAgentJobInfo {
  plugin_id: string;
  job_count: number;
}

interface UsageDataInfo {
  "pipeline_count": number;
  "config_repo_pipeline_count": number;
  "agent_count": number;
  "oldest_pipeline_execution_time": number;
  "job_count": number;
  "elastic_agent_job_count": ElasticAgentJobInfo[];
  "gocd_version": string;
}

export interface UsageDataJSON {
  "server_id": string;
  "message_version": number;
  "data": UsageDataInfo;
}

export class UsageData {

  static API_VERSION: string = 'v3';
  public message: () => UsageDataJSON;
  public represent: () => string;

  constructor(data: UsageDataJSON) {
    this.message   = () => data;
    this.represent = () => JSON.stringify(this.message(), null, 4);
  }

  static fromJSON(json: UsageDataJSON) {
    return new UsageData(json);
  }

  static get() {
    return AjaxHelper.GET({
      url:        SparkRoutes.DataSharingUsageDataPath(),
      apiVersion: UsageData.API_VERSION,
      type:       UsageData
    });
  }

  static getEncrypted = (data: EncryptionKeys) => {
    return AjaxHelper.POST({
      url:        SparkRoutes.DataSharingUsageDataEncryptedPath(),
      payload:    data,
      apiVersion: UsageData.API_VERSION
    });
  }
}
