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

const $                    = require("jquery");
const m                    = require("mithril");
const Stream               = require("mithril/stream");
const TemplateConfigWidget = require("views/template_configs/template_config_widget");
const ElasticProfiles      = require("models/elastic_profiles/elastic_profiles");
const PluginInfos          = require('models/shared/plugin_infos');

describe("TemplateConfigWidget", () => {
  let root;
  beforeEach(() => {
    [, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  const templateJSON = {
    "_links":        {
      "self": {
        "href": "https://ci.example.com/go/api/admin/templates/baz"
      },
      "doc":  {
        "href": "https://api.go.cd/#template-config"
      },
      "find": {
        "href": "https://ci.example.com/go/api/admin/templates/:template_name"
      }
    },
    "name":          "baz",
    "authorization": {
      "admins": {
        "roles": [],
        "users": []
      }
    },
    "stages":        [
      {
        "name":                    "defaultStage",
        "fetch_materials":         true,
        "clean_working_directory": false,
        "never_cleanup_artifacts": false,
        "approval":                {
          "type":          "success",
          "authorization": {
            "roles": [],
            "users": []
          }
        },
        "environment_variables":   [],
        "jobs":                    [
          {
            "name":                  "defaultJob",
            "run_instance_count":    null,
            "timeout":               null,
            "environment_variables": [],
            "resources":             [],
            "tasks":                 [],
            "tabs":                  [],
            "artifacts":             [],
            "properties":            null
          }
        ]
      }
    ]
  };

  const partialTemplateJSON = {
    "name":      "baz",
    "_links":    {
      "self": {
        "href": "https://ci.example.com/api/admin/templates/baz"
      }
    },
    "_embedded": {
      "pipelines": []
    }
  };

  const allTemplatesJSON = {
    "_embedded": {
      "templates": [partialTemplateJSON]
    }
  };

  const profileJSON     = {
    "id":         "unit-tests",
    "plugin_id":  "cd.go.contrib.elastic-agent.docker",
    "properties": [
      {
        "key":   "Image",
        "value": "gocdcontrib/gocd-dev-build"
      },
      {
        "key":   "Environment",
        "value": "JAVA_HOME=/opt/java\nMAKE_OPTS=-j8"
      }
    ]
  };
  const allProfilesJSON = {
    "_embedded": {
      "profiles": [profileJSON]
    }
  };

  beforeEach(() => {
    jasmine.Ajax.install();
    jasmine.Ajax.stubRequest('/go/api/admin/templates/baz', undefined, 'GET').andReturn({
      responseText: JSON.stringify(templateJSON),
      status:       200
    });

    jasmine.Ajax.stubRequest('/go/api/admin/templates', undefined, 'GET').andReturn({
      responseText: JSON.stringify(allTemplatesJSON),
      status:       200
    });

    jasmine.Ajax.stubRequest('/go/api/elastic/profiles').andReturn({
      responseText: JSON.stringify(allProfilesJSON),
      status:       200
    });

    const elasticProfiles = Stream(ElasticProfiles.fromJSON(allProfilesJSON));
    const pluginInfos     = Stream(PluginInfos.fromJSON([]));

    m.mount(root, {
      view () {
        return m(TemplateConfigWidget, {
          elasticProfiles,
          pluginInfos,
          isUserAdmin:  true,
          templateName: templateJSON.name
        });
      }
    });

    m.redraw();
  });

  afterEach(() => {
    jasmine.Ajax.uninstall();

    m.mount(root, null);
    m.redraw(true);
  });

  describe('Headers', () => {
    it('should list the name of the template', () => {
      expect($('.page-header')).toContainText(templateJSON.name);
    });
  });

  describe('Save', () => {
    it('should save the edited template', () => {
      jasmine.Ajax.stubRequest('/go/api/admin/templates/baz', undefined, 'PUT').andReturn({
        responseText: JSON.stringify(templateJSON),
        status:       200
      });

      expect($('.save-template')).not.toHaveClass('success');
      $('.save-template').click();
      m.redraw(true);
      expect($('.save-template')).toHaveClass('success');
    });

    it('shoould show error occured while editing template', () => {
      jasmine.Ajax.stubRequest('/go/api/admin/templates/baz', undefined, 'PUT').andReturn({
        responseText: JSON.stringify({message: "Boom!"}),
        status:       500
      });
      expect($('.save-template')).not.toHaveClass('alert');

      $('.save-template').click();
      m.redraw(true);

      expect($('.template-body')).toContainText("Boom!");
      expect($('.save-template')).toHaveClass('alert');
    });
  });

  describe('Back', () => {
    it('should route back to templates page', () => {
      spyOn(m.route, 'set');

      $('.button')[0].click();

      expect(m.route.set).toHaveBeenCalledWith('default');
    });
  });

  describe("Permissions", () => {
    it('should show permissions tab for admin users', () => {
      expect($('.permissions')).toBeInDOM();
    });

    it('should not show permissions tab for non admin users', () => {
      const elasticProfiles = Stream(ElasticProfiles.fromJSON(allProfilesJSON));
      const pluginInfos     = Stream(PluginInfos.fromJSON([]));

      m.mount(root, {
        view () {
          return m(TemplateConfigWidget, {
            elasticProfiles,
            pluginInfos,
            isUserAdmin:  false,
            templateName: templateJSON.name
          });
        }
      });
      m.redraw(true);

      expect($('.permissions')).not.toBeInDOM();
    });
  });

  describe("Stages", () => {
    it('should show stages of the template', () => {
      expect($('.stages')).toBeInDOM();
    });
  });
});
