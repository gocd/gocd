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

import {ApiResult, ObjectWithEtag} from "helpers/api_request_builder";
import _ from "lodash";
import m from "mithril";
import {PipelineGroup} from "models/admin_pipelines/admin_pipelines";
import {PipelineGroupCRUD} from "models/admin_pipelines/pipeline_groups_crud";
import {pipelineGroupJSON} from "models/admin_pipelines/specs/admin_pipelines_spec";
import {Authorization} from "models/authorization/authorization";
import {ModalManager} from "views/components/modal/modal_manager";
import {TestHelper} from "views/pages/spec/test_helper";
import {EditPipelineGroupModal} from "../edit_pipeline_group_modal";

describe('EditPipelineGroupModal', () => {
  let modal: EditPipelineGroupModal;
  let testHelper: TestHelper;

  function mount(containsPipelinesRemotely: boolean = false) {
    const pipelineGroup = PipelineGroup.fromJSON(pipelineGroupJSON());
    modal               = new EditPipelineGroupModal(pipelineGroup, "etag", [], [], _.noop, containsPipelinesRemotely);
    modal.render();
    m.redraw.sync();
    testHelper = new TestHelper().forModal();
  }

  afterEach(() => {
    ModalManager.closeAll();
  });

  it('should render text field and label for pipeline group name', () => {
    mount();
    expect(testHelper.byTestId("form-field-label-pipeline-group-name")).toBeInDOM();
    expect(testHelper.byTestId("form-field-label-pipeline-group-name")).toHaveText("Pipeline group name");

    expect(testHelper.byTestId("form-field-input-pipeline-group-name")).toBeInDOM();
    expect(testHelper.byTestId("form-field-input-pipeline-group-name")).toHaveValue("pipeline-group");
    expect(testHelper.byTestId("form-field-input-pipeline-group-name")).not.toBeDisabled();
  });

  it('should render pipeline disabled group name with tooltip', () => {
    mount(true);
    expect(testHelper.byTestId("info-tooltip")).toBeInDOM();
    expect(testHelper.byTestId("info-tooltip")).toHaveText("Cannot rename pipeline group as it contains remotely defined pipelines");
    expect(testHelper.byTestId("form-field-input-pipeline-group-name")).toBeDisabled();
  });

  describe('userPermissions', () => {
    beforeEach(() => {
      mount();
    });

    it('should render open collapsible panel', () => {
      expect(testHelper.byTestId("collapse-header", testHelper.byTestId("users-permissions-collapse"))).toContainText("User permissions");
      expect(testHelper.byTestId("add-user-permission", testHelper.byTestId("users-permissions-collapse"))).toBeInDOM();
      expect(testHelper.byTestId("add-user-permission", testHelper.byTestId("users-permissions-collapse"))).toHaveText("Add");
      expect(testHelper.byTestId("collapse-body", testHelper.byTestId("users-permissions-collapse"))).toBeInDOM();
    });

    it('should render collapsible panel with user permissions table', () => {
      expect(testHelper.byTestId("users-permissions")).toBeInDOM();
      expect(testHelper.byTestId("users-permissions")).toContainHeaderCells(["Name", "View", "Operate", "Admin", ""]);

      const permissions = testHelper.allByTestId("table-row", testHelper.byTestId("users-permissions"));
      const row1        = permissions[0] as HTMLElement;
      expect((testHelper.byTestId("user-name", row1) as HTMLInputElement).value).toBe("user1");
      expect((testHelper.byTestId("view-permission", row1) as HTMLInputElement)).toBeChecked();
      expect((testHelper.byTestId("operate-permission", row1) as HTMLInputElement)).not.toBeChecked();
      expect((testHelper.byTestId("admin-permission", row1) as HTMLInputElement)).not.toBeChecked();

      const row2 = permissions[1] as HTMLElement;
      expect((testHelper.byTestId("user-name", row2) as HTMLInputElement).value).toBe("superUser");
      expect((testHelper.byTestId("view-permission", row2) as HTMLInputElement)).toBeChecked();
      expect((testHelper.byTestId("view-permission", row2) as HTMLInputElement)).toBeDisabled();
      expect((testHelper.byTestId("operate-permission", row2) as HTMLInputElement)).toBeChecked();
      expect((testHelper.byTestId("admin-permission", row2) as HTMLInputElement)).not.toBeChecked();

      const row3 = permissions[2] as HTMLElement;
      expect((testHelper.byTestId("user-name", row3) as HTMLInputElement).value).toBe("admin");
      expect((testHelper.byTestId("view-permission", row3) as HTMLInputElement)).toBeChecked();
      expect((testHelper.byTestId("view-permission", row3) as HTMLInputElement)).toBeDisabled();
      expect((testHelper.byTestId("operate-permission", row3) as HTMLInputElement)).toBeChecked();
      expect((testHelper.byTestId("operate-permission", row3) as HTMLInputElement)).toBeDisabled();
      expect((testHelper.byTestId("admin-permission", row3) as HTMLInputElement)).toBeChecked();
    });

    it('should remove user on click of remove button', () => {
      expect(testHelper.byTestId("user-permission-delete")).toBeInDOM();
      expect(testHelper.allByTestId("user-permission-delete").length).toBe(3);

      const userPermission = testHelper.allByTestId("user-permission-delete")[0];
      testHelper.click(userPermission);
      expect(testHelper.allByTestId("table-row", testHelper.byTestId("users-permissions")).length).toBe(2);
      expect(testHelper.allByTestId("table-row", userPermission)).not.toBeInDOM();
    });

    it('should add user on click of add button', () => {
      testHelper.click(testHelper.byTestId("add-user-permission"));
      expect(testHelper.allByTestId("table-row", testHelper.byTestId("users-permissions")).length).toBe(4);
      const newRolePermission = testHelper.allByTestId("table-row", testHelper.byTestId("users-permissions"))[3];
      expect((testHelper.byTestId("user-name", newRolePermission) as HTMLInputElement).value).toBe("");
      expect((testHelper.byTestId("view-permission", newRolePermission) as HTMLInputElement)).toBeChecked();
      expect((testHelper.byTestId("operate-permission", newRolePermission) as HTMLInputElement)).not.toBeChecked();
      expect((testHelper.byTestId("admin-permission", newRolePermission) as HTMLInputElement)).not.toBeChecked();
    });

    it('should open collapse click of add button', () => {
      const collapsibleForUserPermissions = testHelper.byTestId("collapse-header", testHelper.byTestId("users-permissions-collapse"));
      testHelper.click(collapsibleForUserPermissions);
      expect(testHelper.byTestId("collapse-body", testHelper.byTestId("users-permissions-collapse"))).toBeHidden();
      testHelper.click(testHelper.byTestId("add-user-permission"));
      expect(testHelper.byTestId("collapse-body", testHelper.byTestId("users-permissions-collapse"))).not.toBeHidden();
    });

    it('should disable checkboxes for view permissions on enable of operate permission', () => {
      testHelper.clickByTestId("add-user-permission");
      const operateUserPermissions = testHelper.allByTestId("table-row", testHelper.byTestId("users-permissions"))[3] as HTMLElement;

      expect(testHelper.byTestId("view-permission", operateUserPermissions)).not.toBeDisabled();
      expect(testHelper.byTestId("view-permission", operateUserPermissions)).toBeChecked();

      expect(testHelper.byTestId("operate-permission", operateUserPermissions)).not.toBeDisabled();
      expect(testHelper.byTestId("operate-permission", operateUserPermissions)).not.toBeChecked();

      testHelper.click(testHelper.byTestId("operate-permission", operateUserPermissions));

      expect(testHelper.byTestId("view-permission", operateUserPermissions)).toBeDisabled();
      expect(testHelper.byTestId("view-permission", operateUserPermissions)).toBeChecked();

      expect(testHelper.byTestId("operate-permission", operateUserPermissions)).toBeChecked();

    });

    it('should disable checkboxes for view and operate permissions on enable of admin permission', () => {
      testHelper.clickByTestId("add-user-permission");
      const operateUserPermissions = testHelper.allByTestId("table-row", testHelper.byTestId("users-permissions"))[3] as HTMLElement;

      expect(testHelper.byTestId("view-permission", operateUserPermissions)).not.toBeDisabled();
      expect(testHelper.byTestId("view-permission", operateUserPermissions)).toBeChecked();

      expect(testHelper.byTestId("operate-permission", operateUserPermissions)).not.toBeDisabled();
      expect(testHelper.byTestId("operate-permission", operateUserPermissions)).not.toBeChecked();

      expect(testHelper.byTestId("admin-permission", operateUserPermissions)).not.toBeDisabled();
      expect(testHelper.byTestId("admin-permission", operateUserPermissions)).not.toBeChecked();

      testHelper.click(testHelper.byTestId("admin-permission", operateUserPermissions));

      expect(testHelper.byTestId("view-permission", operateUserPermissions)).toBeDisabled();
      expect(testHelper.byTestId("view-permission", operateUserPermissions)).toBeChecked();

      expect(testHelper.byTestId("operate-permission", operateUserPermissions)).toBeDisabled();
      expect(testHelper.byTestId("operate-permission", operateUserPermissions)).toBeChecked();

      expect(testHelper.byTestId("admin-permission", operateUserPermissions)).not.toBeDisabled();
      expect(testHelper.byTestId("admin-permission", operateUserPermissions)).toBeChecked();
    });

    it("should keep only view permission on the unchecked of admin permission", () => {
      testHelper.clickByTestId("add-user-permission");
      const userPermissions = testHelper.allByTestId("table-row", testHelper.byTestId("users-permissions"))[3] as HTMLElement;

      //for enabling admin
      testHelper.click(testHelper.byTestId("admin-permission", userPermissions));
      //for disabling admin
      testHelper.click(testHelper.byTestId("admin-permission", userPermissions));

      expect(testHelper.byTestId("operate-permission", userPermissions)).not.toBeDisabled();
      expect(testHelper.byTestId("operate-permission", userPermissions)).not.toBeChecked();

      expect(testHelper.byTestId("view-permission", userPermissions)).not.toBeDisabled();
      expect(testHelper.byTestId("view-permission", userPermissions)).toBeChecked();

    });
  });

  describe('rolePermissions', () => {
    beforeEach(() => {
      mount();
    });

    it('should render open collapsible panel', () => {
      expect(testHelper.byTestId("collapse-header", testHelper.byTestId("roles-permissions-collapse"))).toContainText("Role permissions");
      expect(testHelper.byTestId("add-role-permission", testHelper.byTestId("roles-permissions-collapse"))).toBeInDOM();
      expect(testHelper.byTestId("add-role-permission", testHelper.byTestId("roles-permissions-collapse"))).toHaveText("Add");
      expect(testHelper.byTestId("collapse-body", testHelper.byTestId("roles-permissions-collapse"))).toBeInDOM();
    });

    it('should render collapsible panel with role permissions table', () => {
      expect(testHelper.byTestId("roles-permissions")).toBeInDOM();
      expect(testHelper.byTestId("roles-permissions")).toContainHeaderCells(["Name", "View", "Operate", "Admin", ""]);

      const permissions = testHelper.allByTestId("table-row", testHelper.byTestId("roles-permissions"));
      const row1        = permissions[0] as HTMLElement;
      expect((testHelper.byTestId("role-name", row1) as HTMLInputElement).value).toBe("role1");
      expect((testHelper.byTestId("view-permission", row1) as HTMLInputElement)).toBeChecked();
      expect((testHelper.byTestId("operate-permission", row1) as HTMLInputElement)).not.toBeChecked();
      expect((testHelper.byTestId("admin-permission", row1) as HTMLInputElement)).not.toBeChecked();

      const row2 = permissions[1] as HTMLElement;
      expect((testHelper.byTestId("role-name", row2) as HTMLInputElement).value).toBe("role2");
      expect((testHelper.byTestId("view-permission", row2) as HTMLInputElement)).toBeChecked();
      expect((testHelper.byTestId("view-permission", row2) as HTMLInputElement)).toBeDisabled();
      expect((testHelper.byTestId("operate-permission", row2) as HTMLInputElement)).toBeChecked();
      expect((testHelper.byTestId("admin-permission", row2) as HTMLInputElement)).not.toBeChecked();

      const row3 = permissions[2] as HTMLElement;
      expect((testHelper.byTestId("role-name", row3) as HTMLInputElement).value).toBe("admin");
      expect((testHelper.byTestId("view-permission", row3) as HTMLInputElement)).toBeChecked();
      expect((testHelper.byTestId("view-permission", row3) as HTMLInputElement)).toBeDisabled();
      expect((testHelper.byTestId("operate-permission", row3) as HTMLInputElement)).toBeChecked();
      expect((testHelper.byTestId("operate-permission", row3) as HTMLInputElement)).toBeDisabled();
      expect((testHelper.byTestId("admin-permission", row3) as HTMLInputElement)).toBeChecked();
    });

    it('should remove role on click of remove button', () => {

      expect(testHelper.byTestId("role-permission-delete")).toBeInDOM();
      expect(testHelper.allByTestId("role-permission-delete").length).toBe(3);

      const rolePermission = testHelper.allByTestId("role-permission-delete")[0];
      testHelper.click(rolePermission);
      expect(testHelper.allByTestId("table-row", testHelper.byTestId("roles-permissions")).length).toBe(2);
      expect(testHelper.allByTestId("table-row", rolePermission)).not.toBeInDOM();
    });

    it('should add role on click of add button', () => {
      testHelper.click(testHelper.byTestId("add-role-permission"));
      expect(testHelper.allByTestId("table-row", testHelper.byTestId("roles-permissions")).length).toBe(4);
      const newRolePermission = testHelper.allByTestId("table-row", testHelper.byTestId("roles-permissions"))[3];
      expect((testHelper.byTestId("role-name", newRolePermission) as HTMLInputElement).value).toBe("");
      expect((testHelper.byTestId("view-permission", newRolePermission) as HTMLInputElement)).toBeChecked();
      expect((testHelper.byTestId("operate-permission", newRolePermission) as HTMLInputElement)).not.toBeChecked();
      expect((testHelper.byTestId("admin-permission", newRolePermission) as HTMLInputElement)).not.toBeChecked();
    });

    it('should open role collapsible panel click of add button', () => {
      const collapsibleForRolePermissions = testHelper.byTestId("collapse-header", testHelper.byTestId("roles-permissions-collapse"));
      testHelper.click(collapsibleForRolePermissions);
      expect(testHelper.byTestId("collapse-body", testHelper.byTestId("roles-permissions-collapse"))).toBeHidden();
      testHelper.click(testHelper.byTestId("add-role-permission"));
      expect(testHelper.byTestId("collapse-body", testHelper.byTestId("roles-permissions-collapse"))).not.toBeHidden();
    });

    it('should disable checkboxes for view permissions on enable of operate permission', () => {
      testHelper.clickByTestId("add-role-permission");
      const operateRolePermissions = testHelper.allByTestId("table-row", testHelper.byTestId("roles-permissions"))[3] as HTMLElement;
      expect(testHelper.byTestId("view-permission", operateRolePermissions)).not.toBeDisabled();
      expect(testHelper.byTestId("view-permission", operateRolePermissions)).toBeChecked();

      expect(testHelper.byTestId("operate-permission", operateRolePermissions)).not.toBeDisabled();
      expect(testHelper.byTestId("operate-permission", operateRolePermissions)).not.toBeChecked();

      testHelper.click(testHelper.byTestId("operate-permission", operateRolePermissions));

      expect(testHelper.byTestId("view-permission", operateRolePermissions)).toBeDisabled();
      expect(testHelper.byTestId("view-permission", operateRolePermissions)).toBeChecked();

      expect(testHelper.byTestId("operate-permission", operateRolePermissions)).toBeChecked();

    });

    it('should disable checkboxes for view and operate permissions on enable of admin permission', () => {
      testHelper.clickByTestId("add-role-permission");
      const operateRolePermissions = testHelper.allByTestId("table-row", testHelper.byTestId("roles-permissions"))[3] as HTMLElement;

      expect(testHelper.byTestId("view-permission", operateRolePermissions)).not.toBeDisabled();
      expect(testHelper.byTestId("view-permission", operateRolePermissions)).toBeChecked();

      expect(testHelper.byTestId("operate-permission", operateRolePermissions)).not.toBeDisabled();
      expect(testHelper.byTestId("operate-permission", operateRolePermissions)).not.toBeChecked();

      expect(testHelper.byTestId("admin-permission", operateRolePermissions)).not.toBeDisabled();
      expect(testHelper.byTestId("admin-permission", operateRolePermissions)).not.toBeChecked();

      testHelper.click(testHelper.byTestId("admin-permission", operateRolePermissions));

      expect(testHelper.byTestId("view-permission", operateRolePermissions)).toBeDisabled();
      expect(testHelper.byTestId("view-permission", operateRolePermissions)).toBeChecked();

      expect(testHelper.byTestId("operate-permission", operateRolePermissions)).toBeDisabled();
      expect(testHelper.byTestId("operate-permission", operateRolePermissions)).toBeChecked();

      expect(testHelper.byTestId("admin-permission", operateRolePermissions)).not.toBeDisabled();
      expect(testHelper.byTestId("admin-permission", operateRolePermissions)).toBeChecked();
    });

    it("should keep only view permission on the unchecked of admin permission", () => {
      testHelper.clickByTestId("add-role-permission");
      const rolePermissions = testHelper.allByTestId("table-row", testHelper.byTestId("roles-permissions"))[3] as HTMLElement;

      //for enabling admin
      testHelper.click(testHelper.byTestId("admin-permission", rolePermissions));
      //for disabling admin
      testHelper.click(testHelper.byTestId("admin-permission", rolePermissions));

      expect(testHelper.byTestId("operate-permission", rolePermissions)).not.toBeDisabled();
      expect(testHelper.byTestId("operate-permission", rolePermissions)).not.toBeChecked();

      expect(testHelper.byTestId("view-permission", rolePermissions)).not.toBeDisabled();
      expect(testHelper.byTestId("view-permission", rolePermissions)).toBeChecked();

    });
  });

  it("should update pipeline group on click of save button", () => {
    mount();
    spyOn(PipelineGroupCRUD, "update").and.returnValue(new Promise<ApiResult<ObjectWithEtag<PipelineGroup>>>((resolve) => {
      resolve(ApiResult.success("", 200, new Map()).map<ObjectWithEtag<PipelineGroup>>(
        () => {
          return {
            object: new PipelineGroup("grp", new Authorization()),
            etag:   "some-etag"
          } as ObjectWithEtag<PipelineGroup>;
        }
      ));
    }));

    testHelper.click(testHelper.byTestId("save-pipeline-group"));
    expect(PipelineGroupCRUD.update).toHaveBeenCalled();
  });

  it('should have save and cancel buttons', () => {
    mount();
    expect(testHelper.byTestId("cancel-button")).toBeInDOM();
    expect(testHelper.byTestId("save-pipeline-group")).toBeInDOM();
  });

  it('should render errors on roles if any', () => {
    const pipelineGroup = PipelineGroup.fromJSON(pipelineGroupJSON());
    pipelineGroup.authorization().admin().errors().add('roles', 'Some roles are invalid');
    modal = new EditPipelineGroupModal(pipelineGroup, "etag", [], [], _.noop, false);
    modal.render();
    m.redraw.sync();
    testHelper = new TestHelper().forModal();

    const element = testHelper.byTestId('errors-on-roles');
    expect(element).toBeInDOM();
    expect(testHelper.qa('li', element).length).toBe(1);
    expect(testHelper.qa('li', element)[0].innerText).toBe('Some roles are invalid');
  });

  it('should render errors on users if any', () => {
    const pipelineGroup = PipelineGroup.fromJSON(pipelineGroupJSON());
    pipelineGroup.authorization().admin().errors().add('users', 'Some users are invalid');
    modal = new EditPipelineGroupModal(pipelineGroup, "etag", [], [], _.noop, false);
    modal.render();
    m.redraw.sync();
    testHelper = new TestHelper().forModal();

    const element = testHelper.byTestId('errors-on-users');
    expect(element).toBeInDOM();
    expect(testHelper.qa('li', element).length).toBe(1);
    expect(testHelper.qa('li', element)[0].innerText).toBe('Some users are invalid');
  });
});
