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

import {JsonUtils} from "helpers/json_utils";
import m from "mithril";
import Stream from "mithril/stream";
import {MailServerCrud} from "models/mail_server/mail_server_crud";
import {MailServer} from "models/mail_server/types";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Attrs, MailServerWidget} from "views/pages/mail_server/mail_server_widget";
import {Page} from "views/pages/page";
import {OperationState, SaveOperation} from "views/pages/page_operations";

interface State extends Attrs, SaveOperation {
}

export class MailServerPage extends Page<null, State> {
  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    return (
      <div>
        <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>
        <MailServerWidget {...vnode.state}/>
      </div>
    );
  }

  pageName(): string {
    return "Email server";
  }

  oninit(vnode: m.Vnode<null, State>) {
    vnode.state.operationState = Stream(OperationState.UNKNOWN) as Stream<OperationState>;
    vnode.state.mailserver     = Stream(new MailServer());

    vnode.state.onSuccessfulSave = (msg: m.Children) => {
      vnode.state.operationState(OperationState.DONE);
      this.flashMessage.setMessage(MessageType.success, msg);
      this.fetchData(vnode);
    };

    vnode.state.onsave = () => {
      vnode.state.operationState(OperationState.IN_PROGRESS);
      MailServerCrud.createOrUpdate(vnode.state.mailserver()).then((result) => {
        result.do(
          (successResponse) => {
            vnode.state.onSuccessfulSave("Configuration was saved successfully!");
          },
          (errorResponse) => {
            vnode.state.operationState(OperationState.DONE);
            if (result.getStatusCode() === 422 && errorResponse.body) {
              vnode.state.mailserver(MailServer.fromJSON(JsonUtils.toCamelCasedObject(JSON.parse(errorResponse.body)).data));
            } else {
              vnode.state.onError(errorResponse.message);
            }
          });
      });
    };

    vnode.state.onError = (msg: m.Children) => {
      this.flashMessage.setMessage(MessageType.alert, msg);
    };

    super.oninit(vnode);
  }

  fetchData(vnode: m.Vnode<null, State>) {
    return MailServerCrud.get()
                         .then((apiResult) => {
                           apiResult.do((successResponse) => {
                             vnode.state.mailserver(successResponse.body);
                           }, () => {
                             this.setErrorState();
                           });
                         });
  }
}
