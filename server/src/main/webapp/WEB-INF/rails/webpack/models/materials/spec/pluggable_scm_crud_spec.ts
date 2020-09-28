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

import {ApiResult, ObjectWithEtag, SuccessResponse} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {OriginType} from "models/origin";
import {getPluggableScm} from "views/pages/pluggable_scms/spec/test_data";
import {Scm, ScmJSON, Scms} from "../pluggable_scm";
import {PluggableScmCRUD} from "../pluggable_scm_crud";

describe('PluggableScmCRUDSpec', () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  it("should get all scms", (done) => {
    const url = SparkRoutes.pluggableScmPath();
    jasmine.Ajax.stubRequest(url).andReturn(scmsResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      const scms         = (responseJSON.body as Scms);

      expect(scms).toHaveLength(1);
      expect(scms[0].id()).toBe('scm-id');
      expect(scms[0].name()).toBe('pluggable.scm.material.name');
      expect(scms[0].pluginMetadata().id()).toBe('scm-plugin-id');
      expect(scms[0].pluginMetadata().version()).toBe('1');

      expect(scms[0].configuration().count()).toBe(1);
      done();
    });

    PluggableScmCRUD.all().then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  it('should get the specified smc', (done) => {
    const url = SparkRoutes.pluggableScmPath("scm-id");
    jasmine.Ajax.stubRequest(url).andReturn(scmResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON      = response.unwrap() as SuccessResponse<any>;
      const packageRepository = (responseJSON.body as ObjectWithEtag<Scm>).object;

      expect(packageRepository.id()).toBe('scm-id');
      expect(packageRepository.name()).toBe('pluggable.scm.material.name');
      expect(packageRepository.origin().type()).toBe(OriginType.GoCD);
      expect(packageRepository.pluginMetadata().id()).toBe('scm-plugin-id');
      expect(packageRepository.pluginMetadata().version()).toBe('1');

      expect(packageRepository.configuration().count()).toBe(1);
      done();
    });

    PluggableScmCRUD.get("scm-id").then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("GET");
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
  });

  it("should create a new scm", () => {
    const url = SparkRoutes.pluggableScmPath();
    jasmine.Ajax.stubRequest(url).andReturn(scmResponse());

    const scm = Scm.fromJSON(getPluggableScm());
    PluggableScmCRUD.create(scm);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("POST");
    expect(request.data()).toEqual(toJSON(scm.toJSON()));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
    expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
  });

  it("should update a scm", () => {
    const url = SparkRoutes.pluggableScmPath("pluggable.scm.material.name");
    jasmine.Ajax.stubRequest(url).andReturn(scmResponse());

    const scm = Scm.fromJSON(getPluggableScm());
    PluggableScmCRUD.update(scm, "old-etag");

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("PUT");
    expect(request.data()).toEqual(toJSON(scm.toJSON()));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
    expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
  });

  it("should delete a scm", () => {
    const url = SparkRoutes.pluggableScmPath("scm-id");
    jasmine.Ajax.stubRequest(url).andReturn(deleteScmResponse());

    PluggableScmCRUD.delete("scm-id");

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("DELETE");
    expect(request.data()).toEqual(toJSON({} as ScmJSON));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
    expect(request.requestHeaders["Content-Type"]).toEqual(undefined!);
    expect(request.requestHeaders["X-GoCD-Confirm"]).toEqual("true");
  });

  it("should verify connection for a scm", (done) => {
    const url = SparkRoutes.pluggableScmCheckConnectionPath();
    jasmine.Ajax.stubRequest(url).andReturn(scmCheckConnectionResponse());

    const scmJSON    = getPluggableScm();
    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      const responseJSON = response.unwrap() as SuccessResponse<any>;
      const parsed       = responseJSON.body;

      expect(parsed.status).toBe('success');
      expect(parsed.messages).toEqual(['message 1', 'message 2']);
      expect(parsed.scm).toEqual(scmJSON);
      done();
    });

    const scm = Scm.fromJSON(scmJSON);
    PluggableScmCRUD.checkConnection(scm).then(onResponse);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(url);
    expect(request.method).toEqual("POST");
    expect(request.data()).toEqual(toJSON(scm.toJSON()));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd+json");
    expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
  });

  function toJSON(object: any) {
    return JSON.parse(JSON.stringify(object));
  }

  function scmsResponse() {
    const scms = {
      _embedded: {
        scms: [getPluggableScm()]
      }
    };
    return {
      status:          200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v3+json; charset=utf-8",
      },
      responseText:    JSON.stringify(scms)
    };
  }

  function scmResponse() {
    return {
      status:          200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v3+json; charset=utf-8",
        "ETag":         "some-etag"
      },
      responseText:    JSON.stringify(getPluggableScm())
    };
  }

  function deleteScmResponse() {
    return {
      status:          200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v3+json; charset=utf-8"
      },
      responseText:    JSON.stringify({message: "The scm was successfully deleted."})
    };
  }

  function scmCheckConnectionResponse() {
    const response = {
      status:   "success",
      messages: ["message 1", "message 2"],
      scm:      getPluggableScm()
    };
    return {
      status:          200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
        "ETag":         "some-etag"
      },
      responseText:    JSON.stringify(response)
    };
  }
});
