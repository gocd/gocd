/*
 * Copyright 2017 ThoughtWorks, Inc.
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

describe("Read Only Stage Operate Permissions Widget", () => {
  const $ = require("jquery");
  const m = require("mithril");
  require('jasmine-jquery');

  const StagesOperatePermissionsWidget = require("views/pipeline_configs/read_only/stage_operate_permissions_widget");
  const Template             = require("models/pipeline_configs/template");

  let $root, root, template;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  describe("Permissions Specified", () => {
    beforeEach(() => {
      template = Template.fromJSON(rawTemplateJSONWithPermissions());
      mount();
    });

    afterEach(() => {
      unmount();
    });

    it('should render stage operate permissions heading', () => {
      expect($('h5')).toContainText('Stage Operate Permissions:');
    });

    it('should render users', () => {
      const usersList = rawTemplateJSONWithPermissions().stages[0].approval.authorization.users.join(', ');

      expect($root).toContainText('Users:');
      expect($root).toContainText(usersList);
    });

    it('should render roles', () => {
      const rolesList = rawTemplateJSONWithPermissions().stages[0].approval.authorization.roles.join(', ');

      expect($root).toContainText('Roles:');
      expect($root).toContainText(rolesList);
    });
  });

  describe("Permissions Not Specified", () => {
    beforeEach(() => {
      template = Template.fromJSON(rawTemplateJSONWithoutPermissions());
      mount();
    });

    afterEach(() => {
      unmount();
    });

    it('should render stage operate permissions heading', () => {
      expect($('h5')).toContainText('Stage Operate Permissions:');
    });

    it('should render users', () => {
      const notSpecifiedMessage = 'Not specified.';

      expect($root).toContainText('Users:');
      expect($root).toContainText(notSpecifiedMessage);
    });

    it('should render roles', () => {
      const notSpecifiedMessage = 'Not specified.';

      expect($root).toContainText('Roles:');
      expect($root).toContainText(notSpecifiedMessage);
    });
  });

  const mount = function () {
    m.mount(root, {
      view () {
        return m(StagesOperatePermissionsWidget, {stage: template.stages().firstStage});
      }
    });
    m.redraw();
  };

  const unmount = function () {
    m.mount(root, null);
    m.redraw();
  };

  const rawTemplateJSONWithPermissions = function () {
    return {
      "name":   "template.name",
      "stages": [
        {
          "name":     "up42_stage",
          "approval": {
            "authorization": {
              "roles": ['admin', 'view'],
              "users": ['jez', 'john']
            }
          }
        }
      ]
    };
  };

  const rawTemplateJSONWithoutPermissions = function () {
    return {
      "name":   "template.name",
      "stages": [
        {
          "name":     "up42_stage",
          "approval": {
            "authorization": {
              "roles": [],
              "users": []
            }
          }
        }
      ]
    };
  };
});
