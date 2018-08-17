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

describe('Plugin Collapsible List', () => {

  const $                     = require("jquery");
  const m                     = require("mithril");
  const simulateEvent         = require('simulate-event');
  const PluginCollapsibleList = require('views/shared/plugin_config/plugin_collapsible_list');
  const PluginConfigurations  = require('models/shared/plugin_configurations');
  require('jasmine-jquery');
  require('jasmine-ajax');

  let $root, root;

  beforeEach(() => {
    [$root, root]             = window.createDomElementForTest();
    const plainTextConfigJSON = {
      "key":   "Url",
      "value": "ldap://your.ldap.server.url:port"
    };

    const encryptedConfigJSON = {
      "key":             "Password",
      "encrypted_value": "secret"
    };

    const properties = PluginConfigurations.fromJSON([plainTextConfigJSON, encryptedConfigJSON]);
    m.mount(root,
      {
        view() {
          return m(PluginCollapsibleList, {headerKey: 'Store ID', headerValue: 'id1', properties});
        }
      }
    );
    m.redraw();
  });

  afterEach(() => {
    window.destroyDomElementForTest();
    m.mount(root, null);
    m.redraw();
  });

  it('should toggle collapsible list', () => {
    expect($('.c-collapse')).toHaveClass('collapsed');
    simulateEvent.simulate($root.find('.c-collapse_header').get(0), 'click');
    expect($('.c-collapse')).not.toHaveClass('collapsed');
  });

  it('should display plain text values', () => {
    simulateEvent.simulate($root.find('.c-collapse_header').get(0), 'click');
    expect($('.c-collapse_body .key-value-pair .key')[0]).toHaveText('Url');
    expect($('.c-collapse_body .key-value-pair .value')[0]).toHaveText('ldap://your.ldap.server.url:port');

  });

  it('should display masked encrypted values', () => {
    simulateEvent.simulate($root.find('.c-collapse_header').get(0), 'click');
    expect($('.c-collapse_body .key-value-pair .key')[1]).toHaveText('Password');
    expect($('.c-collapse_body .key-value-pair .value')[1]).toHaveText('******');
  });

});
