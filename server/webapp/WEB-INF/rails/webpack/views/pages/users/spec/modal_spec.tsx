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

import {ModalManager} from "views/components/modal/modal_manager";
import "views/components/table/spec/table_matchers";
import {UserSearchModal} from "views/pages/users/modal";

describe("UserSearchModal", () => {
  afterEach(ModalManager.closeAll);
  it("should render", () => {
    const modal = new UserSearchModal();
    modal.render();
    expect(modal).toContainTitle("Search users");
    expect(modal).toContainButtons(["Close", "Add"]);
    expect($(`[data-test-id='${modal.id}'] [data-test-id='form-field-input-search-query']`)).toBeInDOM();
  }
  );

  it("should render", () => {
    const modal = new UserSearchModal();
    modal.userResult([
                       {
                         display_name: "Bob",
                         login_name: "bob",
                         email: "bob@example.com"
                       },
                       {
                         display_name: "Alice",
                         login_name: "alice",
                         email: "alice@example.com"
                       }
    ]);
    modal.render();
    expect($(`[data-test-id='${modal.id}'] table`)).toContainHeaderCells(["", "Username", "Display name", "Email"]);
  });

});
