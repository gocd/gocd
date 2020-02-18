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
import Stream from "mithril/stream";

interface PermissionJSON {
  show_notification: boolean;
}

export class DataSharingNotificationPermission {

  static API_VERSION: ApiVersion = ApiVersion.v1;
  public showNotification: Stream<boolean>;

  constructor(permission: PermissionJSON) {
    this.showNotification = Stream(permission.show_notification);
  }

  static get() {
    const url = "/go/api/data_sharing/settings/notification_auth";

    return ApiRequestBuilder.GET(url, this.API_VERSION)
                            .then((result: ApiResult<string>) => {
                              return result.map((body) => {
                                return DataSharingNotificationPermission.fromJSON(JSON.parse(body) as PermissionJSON);
                              });
                            });
  }

  static fromJSON(json: PermissionJSON) {
    return new DataSharingNotificationPermission(json);
  }
}
