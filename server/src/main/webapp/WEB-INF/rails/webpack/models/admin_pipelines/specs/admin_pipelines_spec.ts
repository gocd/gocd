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

import {Authorization, AuthorizedUsersAndRoles, PermissionForEntity} from "../../authorization/authorization";
import {PipelineGroup, PipelineGroupViewModel} from "../admin_pipelines";

describe("PipelineGroup", () => {
  it("should deserialize", () => {
    const pipelineGroup = PipelineGroup.fromJSON(pipelineGroupJSON());

    const expectedAuthorizationObject = new Authorization(new AuthorizedUsersAndRoles(["user1"], ["role1"]),
                                                          new AuthorizedUsersAndRoles(["admin"], ["admin"]),
                                                          new AuthorizedUsersAndRoles(["superUser"], ["role2"]));

    expect(pipelineGroup.name()).toBe("pipeline-group");

    expect(pipelineGroup.authorization().toJSON()).toEqual(expectedAuthorizationObject.toJSON());
  });

  it("should serialize", () => {
    const authorization = new Authorization(new AuthorizedUsersAndRoles(["user1"], ["role1"]),
                                            new AuthorizedUsersAndRoles(["admin"], ["admin"]),
                                            new AuthorizedUsersAndRoles(["superUser"], ["role2"]));

    const pipelineGroup = new PipelineGroup("pipeline-group", authorization);

    const actualJSON   = pipelineGroup.toJSON();
    const expectedJSON = {
      name: "pipeline-group",
      authorization: {
        view: {
          users: ["user1"],
          roles: ["role1"]
        },
        operate: {
          users: ["superUser"],
          roles: ["role2"]
        },
        admins: {
          users: ["admin"],
          roles: ["admin"]
        }
      }
    };

    expect(actualJSON).toEqual(expectedJSON);
  });

});

describe('PipelineGroupViewModel', () => {
  describe('User', () => {
    it("should call add authorized user", () => {
      const pipelineGroup          = PipelineGroup.fromJSON(pipelineGroupJSON());
      const pipelineGroupViewModel = new PipelineGroupViewModel(pipelineGroup);

      const newUser = new PermissionForEntity("newUser", true, true, true);
      pipelineGroupViewModel.addAuthorizedUser(newUser);

      expect(pipelineGroupViewModel.authorizedUsers()[3].name()).toBe(newUser.name());
      expect(pipelineGroupViewModel.authorizedUsers()[3].admin()).toBe(newUser.admin());
      expect(pipelineGroupViewModel.authorizedUsers()[3].operate()).toBe(newUser.operate());
      expect(pipelineGroupViewModel.authorizedUsers()[3].view()).toBe(newUser.view());
    });

    it("should remove authorized user", () => {
      const pipelineGroup          = PipelineGroup.fromJSON(pipelineGroupJSON());
      const pipelineGroupViewModel = new PipelineGroupViewModel(pipelineGroup);

      const user = pipelineGroupViewModel.authorizedUsers()[2];

      pipelineGroupViewModel.removeAuthorizedUser(user);

      expect(pipelineGroupViewModel.authorizedUsers().length).toBe(2);
      expect(pipelineGroupViewModel.authorizedUsers().includes(user)).toBe(false);
    });
  });

  describe('Role', () => {
    it("should call add authorized role", () => {
      const pipelineGroup          = PipelineGroup.fromJSON(pipelineGroupJSON());
      const pipelineGroupViewModel = new PipelineGroupViewModel(pipelineGroup);

      const newRole = new PermissionForEntity("newRole", true, true, true);
      pipelineGroupViewModel.addAuthorizedRole(newRole);

      expect(pipelineGroupViewModel.authorizedRoles()[3].name()).toBe(newRole.name());
      expect(pipelineGroupViewModel.authorizedRoles()[3].admin()).toBe(newRole.admin());
      expect(pipelineGroupViewModel.authorizedRoles()[3].operate()).toBe(newRole.operate());
      expect(pipelineGroupViewModel.authorizedRoles()[3].view()).toBe(newRole.view());
    });

    it("should remove authorized role", () => {
      const pipelineGroup          = PipelineGroup.fromJSON(pipelineGroupJSON());
      const pipelineGroupViewModel = new PipelineGroupViewModel(pipelineGroup);

      const role = pipelineGroupViewModel.authorizedRoles()[2];

      pipelineGroupViewModel.removeAuthorizedRole(role);

      expect(pipelineGroupViewModel.authorizedRoles().length).toBe(2);
      expect(pipelineGroupViewModel.authorizedRoles().includes(role)).toBe(false);
    });
  });

  it('should list authorized users', () => {
    const pipelineGroup          = PipelineGroup.fromJSON(pipelineGroupJSON());
    const pipelineGroupViewModel = new PipelineGroupViewModel(pipelineGroup);

    expect(pipelineGroupViewModel.authorizedUsers().length).toBe(3);
    expect(pipelineGroupViewModel.authorizedUsers()[0].name()).toBe("user1");
    expect(pipelineGroupViewModel.authorizedUsers()[1].name()).toBe("superUser");
    expect(pipelineGroupViewModel.authorizedUsers()[2].name()).toBe("admin");
  });

  it('should list authorized roles', () => {
    const pipelineGroup          = PipelineGroup.fromJSON(pipelineGroupJSON());
    const pipelineGroupViewModel = new PipelineGroupViewModel(pipelineGroup);

    expect(pipelineGroupViewModel.authorizedRoles().length).toBe(3);
    expect(pipelineGroupViewModel.authorizedRoles()[0].name()).toBe("role1");
    expect(pipelineGroupViewModel.authorizedRoles()[1].name()).toBe("role2");
    expect(pipelineGroupViewModel.authorizedRoles()[2].name()).toBe("admin");
  });

  it("should get updated pipeline group", () => {
    const pipelineGroup          = PipelineGroup.fromJSON(pipelineGroupJSON());
    const pipelineGroupViewModel = new PipelineGroupViewModel(pipelineGroup);

    pipelineGroupViewModel.authorizedUsers()[0].operate(true);
    pipelineGroupViewModel.authorizedUsers()[2].operate(true);
    pipelineGroupViewModel.authorizedUsers()[2].view(true);
    pipelineGroupViewModel.authorizedRoles()[2].operate(true);
    pipelineGroupViewModel.authorizedRoles()[2].view(true);

    const updatedPipelineGroup = pipelineGroupViewModel.getUpdatedPipelineGroup();

    expect(updatedPipelineGroup.authorization().view().users()).toEqual(["user1", "admin"]);
    expect(updatedPipelineGroup.authorization().operate().users()).toEqual(["user1", "superUser", "admin"]);
    expect(updatedPipelineGroup.authorization().admin().users()).toEqual(["admin"]);

    expect(updatedPipelineGroup.authorization().view().roles()).toEqual(["role1", "admin"]);
    expect(updatedPipelineGroup.authorization().operate().roles()).toEqual(["role2", "admin"]);
    expect(updatedPipelineGroup.authorization().admin().roles()).toEqual(["admin"]);
    expect(updatedPipelineGroup.name()).toEqual(pipelineGroup.name());
  });
});

export function pipelineGroupJSON() {
  return {
    name: "pipeline-group",
    authorization: {
      view: {
        users: ["user1"],
        roles: ["role1"]
      },
      operate: {
        users: ["superUser"],
        roles: ["role2"]
      },
      admins: {
        users: ["admin"],
        roles: ["admin"]
      }
    }
  };
}
