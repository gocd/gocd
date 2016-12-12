/*
 * Copyright 2016 ThoughtWorks, Inc.
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

define(['lodash', 'mithril', 'string-plus', 'models/template_configs/templates'], function (_, m, s, Templates) {
  describe('Templates', function () {

    var templatesJSON = [
        {
          "_links": {
            "self": {
              "href": "https://ciexample.com/go/api/admin/templates/template1"
            },
            "doc": {
              "href": "https://api.go.cd/#template-config"
            },
            "find": {
              "href": "https://ciexample.com/go/api/admin/templates/:template_name"
            }
          },
          "name": "template1",
          "_embedded": {
            "pipelines": [
              {
                "_links": {
                  "self": {
                    "href": "https://ciexample.com/go/api/admin/pipelines/up42"
                  },
                  "doc": {
                    "href": "https://api.go.cd/#pipeline-config"
                  },
                  "find": {
                    "href": "https://ciexample.com/go/api/admin/pipelines/:pipeline_name"
                  }
                },
                "name": "up42"
              },
              {
                "_links": {
                  "self": {
                    "href": "https://ciexample.com/go/api/admin/pipelines/down42"
                  },
                  "doc": {
                    "href": "https://api.go.cd/#pipeline-config"
                  },
                  "find": {
                    "href": "https://ciexample.com/go/api/admin/pipelines/:pipeline_name"
                  }
                },
                "name": "down42"
              }
            ]
          }
        }
      ];

    describe('Templates.Template', function () {
      describe('deserialization', function () {
        var templates;

        beforeAll(function () {
          templates = Templates.fromJSON(templatesJSON);
        });

        it('should initialize template model with name', function () {
          expect(templates.countTemplate()).toBe(1);
          expect(templates.firstTemplate().name()).toBe('template1');
        });

        it('should initialize template model with self link', function () {
          expect(templates.firstTemplate().url()).toBe('https://ciexample.com/go/api/admin/templates/template1');
        });

        it('should initialize template model with pipelines', function () {
          expect(templates.firstTemplate().pipelines().length).toBe(2);

          expect(templates.firstTemplate().pipelines()[0].name()).toBe('up42');
          expect(templates.firstTemplate().pipelines()[0].url()).toBe('https://ciexample.com/go/api/admin/pipelines/up42');

          expect(templates.firstTemplate().pipelines()[1].name()).toBe('down42');
          expect(templates.firstTemplate().pipelines()[1].url()).toBe('https://ciexample.com/go/api/admin/pipelines/down42');
        })
      });

      describe('delete', function () {
        var template;

        beforeAll(function () {
          template = Templates.fromJSON(templatesJSON).firstTemplate();
          spyOn(m, 'request');
        });

        it('should post a delete request to templates API', function () {
          template.delete();

          requestArgs = m.request.calls.mostRecent().args[0];

          expect(requestArgs.method).toBe('DELETE');
          expect(requestArgs.url).toBe('/go/api/admin/templates/template1');
        });

        it('should post required request headers', function () {
          var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);

          template.delete();

          requestArgs = m.request.calls.mostRecent().args[0];
          requestArgs.config(xhr);

          expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
          expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v3+json");
        });
      });
    });

    describe('Templates.all', function() {
      beforeAll(function () {
        spyOn(m, 'request');
      });

      it('should fetch all templates from templates API', function () {
        Templates.all();

        requestArgs = m.request.calls.mostRecent().args[0];

        expect(requestArgs.method).toBe('GET');
        expect(requestArgs.url).toBe('/go/api/admin/templates');
      });

      it('should post required request headers', function () {
        var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);

        Templates.all();

        requestArgs = m.request.calls.mostRecent().args[0];
        requestArgs.config(xhr);

        expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
        expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v3+json");
      });

      it('should unwrap response data to build templates model', function () {
        Templates.all();

        requestArgs = m.request.calls.mostRecent().args[0];

        var json = {
          "_embedded": {
            "templates": [
              {
                "_links": {
                  "self": {
                    "href": "https://ciexample.com/go/api/admin/templates/template1"
                  }
                },
                "name":   "template1",
                "_embedded": {
                  "pipelines": []
                }
              }
            ]
          }
        };

        var templates = requestArgs.unwrapSuccess(json);

        expect(templates.countTemplate()).toBe(1);
        expect(templates.firstTemplate().name()).toBe('template1');
      });
    });
  });
});