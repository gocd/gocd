/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import {ArtifactStore, ArtifactStoreJSON} from "models/artifact_stores/artifact_stores";
import {ArtifactStoresCRUD} from "models/artifact_stores/artifact_stores_crud";
import {ArtifactStoreTestData} from "models/artifact_stores/spec/test_data";

describe("ArtifactStoreCRUD", () => {
  beforeEach(() => jasmine.Ajax.install());
  afterEach(() => jasmine.Ajax.uninstall());

  it("should make get request", () => {
    jasmine.Ajax.stubRequest("/go/api/admin/artifact_stores").andReturn(listArtifactStoreResponse());

    ArtifactStoresCRUD.all();

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual("/go/api/admin/artifact_stores");
    expect(request.method).toEqual("GET");
    expect(request.data()).toEqual(toJSON({} as ArtifactStoreJSON));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
  });

  it("should create a new artifact store", () => {
    jasmine.Ajax.stubRequest("/go/api/admin/artifact_stores").andReturn(artifactStoreResponse());

    ArtifactStoresCRUD.create(ArtifactStore.fromJSON(ArtifactStoreTestData.dockerArtifactStore()));

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual("/go/api/admin/artifact_stores");
    expect(request.method).toEqual("POST");
    expect(request.data()).toEqual(toJSON(ArtifactStoreTestData.dockerArtifactStore()));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
    expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
  });

  it("should update a artifact store", () => {
    const dockerArtifactStore = ArtifactStoreTestData.dockerArtifactStore();
    jasmine.Ajax.stubRequest(`/go/api/admin/artifact_stores/${dockerArtifactStore.id}`)
           .andReturn(artifactStoreResponse());

    ArtifactStoresCRUD.update(ArtifactStore.fromJSON(dockerArtifactStore), "some-etag");

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(`/go/api/admin/artifact_stores/${dockerArtifactStore.id}`);
    expect(request.method).toEqual("PUT");
    expect(request.data()).toEqual(toJSON(dockerArtifactStore));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
    expect(request.requestHeaders["Content-Type"]).toEqual("application/json; charset=utf-8");
    expect(request.requestHeaders["If-Match"]).toEqual("some-etag");
  });

  it("should delete a artifact store", () => {
    const dockerArtifactStore = ArtifactStoreTestData.dockerArtifactStore();
    jasmine.Ajax.stubRequest(`/go/api/admin/artifact_stores/${dockerArtifactStore.id}`)
           .andReturn(deleteArtifactStoreResponse());

    ArtifactStoresCRUD.delete(dockerArtifactStore.id);

    const request = jasmine.Ajax.requests.mostRecent();
    expect(request.url).toEqual(`/go/api/admin/artifact_stores/${dockerArtifactStore.id}`);
    expect(request.method).toEqual("DELETE");
    expect(request.data()).toEqual(toJSON({} as ArtifactStoreJSON));
    expect(request.requestHeaders.Accept).toEqual("application/vnd.go.cd.v1+json");
    expect(request.requestHeaders["Content-Type"]).toEqual(undefined!);
    expect(request.requestHeaders["X-GoCD-Confirm"]).toEqual("true");
  });

});

function toJSON(object: any) {
  return JSON.parse(JSON.stringify(object));
}

function artifactStoreResponse() {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
      "ETag": "some-etag"
    },
    responseText: JSON.stringify(ArtifactStoreTestData.dockerArtifactStore())
  };
}

function listArtifactStoreResponse() {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
      "ETag": "some-etag"
    },
    responseText: JSON.stringify(ArtifactStoreTestData.artifactStoreList(ArtifactStoreTestData.dockerArtifactStore()))
  };
}

function deleteArtifactStoreResponse() {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8",
      "ETag": "some-etag"
    },
    responseText: JSON.stringify({message: "The artifact store successfully deleted."})
  };
}
