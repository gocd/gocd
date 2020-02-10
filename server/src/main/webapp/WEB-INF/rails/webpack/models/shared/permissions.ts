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

export class Permission {
  public readonly type: SupportedEntity;
  public readonly permission: PermissionJSON;

  constructor(type: SupportedEntity, permission: PermissionJSON) {
    this.type       = type;
    this.permission = permission;
  }

  canAdminister(entity: string): boolean {
    return (this.permission.administer.indexOf(entity) !== -1);
  }

  canView(entity: string): boolean {
    return (this.permission.view.indexOf(entity) !== -1);
  }
}

export interface PermissionsAPIJSON {
  permissions: PermissionsJSON;
}

export enum SupportedEntity {
  environment           = "environment",
  config_repo           = "config_repo",
  cluster_profile       = "cluster_profile",
  elastic_agent_profile = "elastic_agent_profile"
}

export type PermissionsJSON = {
  [key in SupportedEntity]: PermissionJSON;
};

export interface PermissionJSON {
  view: string[];
  administer: string[];
}

export class Permissions extends Array<Permission> {
  private static API_VERSION_HEADER = ApiVersion.v1;

  constructor(permissions: Permission[]) {
    super(...permissions);
    Object.setPrototypeOf(this, Object.create(Permissions.prototype));
  }

  static all(type?: SupportedEntity[]) {
    return ApiRequestBuilder.GET(SparkRoutes.apiPermissionsPath(type), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => {
                              return result.map((body) => {
                                return Permissions.fromJSON(JSON.parse(body) as PermissionsAPIJSON);
                              });
                            });
  }

  static fromJSON(json: PermissionsAPIJSON) {
    // @ts-ignore
    const permissions = Object.keys(json.permissions).map((key: SupportedEntity) => {
      return new Permission(key, json.permissions[key]);
    });

    return new Permissions(permissions);
  }

  for(type: SupportedEntity): Permission {
    return this.find((p) => p.type === type)!;
  }
}
