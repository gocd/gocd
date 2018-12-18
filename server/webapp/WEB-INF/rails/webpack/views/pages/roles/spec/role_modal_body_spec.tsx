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

import * as m from "mithril";
import * as stream from "mithril/stream";
import {AuthConfigs} from "models/auth_configs/auth_configs_new";
import {TestData} from "models/auth_configs/spec/test_data";
import {Role, RoleType} from "models/roles/roles_new";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoTestData} from "models/shared/plugin_infos_new/spec/test_data";
import * as simulateEvent from "simulate-event";
import {Action, RoleModalBody} from "views/pages/roles/role_modal_body";
import {RolesTestData} from "views/pages/roles/spec/test_data";

describe("RoleModalBody", () => {
  const gocdRole       = Role.fromJSON(RolesTestData.GoCDRoleJSON());
  const pluginInfos    = [PluginInfo.fromJSON(PluginInfoTestData.authorization())];
  const authConfigs    = AuthConfigs.fromJSON(TestData.authConfigList(TestData.ldapAuthConfig()));
  const changeRoleType = jasmine.createSpy("changeRoleType");
  let $root: any, root: any;
  beforeEach(() => {
    //@ts-ignore
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(unmount);

  //@ts-ignore
  afterEach(window.destroyDomElementForTest);

  describe("ErrorMessage", () => {
    it("should show error message", () => {
      mount(<RoleModalBody role={stream(gocdRole)}
                           message="Something went wrong contact your admin!!!"
                           pluginInfos={pluginInfos}
                           authConfigs={authConfigs}
                           action={Action.NEW}
                           changeRoleType={changeRoleType}/>);
      expect(findByDataTestId("flash-message-alert").text()).toEqual("Something went wrong contact your admin!!!");
    });

    it("should not show error message when one is not present", () => {
      mount(<RoleModalBody role={stream(gocdRole)}
                           pluginInfos={pluginInfos}
                           authConfigs={authConfigs}
                           action={Action.NEW}
                           changeRoleType={changeRoleType}/>);

      expect(findByDataTestId("flash-message-alert").get(0)).not.toBeInDOM();
    });
  });

  describe("RoleTypeSelector", () => {
    it("should show role type selector", () => {
      mount(<RoleModalBody role={stream(gocdRole)}
                           action={Action.NEW}
                           pluginInfos={pluginInfos}
                           authConfigs={authConfigs}
                           changeRoleType={changeRoleType}/>);

      expect(findByDataTestId("role-type-selector").get(0)).toBeInDOM();
    });

    it("should not show role type selector for clone action", () => {
      mount(<RoleModalBody role={stream(gocdRole)}
                           pluginInfos={pluginInfos}
                           authConfigs={authConfigs}
                           action={Action.CLONE}
                           changeRoleType={changeRoleType}/>);

      expect(findByDataTestId("role-type-selector").get(0)).not.toBeInDOM();
    });

    it("should not show role type selector for edit action", () => {
      mount(<RoleModalBody role={stream(gocdRole)}
                           pluginInfos={pluginInfos}
                           authConfigs={authConfigs}
                           action={Action.EDIT}
                           changeRoleType={changeRoleType}/>);

      expect(findByDataTestId("role-type-selector").get(0)).not.toBeInDOM();
    });

  });

  describe("DisablePluginRoleType", () => {
    it("should disable when no auth configs defined", () => {
      mount(<RoleModalBody role={stream(gocdRole)}
                           action={Action.NEW}
                           pluginInfos={pluginInfos}
                           authConfigs={new AuthConfigs()}
                           changeRoleType={changeRoleType}/>);
      expect(find("#plugin-role").get(0)).toBeInDOM();
      expect(find("#plugin-role").get(0)).toBeDisabled();
    });

    it("should enable when auth configs are defined", () => {
      mount(<RoleModalBody role={stream(gocdRole)}
                           action={Action.NEW}
                           pluginInfos={pluginInfos}
                           authConfigs={authConfigs}
                           changeRoleType={changeRoleType}/>);
      expect(find("#plugin-role").get(0)).toBeInDOM();
      expect(find("#plugin-role").get(0)).not.toBeDisabled();
    });

  });

  describe("ChangeRoleType", () => {
    it("should change role type on click of radio button", () => {
      mount(<RoleModalBody role={stream(gocdRole)}
                           action={Action.NEW}
                           pluginInfos={pluginInfos}
                           authConfigs={authConfigs}
                           changeRoleType={changeRoleType}/>);

      expect(find("#plugin-role").get(0)).toBeInDOM();
      expect(find("#plugin-role").get(0)).not.toBeDisabled();

      simulateEvent.simulate(find("#plugin-role").get(0), "click");
      expect(changeRoleType).toHaveBeenCalledWith(RoleType.plugin, jasmine.any(Event));

      simulateEvent.simulate(find("#core-role").get(0), "click");
      expect(changeRoleType).toHaveBeenCalledWith(RoleType.gocd, jasmine.any(Event));
    });
  });

  function mount(component: m.Children) {
    m.mount(root, {
      view() {
        return <div>{component}</div>;
      }
    });
    m.redraw();
  }

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }

  function findByDataTestId(id: string) {
    return $root.find(`[data-test-id='${id}']`);
  }

  function find(selector: string) {
    return $root.find(selector);
  }
});
