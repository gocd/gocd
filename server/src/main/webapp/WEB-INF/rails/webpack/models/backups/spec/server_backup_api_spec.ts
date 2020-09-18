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

import {ApiResult} from "helpers/api_request_builder";
import {ServerBackupAPI} from "models/backups/server_backup_api";
import {ServerBackup} from "models/backups/types";

describe("ServerBackupAPI", () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  it("should make get request", (done) => {
    jasmine.Ajax.stubRequest("/go/api/backups/12").andReturn(getInProgressServerBackupResponse());

    const onResponse = jasmine.createSpy().and.callFake((response: ApiResult<any>) => {
      // @ts-ignore
      const serverBackup = response.unwrap().body as ServerBackup;
      expect(serverBackup.time.toISOString()).toBe(new Date("2019-02-28T06:46:57Z").toISOString());
      expect(serverBackup.isInProgress()).toBe(true);
      expect(serverBackup.username).toBe("admin");
      expect(serverBackup.message).toBe("Backing up Config.");
      done();
    });
    ServerBackupAPI.get("/go/api/backups/12").then(onResponse);

    expect(() => {
      const request = requestByUrl(jasmine.Ajax.requests, "/go/api/backups/12");
      expect(request.method).toEqual("GET");
      expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v2+json");
    }).not.toThrow();
  });

  it("should call onError when backup fails to start", (done) => {
    jasmine.Ajax.stubRequest("/go/api/backups").andReturn(getApiErrorResponse());

    const onProgress = jasmine.createSpy();
    const onCompletion = jasmine.createSpy();
    const onError = jasmine.createSpy();
    ServerBackupAPI.start(onProgress, onCompletion, onError);

    setTimeout(() => {
      expect(() => {
        const request = requestByUrl(jasmine.Ajax.requests, "/go/api/backups");
        expect(request.method).toEqual("POST");
        expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v2+json");
        expect(onProgress.calls.count()).toEqual(0);
        expect(onCompletion.calls.count()).toEqual(0);
        expect(onError.calls.count()).toEqual(1);
      }).not.toThrow();
      done();

    }, 200);
  });

  it("should call onComplete when backup complete", (done) => {
    jasmine.Ajax.stubRequest("/go/api/backups/12").andReturn(getCompletedServerBackupResponse());

    const onProgress = jasmine.createSpy();
    const onCompletion = jasmine.createSpy();
    const onError = jasmine.createSpy();
    ServerBackupAPI.checkBackupProgress("/go/api/backups/12", onProgress, onCompletion, onError);

    setTimeout(() => {
      expect(() => {
        const request = requestByUrl(jasmine.Ajax.requests, "/go/api/backups/12");
        expect(request.method).toEqual("GET");
        expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v2+json");
        expect(onProgress.calls.count()).toEqual(0);
        expect(onCompletion.calls.count()).toEqual(1);
        expect(onError.calls.count()).toEqual(0);
      }).not.toThrow();
      done();

    }, 200);
  });

  it("should call onComplete when backup fails with error", (done) => {
    jasmine.Ajax.stubRequest("/go/api/backups/12").andReturn(getErrorServerBackupResponse());

    const onProgress = jasmine.createSpy();
    const onCompletion = jasmine.createSpy();
    const onError = jasmine.createSpy();
    ServerBackupAPI.checkBackupProgress("/go/api/backups/12", onProgress, onCompletion, onError);

    setTimeout(() => {
      expect(() => {
        const request = requestByUrl(jasmine.Ajax.requests, "/go/api/backups/12");
        expect(request.method).toEqual("GET");
        expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v2+json");
        expect(onProgress.calls.count()).toEqual(0);
        expect(onCompletion.calls.count()).toEqual(1);
        expect(onError.calls.count()).toEqual(0);
      }).not.toThrow();
      done();

    }, 200);
  });

  it("should call onError when api returns an unknown error", (done) => {
    jasmine.Ajax.stubRequest("/go/api/backups/12").andReturn(getApiErrorResponse());

    const onProgress = jasmine.createSpy();
    const onCompletion = jasmine.createSpy();
    const onError = jasmine.createSpy();
    ServerBackupAPI.checkBackupProgress("/go/api/backups/12", onProgress, onCompletion, onError);

    setTimeout(() => {
      expect(() => {
        const request = requestByUrl(jasmine.Ajax.requests, "/go/api/backups/12");
        expect(request.method).toEqual("GET");
        expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v2+json");
        expect(onProgress.calls.count()).toEqual(0);
        expect(onCompletion.calls.count()).toEqual(0);
        expect(onError.calls.count()).toEqual(1);
      }).not.toThrow();
      done();

    }, 200);
  });

  it("should call onProgress when backup in progress", (done) => {
    jasmine.Ajax.stubRequest("/go/api/backups/12").andReturn(getInProgressServerBackupResponse());

    const onProgress = jasmine.createSpy();
    const onCompletion = jasmine.createSpy();
    const onError = jasmine.createSpy();
    ServerBackupAPI.checkBackupProgress("/go/api/backups/12", onProgress, onCompletion, onError);

    setTimeout(() => {
      expect(() => {
        const request = requestByUrl(jasmine.Ajax.requests, "/go/api/backups/12");
        expect(request.method).toEqual("GET");
        expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v2+json");
        expect(onProgress.calls.count()).toEqual(1);
        expect(onCompletion.calls.count()).toEqual(0);
        expect(onError.calls.count()).toEqual(0);
      }).not.toThrow();
      done();

    }, 200);
  });

  function requestByUrl(requests: JasmineAjaxRequestTracker, url: string) {
    const matches = requests.filter(url);
    if (!matches.length) {
      throw new Error(`Failed to find jasmine mock ajax request with url: ${url}`);
    }

    return matches[0];
  }

  function getCompletedServerBackupResponse() {
    return getServerBackupResponse("COMPLETED");
  }

  function getErrorServerBackupResponse() {
    return getServerBackupResponse("ERROR");
  }

  function getInProgressServerBackupResponse() {
    return getServerBackupResponse("IN_PROGRESS");
  }

  function getApiErrorResponse() {
    return {
      status: 500,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v2+json; charset=utf-8"
      },
      responseText: JSON.stringify({message : "An unknown error occurred on the server"})
    };
  }

  function getServerBackupResponse(status: string) {
    return {
      status: 200,
      responseHeaders: {
        "Content-Type": "application/vnd.go.cd.v2+json; charset=utf-8"
      },
      responseText: JSON.stringify({
                                     _links : {
                                       doc : {
                                         href : "https://api.gocd.org/19.2.0/#backups"
                                       }
                                     },
                                     time : "2019-02-28T06:46:57Z",
                                     path : "/backup/path",
                                     status,
                                     message : "Backing up Config.",
                                     user : {
                                       _links : {
                                         doc : {
                                           href : "https://api.gocd.org/19.2.0/#users"
                                         },
                                         self : {
                                           href : "https://localhost:8154/go/api/users/admin"
                                         },
                                         find : {
                                           href : "https://localhost:8154/go/api/users/:login_name"
                                         },
                                         current_user : {
                                           href : "https://localhost:8154/go/api/current_user"
                                         }
                                       },
                                       login_name : "admin"
                                     }
                                   })
    };
  }
});
