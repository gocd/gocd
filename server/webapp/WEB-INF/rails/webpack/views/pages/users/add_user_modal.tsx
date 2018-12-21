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

import * as _ from "lodash";
import * as m from "mithril";
import * as stream from "mithril/stream";
import {Stream} from "mithril/stream";
import {UserJSON} from "models/users/users";
import {UsersCRUD, UserSearchCRUD} from "models/users/users_crud";
import * as uuid from "uuid/v4";
import * as Buttons from "views/components/buttons";
import {Primary} from "views/components/buttons";
import {FlashMessage, FlashMessageModel, MessageType} from "views/components/flash_message";
import {TextField} from "views/components/forms/input_fields";
import {Modal, Size} from "views/components/modal";
import {Spinner} from "views/components/spinner";
import {Table} from "views/components/table";

export class UserSearchModal extends Modal {

  readonly userResult: Stream<UserJSON[]>               = stream();
  private readonly radioName                            = `user-search-${uuid()}`;
  private readonly searchText: Stream<string>           = stream();
  private readonly searchStatus                         = stream(false);
  private readonly selectedUser: Stream<UserJSON>       = stream();
  private readonly modalLevelMessage: FlashMessageModel = new FlashMessageModel();

  private pageLevelMessage: FlashMessageModel;
  private refreshUsers: () => Promise<any>;

  constructor(message: FlashMessageModel, refreshUsers: () => Promise<any>) {
    super(Size.large);
    this.pageLevelMessage = message;
    this.refreshUsers     = refreshUsers;
  }

  body(): JSX.Element {
    let optionalErrorMesage: JSX.Element | null = null;

    if (this.modalLevelMessage.hasMessage()) {
      optionalErrorMesage =
        <FlashMessage type={this.modalLevelMessage.type as MessageType} message={this.modalLevelMessage.message}/>;
    }

    return (
      <div>
        {optionalErrorMesage}
        <TextField label={"Search query"} property={this.searchText}/>
        <Primary onclick={this.search.bind(this)}>Search</Primary>
        {this.renderTable()}
      </div>
    );
  }

  buttons(): JSX.Element[] {
    return [
      <Buttons.Primary data-test-id="button-add" onclick={this.addUser.bind(this)}>Add</Buttons.Primary>,
      <Buttons.Secondary data-test-id="button-close" onclick={this.close.bind(this)}>Close</Buttons.Secondary>
    ];
  }

  title(): string {
    return "Search users";
  }

  private addUser() {
    UsersCRUD.create(this.selectedUser())
             .then((apiResult) => {
               apiResult.do(() => {
                 this.pageLevelMessage.setSuccess(`User '${this.selectedUser().login_name}' added successfully.`);
                 this.refreshUsers();
                 this.close();
               }, (errorResponse) => {
                 this.modalLevelMessage.setError(errorResponse.message);
               });
             }).finally(m.redraw);
  }

  private search() {
    this.searchStatus(true);
    UserSearchCRUD.search(this.searchText())
                  .then((apiResult) => {
                    this.searchStatus(false);
                    apiResult.do((successResponse) => {
                      this.userResult(successResponse.body);
                    }, (errorResponse) => {
                      // vnode.state.onError(errorResponse.message);
                    });
                  });

  }

  private renderTable() {
    if (this.searchStatus()) {
      return <Spinner/>;
    }

    if (_.isEmpty(this.userResult())) {
      return <FlashMessage message="No users to display!" type={MessageType.warning}/>;
    }

    return (
      <Table headers={["", "Username", "Display name", "Email"]} data={this.userResult().map((user) => {
        return [<input type="radio"
                       name={this.radioName}
                       onclick={this.selectedUser.bind(this, user)}/>, user.login_name, user.display_name, user.email];
      })}/>
    );
  }
}
