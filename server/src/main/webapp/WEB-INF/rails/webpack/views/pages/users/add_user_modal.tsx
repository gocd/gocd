/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import {bind} from "classnames/bind";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {UserJSON} from "models/users/users";
import {UsersCRUD, UserSearchCRUD} from "models/users/users_crud";
import { v4 as uuid } from 'uuid';
import * as Buttons from "views/components/buttons";
import {FlashMessage, FlashMessageModel, MessageType} from "views/components/flash_message";
import {SearchFieldWithButton} from "views/components/forms/input_fields";
import {Modal, Size} from "views/components/modal";
import {Spinner} from "views/components/spinner";
import {Table} from "views/components/table";
import {OperationState} from "views/pages/page_operations";
import styles from "views/pages/users/index.scss";

const classnames = bind(styles);

export class UserSearchModal extends Modal {

  readonly userResult: Stream<UserJSON[]>               = Stream();
  private readonly radioName                            = `user-search-${uuid()}`;
  private readonly searchText: Stream<string>           = Stream();
  private readonly searchStatus                         = Stream(false);
  private readonly selectedUser: Stream<UserJSON>       = Stream();
  private readonly modalLevelMessage: FlashMessageModel = new FlashMessageModel();

  private pageLevelMessage: FlashMessageModel;
  private refreshUsers: () => Promise<any>;

  private ajaxOperationMonitor = Stream<OperationState>(OperationState.UNKNOWN);

  constructor(message: FlashMessageModel, refreshUsers: () => Promise<any>) {
    super(Size.large);
    this.pageLevelMessage = message;
    this.refreshUsers     = refreshUsers;
  }

  body(): m.Children {
    let optionalErrorMesage: m.Children;

    if (this.modalLevelMessage.hasMessage()) {
      optionalErrorMesage =
        <FlashMessage type={this.modalLevelMessage.type as MessageType} message={this.modalLevelMessage.message}/>;
    }

    return (
      <div>
        {optionalErrorMesage}
        <div class={classnames(styles.searchUser)}>
          <SearchFieldWithButton property={this.searchText}
                                 placeholder={'Search user..'}
                                 dataTestId={"user-search-query"}
                                 onclick={this.search.bind(this)}
                                 buttonDisableReason={"Please type the search query to search user."}/>
        </div>
        {this.renderTable()}
      </div>
    );
  }

  buttons(): m.ChildArray {
    return [
      <Buttons.Primary data-test-id="button-add" ajaxOperationMonitor={this.ajaxOperationMonitor} ajaxOperation={this.addUser.bind(this)} disabled={_.isEmpty(this.selectedUser())}>Import</Buttons.Primary>,
      <Buttons.Cancel data-test-id="button-close" ajaxOperationMonitor={this.ajaxOperationMonitor} onclick={this.close.bind(this)}>Cancel</Buttons.Cancel>
    ];
  }

  title(): string {
    return "Import User";
  }

  private addUser() {
    return UsersCRUD.create(this.selectedUser())
             .then((apiResult) => {
               apiResult.do(() => {
                 this.pageLevelMessage.setMessage(MessageType.success,
                                                  `User '${this.selectedUser().login_name}' added successfully.`);
                 this.refreshUsers();
                 this.close();
               }, (errorResponse) => {
                 this.modalLevelMessage.setMessage(MessageType.alert, JSON.parse(errorResponse.body!).message);
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
                      // vnode.state.onError(JSON.parse(errorResponse.body!).message);
                    });
                  });

  }

  private renderTable() {
    if (this.searchStatus()) {
      return <div class={styles.spinnerWrapper}><Spinner/></div>;
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
