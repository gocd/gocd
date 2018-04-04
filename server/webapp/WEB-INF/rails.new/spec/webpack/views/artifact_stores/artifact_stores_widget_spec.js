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

describe('Artifact Stores Widget', () => {

  const $ = require("jquery");
  const m = require("mithril");

  require('jasmine-jquery');
  require('jasmine-ajax');

  const ArtifactStoresWidget = require("views/artifact_stores/artifact_stores_widget");

  let $root, root; //eslint-disable-line no-unused-vars
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(() => {
    window.destroyDomElementForTest();
    m.mount(root, null);
    m.redraw();
  });

  const url            = '/go/api/admin/artifact_stores';
  const pluginInfosUrl = '/go/api/admin/plugin_info?type=artifact';

  describe('Loading Data', () => {

    it('should show error on failure', () => {
      jasmine.Ajax.install();
      jasmine.Ajax.stubRequest(url, undefined, 'GET').andReturn({
        responseText: '',
        status:       500
      });

      m.mount(root, ArtifactStoresWidget);

      expect($(".alert")).toBeInDOM();
    });

    it('should show spinner while data is loading', () => {
      jasmine.Ajax.install();
      jasmine.Ajax.stubRequest(url, undefined, 'GET');

      m.mount(root, ArtifactStoresWidget);

      expect($(".page-spinner")).toBeInDOM();
    });

    it('should show data after response is received', () => {
      jasmine.Ajax.install();
      stubGetArtifactStoresResponse();
      jasmine.Ajax.stubRequest(pluginInfosUrl, undefined, 'GET').andReturn({
        responseText: JSON.stringify({}),
        status:       200
      });

      m.mount(root, ArtifactStoresWidget);

      expect($(".header-panel")).toBeInDOM();
    });
  });

  describe('On data received', () => {
    it('should disable Add button when no plugins are present', () => {
      jasmine.Ajax.install();
      stubGetArtifactStoresResponse();
      jasmine.Ajax.stubRequest(pluginInfosUrl, undefined, 'GET').andReturn({
        responseText: JSON.stringify({
          "_embedded": {
            "plugin_info": []
          }
        }),
        status:       200
      });

      m.mount(root, ArtifactStoresWidget);

      expect($(".add-artifact-store")).toHaveAttr("disabled");
    });

    it('should enable Add button when plugins are present', () => {
      jasmine.Ajax.install();
      stubGetArtifactStoresResponse();
      jasmine.Ajax.stubRequest(pluginInfosUrl, undefined, 'GET').andReturn({
        responseText: JSON.stringify({
          "_embedded": {
            "plugin_info": [{
              "id":     "cd.go.artifact.example",
              "status": {
                "state": "active"
              }
            }]
          }
        }),
        status:       200
      });

      m.mount(root, ArtifactStoresWidget);

      expect($(".add-artifact-store")).not.toHaveAttr("disabled");
    });
  });

  function stubGetArtifactStoresResponse() {
    jasmine.Ajax.stubRequest(url, undefined, 'GET').andReturn({
      responseText: JSON.stringify(sampleResponse),
      status:       200
    });
  }

  const sampleResponse = {
    "_links":    {
      "self": {
        "href": "http://localhost:8253/go/api/admin/artifact_stores"
      },
      "doc":  {
        "href": "https://api.gocd.org/current/#artifact_stores"
      },
      "find": {
        "href": "http://localhost:8253/go/api/admin/artifact_stores/:id"
      }
    },
    "_embedded": {
      "artifact_stores": [
        {
          "_links":     {
            "self": {
              "href": "http://localhost:8253/go/api/admin/artifact_stores/A1"
            },
            "doc":  {
              "href": "https://api.gocd.org/current/#artifact_stores"
            },
            "find": {
              "href": "http://localhost:8253/go/api/admin/artifact_stores/:id"
            }
          },
          "id":         "A1",
          "plugin_id":  "cd.go.artifact.docker.registry",
          "properties": [
            {
              "key":   "RegistryURL",
              "value": "http://example.com/updated"
            },
            {
              "key":   "Username",
              "value": "username-new"
            },
            {
              "key":             "Password",
              "encrypted_value": "kB+k1qd0ddB9RrtMqBnqHQ=="
            }
          ]
        }
      ]
    }
  };
});
