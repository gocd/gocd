/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import {ApiRequestBuilder, ApiResult, ApiVersion, ObjectWithEtag} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {BulkUserRoleUpdateJSON, GoCDRole, PluginRole, Role, Roles, RolesWithSuggestions} from "models/roles/roles";

export class RolesCRUD {
  private static API_VERSION_HEADER = ApiVersion.v3;

  static all(type?: 'gocd' | 'plugin') {
    return ApiRequestBuilder.GET(SparkRoutes.rolesPath(type), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => {
                              return Roles.fromJSON(JSON.parse(body));
                            }));
  }

  static allWithAutocompleteSuggestions(type?: 'gocd' | 'plugin') {
    return ApiRequestBuilder.GET(SparkRoutes.internalRolesPath(type), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => {
                              return RolesWithSuggestions.fromJSON(JSON.parse(body));
                            }));
  }

  static get(id: string) {
    return ApiRequestBuilder.GET(SparkRoutes.rolePath(id), this.API_VERSION_HEADER)
                            .then(this.extractObjectWithEtag);
  }

  static create(role: GoCDRole | PluginRole) {
    return ApiRequestBuilder.POST(SparkRoutes.rolesPath(),
                                  this.API_VERSION_HEADER,
                                  {payload: role}).then(this.extractObjectWithEtag);
  }

  static update(updatedRole: GoCDRole | PluginRole, etag: string) {
    return ApiRequestBuilder.PUT(SparkRoutes.rolePath(updatedRole.name()),
                                 this.API_VERSION_HEADER,
                                 {payload: updatedRole, etag})
                            .then(this.extractObjectWithEtag);
  }

  static delete(id: string) {
    return ApiRequestBuilder.DELETE(SparkRoutes.rolePath(id), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => JSON.parse(body)));
  }

  static bulkUserRoleUpdate(bulkUserRoleUpdateJSON: BulkUserRoleUpdateJSON) {
    return ApiRequestBuilder.PATCH(SparkRoutes.rolesPath(), this.API_VERSION_HEADER, {payload: bulkUserRoleUpdateJSON})
                            .then((result: ApiResult<string>) => {
                              return result.map((body) => {
                                // return;
                              });
                            });
  }

  private static extractObjectWithEtag(result: ApiResult<string>) {
    return result.map((body) => {
      const roleJSON = JSON.parse(body); //as RoleJSON;
      return {
        object: Role.fromJSON(roleJSON),
        etag:   result.getEtag()
      } as ObjectWithEtag<GoCDRole | PluginRole>;
    });
  }
}
