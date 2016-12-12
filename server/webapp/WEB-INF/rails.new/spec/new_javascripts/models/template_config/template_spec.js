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

define([
  'lodash', 'mithril', 'string-plus', 'models/template_configs/template', 'models/pipeline_configs/stages'
], function (_, m, s, Template, Stages) {
  describe('Template', function () {

    var templateJSON = {
      name: "template.name",
      is_extracted_from_pipeline: false,
      pipeline: '',
      authorization: {
        admins: {
          "roles": [
            'role1', 'role2'
          ],
          "users": [
            'user1', 'user2'
          ]
        }
      },
      "stages": [
        {
          name:                    "UnitTest",
          fetch_materials:         true,
          clean_working_directory: false,
          never_cleanup_artifacts: true,
          environment_variables:   [
            {
              name:   "MULTIPLE_LINES",
              encrypted_value:  "multiplelines",
              secure: true
            },
            {
              name:   "COMPLEX",
              value:  "This has very <complex> data",
              secure: false
            }
          ],
          jobs:                    [],
          approval:                {
            type:          'manual',
            authorization: {
              users: [],
              roles: []
            }
          }
        }
      ]
    };

    describe('deserialization', function () {
      var template;

      beforeAll(function() {
        template = Template.fromJSON(templateJSON);
      });

      it('should initialize template model with name', function () {
        expect(template.name()).toBe("template.name");
      });

      it('should initialize template model with stages', function () {
        var expectedStages = Stages.fromJSON(templateJSON.stages);
        var actualStages = template.stages();

        expect(actualStages.countStage()).toBe(expectedStages.countStage());
        expect(actualStages.firstStage().name()).toBe(expectedStages.firstStage().name());
      });

      it('should initialize template with authorization', function() {
        expect(template.authorization().admins().users()).toEqual(['user1', 'user2']);
        expect(template.authorization().admins().roles()).toEqual(['role1', 'role2']);
      });

      it("should serialize to JSON", function () {
        expect(JSON.parse(JSON.stringify(template, s.snakeCaser))).toEqual(templateJSON);
      });
    });

    describe('validation', function() {
      it('should validate presence of template name', function () {
        var template = new Template({});

        expect(template.isValid()).toBe(false);
        expect(template.errors().errors('name')).toEqual(['Name must be present']);
      });

      it('should validate stages', function () {
        var template = Template.fromJSON(templateJSON);

        expect(template.isValid()).toBe(true);

        template.stages().firstStage().name('');

        expect(template.isValid()).toBe(false);
        expect(template.stages().firstStage().errors().errors('name')).toEqual(['Name must be present']);
      });
    });

    describe('update', function() {
      var template;
      beforeAll(function () {
        template = Template.fromJSON(templateJSON);
        spyOn(m, 'request');
      });

      it('should post data to templates API', function() {
        template.update();

        requestArgs = m.request.calls.mostRecent().args[0];

        expect(requestArgs.method).toBe('PUT');
        expect(requestArgs.url).toBe('/go/api/admin/templates/template.name');
      });

      it('should post required request headers', function () {
        var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);

        template.update('etag');

        requestArgs = m.request.calls.mostRecent().args[0];
        requestArgs.config(xhr);

        expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
        expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v3+json");
        expect(xhr.setRequestHeader).toHaveBeenCalledWith("If-Match", "etag");
      });

      it('should post serialized template', function() {
        template.update();

        requestArgs = m.request.calls.mostRecent().args[0];

        expect(requestArgs.data).toEqual(JSON.parse(JSON.stringify(template, s.snakeCaser)));
      });
    });

    describe('create', function() {
      var template;
      beforeAll(function () {
        template = Template.fromJSON(templateJSON);
        spyOn(m, 'request');
      });

      it('should post data to templates API', function() {
        template.create();

        requestArgs = m.request.calls.mostRecent().args[0];

        expect(requestArgs.method).toBe('POST');
        expect(requestArgs.url).toBe('/go/api/admin/templates');
      });

      it('should post required request headers', function () {
        var xhr = jasmine.createSpyObj(xhr, ['setRequestHeader']);

        template.create();

        requestArgs = m.request.calls.mostRecent().args[0];
        requestArgs.config(xhr);

        expect(xhr.setRequestHeader).toHaveBeenCalledWith("Content-Type", "application/json");
        expect(xhr.setRequestHeader).toHaveBeenCalledWith("Accept", "application/vnd.go.cd.v3+json");
      });

      it('should post serialized template', function() {
        template.create();

        requestArgs = m.request.calls.mostRecent().args[0];

        expect(requestArgs.data).toEqual(JSON.parse(JSON.stringify(template, s.snakeCaser)));
      });

      it('should unwrap response data to build templates model', function () {
        template.create();

        requestArgs = m.request.calls.mostRecent().args[0];

        var tmp = requestArgs.unwrapSuccess(templateJSON);

        expect(tmp.name()).toBe('template.name');
      });
    });
  });
});