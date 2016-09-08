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

define(['models/agents/resources'], function (Resources) {
  describe('Resources Model', function () {

    beforeAll(function () {
      jasmine.Ajax.install();
      jasmine.Ajax.stubRequest(/\/api\/admin\/internal\/resources/).andReturn({
        "responseText": JSON.stringify(["Firefox", "Linux"]),
        "status":       200
      });
    });

    afterAll(function () {
      jasmine.Ajax.uninstall();
    });

    it("should initialize the resources", function () {
      Resources.init();
      expect(Resources.list().length).toBe(2);
      expect(Resources.list()[0].name()).toBe('Firefox');
      expect(Resources.list()[1].name()).toBe('Linux');
    });

    it("should initialize the resources with state depending upon the checkedAgents", function () {
      var checkedAgents = [{
        resources: function () {
          return ['Linux'];
        }
      }, {
        resources: function () {
          return ['Linux', 'Firefox'];
        }
      }];
      Resources.init(checkedAgents);
      expect(Resources.list().length).toBe(2);
      expect(Resources.list()[0].name()).toBe('Firefox');
      expect(Resources.list()[1].name()).toBe('Linux');

      expect(Resources.list()[0].isChecked()).toBe(false);
      expect(Resources.list()[0].isIndeterminate()).toBe(true);
      expect(Resources.list()[1].isChecked()).toBe(true);
      expect(Resources.list()[1].isIndeterminate()).toBe(false);
    });
  });
});
