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
import {FlashMessageModel} from "views/components/flash_message";
import {ModalManager} from "views/components/modal/modal_manager";
import "views/components/table/spec/table_matchers";
import {UserSearchModal} from "views/pages/users/add_user_modal";

describe("UserSearchModal", () => {

  const flashMessageModel = new FlashMessageModel();
  const refreshUsers      = jasmine.createSpy("refresh-agents");

  afterEach(ModalManager.closeAll);
  it("should render modal", () => {
       const modal = new UserSearchModal(flashMessageModel, refreshUsers);
       modal.render();
       m.redraw.sync();
       expect(modal).toContainTitle("Import User");
       expect(modal).toContainButtons(["Cancel", "Import"]);
       expect(document.querySelector(`[data-test-id='${modal.id}'] [data-test-id='user-search-query']`)).toBeInDOM();
     }
  );

  it("should render search results", () => {
    const modal = new UserSearchModal(flashMessageModel, refreshUsers);
    modal.userResult([
                       {
                         display_name: "Bob",
                         login_name: "bob",
                         is_admin: true,
                         email: "bob@example.com"
                       },
                       {
                         display_name: "Alice",
                         login_name: "alice",
                         is_admin: true,
                         email: "alice@example.com"
                       }
                     ]);
    modal.render();
    m.redraw.sync();

    expect(document.querySelector(`[data-test-id='${modal.id}'] table`)).toContainHeaderCells(["", "Username", "Display name", "Email"]);
  });

});
