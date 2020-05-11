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

import Stream from "mithril/stream";
import {AuthorizedUsersAndRoles} from "models/authorization/authorization";
import {TemplateAuthorization} from "../templates";

describe('TemplateModelSpec', () => {

  it("should deserialize", () => {
    const authorization = TemplateAuthorization.fromJSON(templateJSON());

    expect(authorization.allGroupAdminsAreViewUsers()).toBeTrue();
    expect(authorization.admin()).not.toBeUndefined();
    expect(authorization.admin()!.roles()).toEqual(['role1']);
    expect(authorization.admin()!.users()).toEqual(['user1']);
    expect(authorization.view()).not.toBeUndefined();
    expect(authorization.view()!.roles()).toEqual(['role2']);
    expect(authorization.view()!.users()).toEqual(['user2']);
  });

  it("should serialize", () => {
    const admins                = new AuthorizedUsersAndRoles([Stream('user1')], [Stream('role1')]);
    const views                 = new AuthorizedUsersAndRoles([Stream('user2')], [Stream('role2')]);
    const templateAuthorization = new TemplateAuthorization(true, admins, views);

    const actualJSON = templateAuthorization.toJSON();

    expect(actualJSON).toEqual(templateJSON());
  });
});

export function templateJSON() {
  return {
    all_group_admins_are_view_users: true,
    admin:                           {
      users: ['user1'],
      roles: ['role1']
    },
    view:                            {
      users: ['user2'],
      roles: ['role2']
    }
  };
}
