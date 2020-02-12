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

import {ApiRequestBuilder, ApiResult, ApiVersion} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {ServerBackup} from "models/backups/types";

export class ServerBackupAPI {
  private static API_VERSION_HEADER = ApiVersion.v2;

  static start(onProgress: (serverBackup: ServerBackup) => void,
               onCompletion: (serverBackup: ServerBackup) => void,
               onError: (error: string) => void) {

    return ApiRequestBuilder.POST(SparkRoutes.apiCreateServerBackupPath(), this.API_VERSION_HEADER)
                     .then((result: ApiResult<string>) => {
                       result.do(() => {
                         this.startPolling(result.getRedirectUrl(),
                                           result.getRetryAfterIntervalInMillis(),
                                           onProgress,
                                           onCompletion,
                                           onError);
                       }, (errorResponse) => {
                         onError(JSON.parse(errorResponse.body!).message);
                       });
                     });
  }

  static startPolling(pollingUrl: string,
                      retryIntervalMillis: number,
                      onProgress: (serverBackup: ServerBackup) => void,
                      onCompletion: (serverBackup: ServerBackup) => void,
                      onError: (error: string) => void) {

    const onProgressWithRetry = (serverBackup: ServerBackup) => {
      onProgress(serverBackup);
      setTimeout(() => {
        ServerBackupAPI.checkBackupProgress(pollingUrl, onProgressWithRetry, onCompletion, onError);
      }, retryIntervalMillis);
    };
    this.checkBackupProgress(pollingUrl, onProgressWithRetry, onCompletion, onError);
  }

  static getRunningBackups() {
    return this.get(SparkRoutes.apiRunningServerBackupsPath());
  }

  static get(backupUrl: string) {
    return ApiRequestBuilder.GET(backupUrl, this.API_VERSION_HEADER)
                            .then(this.extractServerBackup());
  }

  static checkBackupProgress(pollingUrl: string,
                             onProgress: (serverBackup: ServerBackup) => void,
                             onCompletion: (serverBackup: ServerBackup) => void,
                             onError: (error: string) => void) {
    this.get(pollingUrl).then((result) => {
      result.do((successResponse) => {
        const serverBackup = successResponse.body;
        if (serverBackup.isInProgress()) {
          onProgress(serverBackup);
        } else {
          onCompletion(serverBackup);
        }
      }, (errorResponse) => {
        onError(`Failed to poll for serverBackup. Reason: ${JSON.parse(errorResponse.body!).message}`);
      });
    });
  }

  private static extractServerBackup() {
    return (result: ApiResult<string>) => {
      return result.map((body) => {
        return ServerBackup.fromJSON(JSON.parse(body));
      });
    };
  }

}
