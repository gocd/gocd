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

import m from "mithril";
import Stream from "mithril/stream";
import {PipelineGroup} from "models/admin_pipelines/admin_pipelines";
import {Authorization, AuthorizedUsersAndRoles} from "models/authorization/authorization";
import {PipelineConfigTestData} from "models/pipeline_configs/spec/test_data";
import {Stage} from "models/pipeline_configs/stage";
import {
  PermissionsWidget,
  RolesSuggestionProvider
} from "views/pages/clicky_pipeline_config/tabs/stage/permissions_tab_content";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Permissions Tab Content", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render permissions info message", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    mount(stage);

    expect(helper.byTestId("flash-message-info"))
      .toContainText(
        "All system administrators and pipeline group administrators can operate on this stage (this cannot be overridden).");
  });

  it("should render switch for inherit or specify locally", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    mount(stage);

    expect(helper.byTestId("permissions-heading")).toContainText("Permissions for this stage:");

    expect(helper.byTestId("input-field-for-inherit")).toBeInDOM();
    expect(helper.allByTestId("form-field-label")[0]).toContainText("Inherit from the Pipeline Group");
    expect(helper.q("span", helper.byTestId("input-field-for-inherit")))
      .toContainText("Inherit authorization from the pipeline group.");

    expect(helper.byTestId("input-field-for-local")).toBeInDOM();
    expect(helper.allByTestId("form-field-label")[1]).toContainText("Specify locally");
    expect(helper.q("span", helper.byTestId("input-field-for-local")))
      .toContainText("Define specific permissions locally. This will override pipeline group authorization.");
  });

  it("should select inherit when no permissions are specified", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    mount(stage);

    expect(stage.approval().authorization().isInherited()).toBeTrue();

    expect(helper.byTestId("radio-inherit")).toBeChecked();
    expect(helper.byTestId("radio-local")).not.toBeChecked();
  });

  it("should select local when permissions are overridden locally", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.approval().authorization().isInherited(false);
    stage.approval().authorization()._users.push(Stream("user1"));
    mount(stage);

    expect(stage.approval().authorization().isInherited()).toBeFalse();
    expect(helper.byTestId("radio-inherit")).not.toBeChecked();
    expect(helper.byTestId("radio-local")).toBeChecked();
  });

  it("should toggle isInherited state when permissions are toggled", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    mount(stage);

    expect(stage.approval().authorization().isInherited()).toBeTrue();
    expect(helper.byTestId("radio-inherit")).toBeChecked();
    expect(helper.byTestId("radio-local")).not.toBeChecked();

    helper.click(helper.byTestId("radio-local"));

    expect(stage.approval().authorization().isInherited()).toBeFalse();
    expect(helper.byTestId("radio-inherit")).not.toBeChecked();
    expect(helper.byTestId("radio-local")).toBeChecked();
  });

  it("should not render users and roles section when permissions are inherited", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    mount(stage);

    expect(stage.approval().authorization().isInherited()).toBeTrue();
    expect(helper.byTestId("radio-inherit")).toBeChecked();
    expect(helper.byTestId("radio-local")).not.toBeChecked();

    expect(helper.byTestId("users-and-roles")).not.toBeInDOM();
  });

  it("should render users and roles section when permissions are defined locally", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.approval().authorization().isInherited(false);
    stage.approval().authorization()._users.push(Stream("user1"));
    mount(stage);

    expect(stage.approval().authorization().isInherited()).toBeFalse();
    expect(helper.byTestId("radio-inherit")).not.toBeChecked();
    expect(helper.byTestId("radio-local")).toBeChecked();

    expect(helper.byTestId("users-and-roles")).toBeInDOM();
  });

  it("should show no group permissions configured message no pipeline group permissions are defined", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.approval().authorization().isInherited(true);
    mount(stage);

    expect(stage.approval().authorization().isInherited()).toBeTrue();
    expect(helper.byTestId("radio-inherit")).toBeChecked();
    expect(helper.byTestId("radio-local")).not.toBeChecked();

    const msg = "There is no authorization configured for this stage nor its pipeline group. Only GoCD administrators can operate this stage.";
    expect(helper.qa(`[data-test-id="flash-message-info"]`)).toContainText(msg);
  });

  it("should render inherited group permissions", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.approval().authorization().isInherited(true);
    const admin         = new AuthorizedUsersAndRoles([Stream("admin")], [Stream("admin-role")]);
    const operate       = new AuthorizedUsersAndRoles([Stream("operate")], [Stream("operate-role")]);
    const pipelineGroup = new PipelineGroup("group1", new Authorization(undefined, admin, operate));
    mount(stage, false, pipelineGroup);

    expect(helper.q("input", helper.byTestId("input-field-for-admin"))).toHaveValue("admin");
    expect(helper.q("input", helper.byTestId("input-field-for-operate"))).toHaveValue("operate");

    expect(helper.q("input", helper.byTestId("input-field-for-admin-role"))).toHaveValue("admin-role");
    expect(helper.q("input", helper.byTestId("input-field-for-operate-role"))).toHaveValue("operate-role");
  });

  it("should copy over inherited permissions when toggled to specify locally", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.approval().authorization().isInherited(true);
    const admin         = new AuthorizedUsersAndRoles([Stream("admin")], [Stream("admin-role")]);
    const operate       = new AuthorizedUsersAndRoles([Stream("operate")], [Stream("operate-role")]);
    const pipelineGroup = new PipelineGroup("group1", new Authorization(undefined, admin, operate));
    mount(stage, false, pipelineGroup);

    expect(helper.byTestId("radio-inherit")).toBeChecked();
    expect(helper.byTestId("radio-local")).not.toBeChecked();

    expect(helper.q("input", helper.byTestId("input-field-for-admin"))).toHaveValue("admin");
    expect(helper.q("input", helper.byTestId("input-field-for-operate"))).toHaveValue("operate");

    expect(helper.q("input", helper.byTestId("input-field-for-admin-role"))).toHaveValue("admin-role");
    expect(helper.q("input", helper.byTestId("input-field-for-operate-role"))).toHaveValue("operate-role");

    //toggle to specify locally
    helper.click(helper.byTestId("radio-local"));

    expect(helper.byTestId("radio-inherit")).not.toBeChecked();
    expect(helper.byTestId("radio-local")).toBeChecked();

    expect(helper.q("input", helper.byTestId("input-field-for-admin"))).toHaveValue("admin");
    expect(helper.q("input", helper.byTestId("input-field-for-operate"))).toHaveValue("operate");

    expect(helper.q("input", helper.byTestId("input-field-for-admin-role"))).toHaveValue("admin-role");
    expect(helper.q("input", helper.byTestId("input-field-for-operate-role"))).toHaveValue("operate-role");
  });

  it("should not copy over inherited permissions when toggled to specify locally and locally permissions exists", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.approval().authorization().isInherited(false);
    stage.approval().authorization()._users.push(Stream("user1"));

    const admin         = new AuthorizedUsersAndRoles([Stream("admin")], [Stream("admin-role")]);
    const operate       = new AuthorizedUsersAndRoles([Stream("operate")], [Stream("operate-role")]);
    const pipelineGroup = new PipelineGroup("group1", new Authorization(undefined, admin, operate));
    mount(stage, false, pipelineGroup);

    //initially permissions are defined locally
    expect(helper.byTestId("radio-inherit")).not.toBeChecked();
    expect(helper.byTestId("radio-local")).toBeChecked();

    expect(helper.q("input", helper.byTestId("input-field-for-user1"))).toHaveValue("user1");

    //toggle to inherit
    helper.click(helper.byTestId("radio-inherit"));

    expect(helper.byTestId("radio-inherit")).toBeChecked();
    expect(helper.byTestId("radio-local")).not.toBeChecked();

    expect(helper.q("input", helper.byTestId("input-field-for-admin"))).toHaveValue("admin");
    expect(helper.q("input", helper.byTestId("input-field-for-operate"))).toHaveValue("operate");

    expect(helper.q("input", helper.byTestId("input-field-for-admin-role"))).toHaveValue("admin-role");
    expect(helper.q("input", helper.byTestId("input-field-for-operate-role"))).toHaveValue("operate-role");

    //again toggle back to specify locally
    helper.click(helper.byTestId("radio-local"));

    expect(helper.byTestId("radio-inherit")).not.toBeChecked();
    expect(helper.byTestId("radio-local")).toBeChecked();

    expect(helper.q("input", helper.byTestId("input-field-for-user1"))).toHaveValue("user1");
  });

  it("should not allow users to modify inherited group permissions", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.approval().authorization().isInherited(true);
    const admin         = new AuthorizedUsersAndRoles([Stream("admin")], [Stream("admin-role")]);
    const operate       = new AuthorizedUsersAndRoles([Stream("operate")], [Stream("operate-role")]);
    const pipelineGroup = new PipelineGroup("group1", new Authorization(undefined, admin, operate));
    mount(stage, false, pipelineGroup);

    expect(helper.q("input", helper.byTestId("input-field-for-admin"))).toBeDisabled();
    expect(helper.q("input", helper.byTestId("input-field-for-operate"))).toBeDisabled();

    expect(helper.q("input", helper.byTestId("input-field-for-admin-role"))).toBeDisabled();
    expect(helper.q("input", helper.byTestId("input-field-for-operate-role"))).toBeDisabled();

    expect(helper.q("i", helper.byTestId("input-field-for-admin"))).not.toBeInDOM();
    expect(helper.q("i", helper.byTestId("input-field-for-operate"))).not.toBeInDOM();

    expect(helper.q("i", helper.byTestId("input-field-for-admin-role"))).not.toBeInDOM();
    expect(helper.q("i", helper.byTestId("input-field-for-operate-role"))).not.toBeInDOM();
  });

  it("should render users in an input field", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.approval().authorization().isInherited(false);
    stage.approval().authorization()._users.push(Stream("user1"), Stream("user2"));
    mount(stage);

    expect(helper.byTestId("input-field-for-user1")).toBeInDOM();
    expect(helper.q("input", helper.byTestId("input-field-for-user1"))).toHaveValue("user1");

    expect(helper.byTestId("input-field-for-user2")).toBeInDOM();
    expect(helper.q("input", helper.byTestId("input-field-for-user2"))).toHaveValue("user2");
  });

  it("should render users in an input field", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.approval().authorization().isInherited(false);
    stage.approval().authorization()._users.push(Stream("user1"), Stream("user2"));
    mount(stage);

    expect(helper.qa("input", helper.byTestId("users"))).toHaveLength(2);

    expect(helper.byTestId("input-field-for-user1")).toBeInDOM();
    expect(helper.q("input", helper.byTestId("input-field-for-user1"))).toHaveValue("user1");

    expect(helper.byTestId("input-field-for-user2")).toBeInDOM();
    expect(helper.q("input", helper.byTestId("input-field-for-user2"))).toHaveValue("user2");
  });

  it("should render roles in an input field", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.approval().authorization().isInherited(false);
    stage.approval().authorization()._roles.push(Stream("role1"), Stream("role2"));
    mount(stage);

    expect(helper.qa("input", helper.byTestId("roles"))).toHaveLength(2);

    expect(helper.byTestId("input-field-for-role1")).toBeInDOM();
    expect(helper.q("input", helper.byTestId("input-field-for-role1"))).toHaveValue("role1");

    expect(helper.byTestId("input-field-for-role2")).toBeInDOM();
    expect(helper.q("input", helper.byTestId("input-field-for-role2"))).toHaveValue("role2");
  });

  it("should bind user input to model", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.approval().authorization().isInherited(false);
    stage.approval().authorization()._users.push(Stream("user1"));
    mount(stage);

    expect(stage.approval().authorization()._users[0]()).toEqual("user1");
    expect(helper.q("input", helper.byTestId("input-field-for-user1"))).toHaveValue("user1");

    helper.oninput("input", "bob", helper.byTestId("input-field-for-user1"));

    expect(stage.approval().authorization()._users[0]()).toEqual("bob");
    expect(helper.q("input", helper.byTestId("input-field-for-bob"))).toHaveValue("bob");
  });

  it("should bind role input to model", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.approval().authorization().isInherited(false);
    stage.approval().authorization()._roles.push(Stream("role1"));
    mount(stage);

    expect(stage.approval().authorization()._roles[0]()).toEqual("role1");
    expect(helper.q("input", helper.byTestId("input-field-for-role1"))).toHaveValue("role1");

    helper.oninput("input", "developers", helper.byTestId("input-field-for-role1"));

    expect(stage.approval().authorization()._roles[0]()).toEqual("developers");
    expect(helper.q("input", helper.byTestId("input-field-for-developers"))).toHaveValue("developers");
  });

  it("should remove user on click of remove icon", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.approval().authorization().isInherited(false);
    stage.approval().authorization()._users.push(Stream("user1"), Stream("user2"));
    mount(stage);

    expect(stage.approval().authorization()._users).toHaveLength(2);
    expect(helper.q("input", helper.byTestId("input-field-for-user1"))).toHaveValue("user1");
    expect(helper.q("input", helper.byTestId("input-field-for-user2"))).toHaveValue("user2");

    helper.click("i", helper.byTestId("input-field-for-user1"));

    expect(stage.approval().authorization()._users).toHaveLength(1);
    expect(helper.byTestId("input-field-for-user1")).not.toBeInDOM();
    expect(helper.q("input", helper.byTestId("input-field-for-user2"))).toHaveValue("user2");
  });

  it("should remove role on click of remove icon", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.approval().authorization().isInherited(false);
    stage.approval().authorization()._roles.push(Stream("role1"), Stream("role2"));
    mount(stage);

    expect(stage.approval().authorization()._roles).toHaveLength(2);
    expect(helper.q("input", helper.byTestId("input-field-for-role1"))).toHaveValue("role1");
    expect(helper.q("input", helper.byTestId("input-field-for-role2"))).toHaveValue("role2");

    helper.click("i", helper.byTestId("input-field-for-role1"));

    expect(stage.approval().authorization()._roles).toHaveLength(1);
    expect(helper.byTestId("input-field-for-role1")).not.toBeInDOM();
    expect(helper.q("input", helper.byTestId("input-field-for-role2"))).toHaveValue("role2");
  });

  it("should add an empty user input on click of add", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.approval().authorization().isInherited(false);
    mount(stage);

    expect(helper.qa("input", helper.byTestId("users"))).toHaveLength(0);

    helper.click("button", helper.byTestId("users"));

    expect(helper.qa("input", helper.byTestId("users"))).toHaveLength(1);
  });

  it("should add an empty role input on click of add", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.approval().authorization().isInherited(false);
    mount(stage);

    expect(helper.qa("input", helper.byTestId("roles"))).toHaveLength(0);

    helper.click("button", helper.byTestId("roles"));

    expect(helper.qa("input", helper.byTestId("roles"))).toHaveLength(1);
  });

  describe("Role Autocompletion", () => {
    beforeEach(() => {
      const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
      stage.approval().authorization().isInherited(false);
      mount(stage);
    });

    it("should provide all roles", (done) => {
      const allRoles        = Stream(["admin", "operators", "viewers"] as string[]);
      const configuredRoles = [] as string[];

      const provider = new RolesSuggestionProvider(allRoles, configuredRoles);

      provider.getData().then((suggestions) => {
        expect(suggestions).toEqual(allRoles());
        done();
      });
    });

    it("should provide no roles suggestions when all roles are configured", (done) => {
      const allRoles        = Stream(["admin", "operators", "viewers"] as string[]);
      const configuredRoles = ["admin", "operators", "viewers"] as string[];

      const provider = new RolesSuggestionProvider(allRoles, configuredRoles);

      provider.getData().then((suggestions) => {
        expect(suggestions).toEqual([]);
        done();
      });
    });

    it("should provide roles suggestions which are not configured", (done) => {
      const allRoles        = Stream(["admin", "operators", "viewers"] as string[]);
      const configuredRoles = ["admin"] as string[];

      const provider = new RolesSuggestionProvider(allRoles, configuredRoles);

      provider.getData().then((suggestions) => {
        expect(suggestions).toEqual(["operators", "viewers"]);
        done();
      });
    });
  });

  it("should render errors related to users", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.approval().authorization().isInherited(false);
    stage.approval().authorization()._users.push(Stream("user1"));
    stage.approval().authorization().errors().add("users", "user 'user1' does not exists");
    mount(stage);

    expect(helper.byTestId("users-errors")).toContainText("user 'user1' does not exists");
  });

  it("should render errors related to roles", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.approval().authorization().isInherited(false);
    stage.approval().authorization()._roles.push(Stream("role1"));
    stage.approval().authorization().errors().add("roles", "role 'role1' does not exists");
    mount(stage);

    expect(helper.byTestId("roles-errors")).toContainText("role 'role1' does not exists");
  });

  it("should not render local permission definition message when permissions are inherited", () => {
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.approval().authorization().isInherited(true);
    mount(stage);

    expect(helper.byTestId("local-permission-msg")).not.toBeInDOM();
  });

  it("should render local permission definition message", () => {
    const msg = "The pipeline group that this pipeline belongs to has permissions configured. You can add only those users and roles that have permissions to operate on this pipeline group.";
    const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test"));
    stage.approval().authorization().isInherited(false);
    stage.approval().authorization()._roles.push(Stream("role1"));
    mount(stage);

    expect(helper.byTestId("local-permission-msg")).toContainText(msg);
  });

  describe("Read Only", () => {
    beforeEach(() => {
      const stage = Stage.fromJSON(PipelineConfigTestData.stage("Test", "Job1"));
      stage.approval().authorization().isInherited(false);
      stage.approval().authorization()._users.push(Stream("user1"));
      stage.approval().authorization()._roles.push(Stream("role1"));
      mount(stage, true);
    });

    it("should render disabled switch for inherit or specify locally", () => {
      expect(helper.byTestId("radio-inherit")).toBeDisabled();
      expect(helper.byTestId("radio-local")).toBeDisabled();
    });

    it("should render disabled input for user", () => {
      expect(helper.q("input", helper.byTestId("users"))).toBeDisabled();
    });

    it("should render disabled input for role", () => {
      expect(helper.q("input", helper.byTestId("roles"))).toBeDisabled();
    });

    it("should not render remove user", () => {
      expect(helper.q("i", helper.byTestId("input-field-for-user1"))).not.toBeInDOM();
    });

    it("should not render remove role", () => {
      expect(helper.q("i", helper.byTestId("input-field-for-role1"))).not.toBeInDOM();
    });

    it("should not render add user button", () => {
      expect(helper.q("button", helper.byTestId("users"))).not.toBeInDOM();
    });

    it("should not render add role button", () => {
      expect(helper.q("button", helper.byTestId("roles"))).not.toBeInDOM();
    });
  });

  function mount(stage: Stage, isEntityDefinedInConfigRepository: boolean = false,
                 pipelineGroup: PipelineGroup                             = new PipelineGroup("",
                                                                                              new Authorization())) {
    const isInherited        = stage.approval().authorization().isInherited();
    const selectedPermission = Stream(isInherited ? "inherit" : "local");

    helper.mount(() => <PermissionsWidget entity={stage}
                                          groupPermissions={Stream(pipelineGroup)}
                                          isEntityDefinedInConfigRepository={isEntityDefinedInConfigRepository}
                                          selectedPermission={selectedPermission}
                                          allRoles={Stream<string[]>([])}/>);
  }
});
