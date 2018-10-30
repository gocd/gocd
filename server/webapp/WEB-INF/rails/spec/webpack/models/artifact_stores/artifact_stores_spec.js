/*
 * Copyright 2018 ThoughtWorks, Inc.
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
describe('Artifact Stores', () => {

  const _ = require('lodash');
  const s = require('string-plus');

  const ArtifactStores = require('models/artifact_stores/artifact_stores');

  const url = '/go/api/admin/artifact_stores';

  const artifactStoreJSON = {
    "id":         "foo",
    "plugin_id":  "cd.go.artifact.docker",
    "properties": [
      {
        "key":   "RegistryURL",
        "value": "http://foo"
      }
    ]
  };

  const allArtifactStoresJSON = {
    "_embedded": {
      "artifact_stores": [artifactStoreJSON]
    }
  };

  it('should deserialize a store from JSON', () => {
    const store = ArtifactStores.ArtifactStore.fromJSON(artifactStoreJSON);
    expect(store.id()).toBe("foo");
    expect(store.pluginId()).toBe("cd.go.artifact.docker");
    expect(store.properties().collectConfigurationProperty('key')).toEqual(['RegistryURL']);
    expect(store.properties().collectConfigurationProperty('value')).toEqual(['http://foo']);
  });

  it('should serialize a store to JSON', () => {
    const store = ArtifactStores.ArtifactStore.fromJSON(artifactStoreJSON);
    expect(JSON.parse(JSON.stringify(store, s.snakeCaser))).toEqual(artifactStoreJSON);
  });

  describe("list all artifact stores", () => {
    it('should get all artifact stores and call the success callback', () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(url).andReturn({
          responseText:    JSON.stringify(allArtifactStoresJSON),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const successCallback = jasmine.createSpy().and.callFake((stores) => {
          expect(stores.countArtifactStore()).toBe(1);
          expect(stores.firstArtifactStore().id()).toBe("foo");
          expect(stores.firstArtifactStore().pluginId()).toBe("cd.go.artifact.docker");
        });

        ArtifactStores.all().then(successCallback);
        expect(successCallback).toHaveBeenCalled();
      });
    });

    it('should get all artifact stores and call the error callback on error', () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(url).andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          400,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy();

        ArtifactStores.all().then(_.noop, errorCallback);
        expect(errorCallback).toHaveBeenCalledWith('Boom!');
      });
    });
  });

  describe("update an artifact store", () => {
    it('should update an artifact store and call success callback', () => {
      const artifactStore = ArtifactStores.ArtifactStore.fromJSON(artifactStoreJSON);
      artifactStore.etag("some-etag");
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`${url}/${artifactStore.id()}`, undefined, 'PUT').andReturn({
          responseText:    JSON.stringify({id: 'gocd', 'plugin_id': 'docker'}),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });
        const successCallback = jasmine.createSpy();

        artifactStore.update().then(successCallback);

        expect(successCallback).toHaveBeenCalled();
        expect(successCallback.calls.mostRecent().args[0].id()).toEqual('gocd');
        expect(successCallback.calls.mostRecent().args[0].pluginId()).toEqual('docker');

        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('PUT');
        expect(request.url).toBe(`${url}/${artifactStore.id()}`);
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(request.requestHeaders['If-Match']).toBe('some-etag');
        expect(JSON.parse(request.params)).toEqual(artifactStoreJSON);
      });
    });

    it('should update an artifact store and call error callback on error', () => {
      const store = ArtifactStores.ArtifactStore.fromJSON(artifactStoreJSON);
      store.etag("some-etag");

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`${url}/${store.id()}`, undefined, 'PUT').andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          400,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });
        const errorCallback = jasmine.createSpy();

        store.update().then(_.noop, errorCallback);

        expect(errorCallback).toHaveBeenCalledWith('Boom!');
        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('PUT');
        expect(request.url).toBe(`${url}/${store.id()}`);
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(request.requestHeaders['If-Match']).toBe('some-etag');
        expect(JSON.parse(request.params)).toEqual(artifactStoreJSON);
      });
    });
  });

  describe("create an artifact store", () => {
    it("should create an artifact store and call the success callback", () => {
      const store = ArtifactStores.ArtifactStore.fromJSON(artifactStoreJSON);

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(url, undefined, 'POST').andReturn({
          responseText:    JSON.stringify(artifactStoreJSON),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const successCallback = jasmine.createSpy();

        store.create().then(successCallback);

        expect(successCallback).toHaveBeenCalled();
        expect(successCallback.calls.mostRecent().args[0].id()).toEqual(artifactStoreJSON['id']);
        expect(successCallback.calls.mostRecent().args[0].pluginId()).toEqual(artifactStoreJSON['plugin_id']);

        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('POST');
        expect(JSON.parse(request.params)).toEqual(artifactStoreJSON);
        expect(request.url).toBe(url);
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(JSON.parse(request.params)).toEqual(artifactStoreJSON);
      });

    });

    it("should not create an artifact store and call the error callback on non-422 failure code", () => {
      const store = ArtifactStores.ArtifactStore.fromJSON(artifactStoreJSON);

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(url, undefined, 'POST').andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          400,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });
        const errorCallback = jasmine.createSpy();

        store.create().then(_.noop, errorCallback);

        expect(errorCallback).toHaveBeenCalledWith('Boom!');
        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('POST');
        expect(JSON.parse(request.params)).toEqual(artifactStoreJSON);
        expect(request.url).toBe(url);
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(JSON.parse(request.params)).toEqual(artifactStoreJSON);
      });
    });

    it("should not create a artifact store and call the error callback on 422 failure code with the artifact store object", () => {
      const store = ArtifactStores.ArtifactStore.fromJSON(artifactStoreJSON);

      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(url, undefined, 'POST').andReturn({
          responseText:    JSON.stringify({data: artifactStoreJSON}),
          status:          422,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy().and.callFake((storeWithErrors) => {
          expect(storeWithErrors.id()).toBe(store.id());
          expect(storeWithErrors.pluginId()).toBe(store.pluginId());
        });

        store.create().then(_.noop, errorCallback);

        expect(errorCallback).toHaveBeenCalled();
        expect(jasmine.Ajax.requests.count()).toBe(1);

        const request = jasmine.Ajax.requests.mostRecent();

        expect(request.method).toBe('POST');
        expect(JSON.parse(request.params)).toEqual(artifactStoreJSON);
        expect(request.url).toBe(url);
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
        expect(JSON.parse(request.params)).toEqual(artifactStoreJSON);
      });
    });
  });

  describe("find an artifact store", () => {
    it('should find an artifact store and call the success callback', () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`${url}/${artifactStoreJSON['id']}`, undefined, 'GET').andReturn({
          responseText:    JSON.stringify(artifactStoreJSON),
          responseHeaders: {
            ETag:           'some-etag',
            'Content-Type': 'application/vnd.go.cd.v1+json'
          },
          status:          200
        });

        const successCallback = jasmine.createSpy().and.callFake((store) => {
          expect(store.id()).toBe("foo");
          expect(store.pluginId()).toBe("cd.go.artifact.docker");
          expect(store.properties().collectConfigurationProperty('key')).toEqual(['RegistryURL']);
          expect(store.properties().collectConfigurationProperty('value')).toEqual(['http://foo']);
          expect(store.etag()).toBe("some-etag");
        });

        ArtifactStores.ArtifactStore.get(artifactStoreJSON['id']).then(successCallback);

        expect(successCallback).toHaveBeenCalled();

        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('GET');
        expect(request.url).toBe(`${url}/${artifactStoreJSON['id']}`);
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
      });
    });

    it("should find an artifact store and call the error callback on error", () => {
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`${url}/${artifactStoreJSON['id']}`, undefined, 'GET').andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          400,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });


        const failureCallback = jasmine.createSpy();

        ArtifactStores.ArtifactStore.get(artifactStoreJSON['id']).then(_.noop, failureCallback);

        expect(failureCallback.calls.argsFor(0)[0]).toBe('Boom!');

        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('GET');
        expect(request.url).toBe(`${url}/${artifactStoreJSON['id']}`);
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
      });
    });
  });

  describe('delete an artifact store', () => {
    it("should call the success callback with the message", () => {
      const store = ArtifactStores.ArtifactStore.fromJSON(artifactStoreJSON);
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`${url}/${store.id()}`).andReturn({
          responseText:    JSON.stringify({message: 'Success!'}),
          status:          200,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const successCallback = jasmine.createSpy();

        store.delete().then(successCallback);

        expect(successCallback).toHaveBeenCalledWith('Success!');
        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('DELETE');
        expect(request.url).toBe(`${url}/${store.id()}`);
      });
    });

    it("should call the error callback with the message", () => {
      const store = ArtifactStores.ArtifactStore.fromJSON(artifactStoreJSON);
      jasmine.Ajax.withMock(() => {
        jasmine.Ajax.stubRequest(`${url}/${store.id()}`).andReturn({
          responseText:    JSON.stringify({message: 'Boom!'}),
          status:          422,
          responseHeaders: {
            'Content-Type': 'application/vnd.go.cd.v1+json'
          }
        });

        const errorCallback = jasmine.createSpy();
        store.delete().then(_.noop, errorCallback);

        expect(errorCallback).toHaveBeenCalledWith('Boom!');

        expect(jasmine.Ajax.requests.count()).toBe(1);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('DELETE');
        expect(request.url).toBe(`${url}/${store.id()}`);
      });
    });
  });

  describe('Validate artifact store id', () => {
    let artifactStore;
    beforeEach(() => {
      artifactStore = ArtifactStores.ArtifactStore.fromJSON(artifactStoreJSON);
    });

    it('should return empty object on valid artifact store id', () => {
      artifactStore.id("foo.bar-baz_bar");

      const result = artifactStore.validate();

      expect(result._isEmpty()).toEqual(true);
    });

    it('should return error if artifact store id contains `!`', () => {
      artifactStore.id("foo!bar");

      const result = artifactStore.validate();

      expect(result._isEmpty()).toEqual(false);
      expect(result.errors().id[0]).toContain('Invalid id.');

    });

    it('should return error if artifact store id contains `*`', () => {
      artifactStore.id("foo*bar");

      const result = artifactStore.validate();

      expect(result._isEmpty()).toEqual(false);
      expect(result.errors().id[0]).toContain('Invalid id.');

    });

    it('should return error if artifact store id starts with `.`', () => {
      artifactStore.id(".foo");

      const result = artifactStore.validate();

      expect(result._isEmpty()).toEqual(false);
      expect(result.errors().id[0]).toContain('Invalid id.');

    });
  });
});
