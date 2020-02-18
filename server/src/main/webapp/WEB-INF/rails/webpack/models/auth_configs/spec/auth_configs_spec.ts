/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import {TestData} from "models/auth_configs/spec/test_data";
import {AuthConfigs} from "../auth_configs";

describe("AuthorizationConfigurationModel", () => {
  it("should deserialize json to AuthConfigs", () => {
    const authConfigsJSON = TestData.authConfigList(TestData.ldapAuthConfig(), TestData.gitHubAuthConfig());
    const authConfigs     = AuthConfigs.fromJSON(authConfigsJSON);

    expect(authConfigs.length).toEqual(2);

    expect(authConfigs[0].id()).toEqual("ldap");
    expect(authConfigs[0].pluginId()).toEqual("cd.go.authorization.ldap");
    expect(authConfigs[0].allowOnlyKnownUsersToLogin()).toEqual(true);
    expect(authConfigs[0].properties()!.count()).toEqual(3);
    expect(authConfigs[0].properties()!.valueFor("Url")).toEqual("ldap://ldap.server.url");
    expect(authConfigs[0].properties()!.valueFor("ManagerDN")).toEqual("uid=admin,ou=system");
    expect(authConfigs[0].properties()!.valueFor("Password")).toEqual("gGx7G+4+BAQ=");

    expect(authConfigs[1].id()).toEqual("github");
    expect(authConfigs[1].pluginId()).toEqual("cd.go.authorization.github");
    expect(authConfigs[1].allowOnlyKnownUsersToLogin()).toEqual(false);
    expect(authConfigs[1].properties()!.count()).toEqual(3);
    expect(authConfigs[1].properties()!.valueFor("Url")).toEqual("https://foo.github.com");
    expect(authConfigs[1].properties()!.valueFor("ClientKey")).toEqual("some-key");
    expect(authConfigs[1].properties()!.valueFor("ClientSecret")).toEqual("gGx7G+4+BAQ=");
  });

  it("should validate presence of plugin id", () => {
    const ldapAuthConfigJSON = TestData.ldapAuthConfig();
    delete ldapAuthConfigJSON.plugin_id;
    const authConfigs = AuthConfigs.fromJSON(TestData.authConfigList(ldapAuthConfigJSON));

    const isValid = authConfigs[0].isValid();

    expect(isValid).toBe(false);
    expect(authConfigs[0].errors().count()).toEqual(1);
    expect(authConfigs[0].errors().keys()).toEqual(["pluginId"]);
  });

  it("should validate presence of id", () => {
    const ldapAuthConfigJSON = TestData.ldapAuthConfig();
    delete ldapAuthConfigJSON.id;
    const authConfigs = AuthConfigs.fromJSON(TestData.authConfigList(ldapAuthConfigJSON));

    const isValid = authConfigs[0].isValid();

    expect(isValid).toBe(false);
    expect(authConfigs[0].errors().count()).toEqual(1);
    expect(authConfigs[0].errors().keys()).toEqual(["id"]);
  });

  it("should validate pattern for id", () => {
    const ldapAuthConfigJSON = TestData.ldapAuthConfig();
    ldapAuthConfigJSON.id    = "&%$Not-allowed";
    const authConfigs        = AuthConfigs.fromJSON(TestData.authConfigList(ldapAuthConfigJSON));

    const isValid = authConfigs[0].isValid();

    expect(isValid).toBe(false);
    expect(authConfigs[0].errors().count()).toEqual(1);
    expect(authConfigs[0].errors().keys()).toEqual(["id"]);
  });

  it("should validate length for id", () => {
    const ldapAuthConfigJSON = TestData.ldapAuthConfig();
    ldapAuthConfigJSON.id    = "This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters_This-is-longer-then-255-characters";
    const authConfigs        = AuthConfigs.fromJSON(TestData.authConfigList(ldapAuthConfigJSON));

    const isValid = authConfigs[0].isValid();

    expect(isValid).toBe(false);
    expect(authConfigs[0].errors().count()).toEqual(1);
    expect(authConfigs[0].errors().keys()).toEqual(["id"]);
  });
});
