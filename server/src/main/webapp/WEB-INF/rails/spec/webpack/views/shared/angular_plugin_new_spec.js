/*
 * Copyright 2019 ThoughtWorks, Inc.
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


import {AngularPluginNew} from "views/shared/angular_plugin_new.js.msx";
import {TestHelper} from "views/pages/spec/test_helper";
import Stream from "mithril/stream";
import {Configurations, Configuration} from "models/shared/configuration";
import {PlainTextValue, EncryptedValue} from "models/shared/config_value";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {AuthorizationPluginInfo} from "models/shared/plugin_infos_new/spec/test_data";
import m from "mithril";

describe("Angular Plugin View", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should ignore unknown properties", () => {
    const pluginInfo     = PluginInfo.fromJSON(AuthorizationPluginInfo.ldap());
    const configurations = new Configurations([
      new Configuration("Url", new PlainTextValue("some-url")),
      new Configuration("Password", new EncryptedValue("secret-password")),
      new Configuration("UnknownField", new PlainTextValue("random-value"))
    ]);

    expect(configurations.findConfiguration("Url")).not.toBeNull();
    expect(configurations.findConfiguration("Password")).not.toBeNull();
    expect(configurations.findConfiguration("UnknownField")).not.toBeNull();
    expect(configurations.findConfiguration("UnknownField")).not.toBeUndefined();

    mount(pluginInfo.extensions[0].authConfigSettings, configurations);

    expect(configurations.findConfiguration("UnknownField")).toBeUndefined();
    expect(configurations.findConfiguration("Url")).not.toBeUndefined();
    expect(configurations.findConfiguration("Url").isEncrypted()).toBeFalsy();
    expect(configurations.findConfiguration("Password")).not.toBeUndefined();
    expect(configurations.findConfiguration("Password").isEncrypted()).toBeTruthy();
  });

  it("should not ignore appropriate properties", () => {
    const pluginInfo     = PluginInfo.fromJSON(AuthorizationPluginInfo.file());
    const configurations = new Configurations([
      new Configuration("PasswordFilePath", new PlainTextValue("/var/lib/pass.prop")),
      new Configuration("AnotherProperty", new PlainTextValue("some_value"))
    ]);

    expect(configurations.findConfiguration("PasswordFilePath")).not.toBeNull();
    expect(configurations.findConfiguration("PasswordFilePath")).not.toBeNull();
    expect(configurations.findConfiguration("AnotherProperty")).not.toBeNull();
    expect(configurations.findConfiguration("AnotherProperty")).not.toBeUndefined();

    mount(pluginInfo.extensions[0].authConfigSettings, configurations);

    expect(configurations.findConfiguration("AnotherProperty")).not.toBeUndefined();
    expect(configurations.findConfiguration("PasswordFilePath")).not.toBeUndefined();
  });

  it('should disable fields when flag is set', () => {
    const pluginInfo     = PluginInfo.fromJSON(AuthorizationPluginInfo.ldap());
    const configurations = new Configurations([
      new Configuration("Url", new PlainTextValue("some-url")),
      new Configuration("Password", new EncryptedValue("secret-password")),
    ]);

    mount(pluginInfo.extensions[0].authConfigSettings, configurations, true);
    m.redraw.sync();

    expect(helper.q('#test-field-1')).toHaveAttr('disabled');
    expect(helper.q('#test-field-2')).toHaveAttr('disabled');
    expect(helper.q('#test-field-3')).toHaveAttr('disabled');
  });

  function mount(pluginSettings, configurations, disabled) {
    helper.mount(() => <AngularPluginNew pluginInfoSettings={Stream(pluginSettings)}
                                         disabled={disabled}
                                         configuration={configurations}/>);
  }
});
