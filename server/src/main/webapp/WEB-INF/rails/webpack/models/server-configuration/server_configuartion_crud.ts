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

import {ApiRequestBuilder, ApiResult, ApiVersion, ObjectWithEtag} from "helpers/api_request_builder";
import {JsonUtils} from "helpers/json_utils";
import {SparkRoutes} from "helpers/spark_routes";
import {
  ArtifactConfig,
  DefaultJobTimeout,
  MailServer,
  SiteUrls
} from "models/server-configuration/server_configuration";

export class ServerManagementCRUD {
  private static API_VERSION_HEADER = ApiVersion.v1;

  static get() {
    return ApiRequestBuilder.GET(SparkRoutes.siteUrlsPath(), this.API_VERSION_HEADER)
                            .then(this.extractObjectWithEtag);
  }

  static put(updatedSiteUrls: SiteUrls, etag: string | undefined) {
    return ApiRequestBuilder.PUT(SparkRoutes.siteUrlsPath(),
                                 this.API_VERSION_HEADER,
                                 {
                                   payload: {
                                     site_url:        updatedSiteUrls.siteUrl(),
                                     secure_site_url: updatedSiteUrls.secureSiteUrl()
                                   }, etag
                                 }).then(this.extractObjectWithEtag);
  }

  private static extractObjectWithEtag(result: ApiResult<string>) {
    return result.map((body) => {
      const siteUrlsJSON = JSON.parse(body); //as siteUrlsJSON;
      return {
        object: SiteUrls.fromJSON(siteUrlsJSON),
        etag:   result.getEtag()
      } as ObjectWithEtag<SiteUrls>;
    });
  }
}

export class ArtifactConfigCRUD {
  private static API_VERSION_HEADER = ApiVersion.v1;

  static get() {
    return ApiRequestBuilder.GET(SparkRoutes.artifactConfigPath(), this.API_VERSION_HEADER)
                            .then(this.extractObjectWithEtag);
  }

  static put(artifactConfig: ArtifactConfig, etag: string | undefined) {
    return ApiRequestBuilder.PUT(SparkRoutes.artifactConfigPath(),
                                 this.API_VERSION_HEADER,
                                 {payload: artifactConfig.toJSON(), etag})
                            .then(this.extractObjectWithEtag);
  }

  private static extractObjectWithEtag(result: ApiResult<string>) {
    return result.map((body) => {
      const artifactConfigJSON = JSON.parse(body);
      return {
        object: ArtifactConfig.fromJSON(artifactConfigJSON),
        etag:   result.getEtag()
      } as ObjectWithEtag<ArtifactConfig>;
    });
  }
}

export class JobTimeoutManagementCRUD {
  private static API_VERSION_HEADER = ApiVersion.v1;

  static get() {
    return ApiRequestBuilder.GET(SparkRoutes.jobTimeoutPath(), this.API_VERSION_HEADER)
                            .then(this.extractJobTimeout);
  }

  static createOrUpdate(defaultJobTimeout: DefaultJobTimeout) {
    return ApiRequestBuilder.POST(SparkRoutes.jobTimeoutPath(),
                                  this.API_VERSION_HEADER,
                                  {payload: defaultJobTimeout.toJSON()})
                            .then(this.extractJobTimeout);
  }

  private static extractJobTimeout(result: ApiResult<string>) {
    return result.map((body) => {
      return DefaultJobTimeout.fromJSON(JSON.parse(body));
    });
  }
}

export class MailServerCrud {
  private static API_VERSION_HEADER = ApiVersion.v1;

  static get() {
    return ApiRequestBuilder.GET(SparkRoutes.mailServerConfigPath(), this.API_VERSION_HEADER)
                            .then(this.extractMailServer());
  }

  static createOrUpdate(mailServerConfig: MailServer) {
    return ApiRequestBuilder.POST(SparkRoutes.mailServerConfigPath(),
                                  this.API_VERSION_HEADER,
                                  {payload: JsonUtils.toSnakeCasedObject(mailServerConfig)})
                            .then(this.extractMailServer());
  }

  static delete() {
    return ApiRequestBuilder.DELETE(SparkRoutes.mailServerConfigPath(), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => JSON.parse(body)));
  }

  static testMail(mailServerConfig: MailServer) {
    return ApiRequestBuilder.POST(SparkRoutes.testMailForMailServerConfigPath(), this.API_VERSION_HEADER,
                                  {payload: JsonUtils.toSnakeCasedObject(mailServerConfig)})
                            .then((result: ApiResult<string>) => result.map((body) => JSON.parse(body)));
  }

  private static extractMailServer() {
    return (result: ApiResult<string>) => result.map((body) => {
      return MailServer.fromJSON(JsonUtils.toCamelCasedObject(JSON.parse(body)));
    });
  }

}
