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

import {ApiResult, ObjectWithEtag, SuccessResponse} from "../../../helpers/api_request_builder";
import {PipelineGroup} from "../admin_pipelines";
import {PipelineGroupCRUD} from "../pipeline_groups_crud";
import {pipelineGroupJSON} from "./admin_pipelines_spec";

describe('pipelineGroupsCrud', () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  describe('get', () => {
    it("should make a get request", (done) => {
      const GET_PIPELINE_GROUP_API = "/go/api/admin/pipeline_groups/pipeline-group";
      jasmine.Ajax.stubRequest(GET_PIPELINE_GROUP_API).andReturn({
                                                                   status: 200,
                                                                   responseHeaders: {
                                                                     "Content-Type": "application/vnd.go.cd+json; charset=utf-8",
                                                                     "ETag": "some-etag"
                                                                   },
                                                                   responseText: JSON.stringify(
                                                                     pipelineGroupJSON())
                                                                 });

      const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<ObjectWithEtag<PipelineGroup>>) => {
        const responseJSON = response.unwrap() as SuccessResponse<ObjectWithEtag<PipelineGroup>>;
        expect(responseJSON.body.object.name()).toEqual("pipeline-group");
        done();
      });

      PipelineGroupCRUD.get("pipeline-group").then(onResponse);

      const request = jasmine.Ajax.requests.mostRecent();
      expect(request.url).toEqual(GET_PIPELINE_GROUP_API);
      expect(request.method).toEqual("GET");
      expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
    });
  });

  describe('put', () => {
    it("should make a put request", (done) => {
      const pipelineGroup          = PipelineGroup.fromJSON(pipelineGroupJSON());
      const PUT_PIPELINE_GROUP_API = "/go/api/admin/pipeline_groups/pipeline-group";
      jasmine.Ajax.stubRequest(PUT_PIPELINE_GROUP_API, JSON.stringify(pipelineGroupJSON()), "PUT")
             .andReturn({
                          status: 200,
                          responseHeaders: {
                            "Content-Type": "application/vnd.go.cd+json; charset=utf-8",
                            "ETag": "some-etag"
                          },
                          responseText: JSON.stringify(
                            pipelineGroupJSON())
                        });

      const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
        const responseJSON = response.unwrap() as SuccessResponse<ObjectWithEtag<PipelineGroup>>;
        expect(responseJSON.body.object.name()).toEqual("pipeline-group");
        done();
      });

      PipelineGroupCRUD.update(pipelineGroup, "some-etag").then(onResponse);

      const request = jasmine.Ajax.requests.mostRecent();

      expect(request.url).toEqual(PUT_PIPELINE_GROUP_API);
      expect(request.method).toEqual("PUT");
      expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
    });
  });
});
