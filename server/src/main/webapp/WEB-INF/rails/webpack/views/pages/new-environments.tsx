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

import m from "mithril";
import Stream from "mithril/stream";
import {Environments, EnvironmentWithOrigin} from "models/new-environments/environments";
import {EnvironmentsAPIs} from "models/new-environments/environments_apis";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {EnvironmentsWidget} from "views/pages/new-environments/environments_widget";
import {Page, PageState} from "views/pages/page";

export class NewEnvironmentsPage extends Page<null, {}> {
  private readonly message: Stream<string | undefined>          = Stream();
  private readonly messageType: Stream<MessageType | undefined> = Stream();
  private readonly environments: Stream<Environments>           = Stream(new Environments());

  componentToDisplay(vnode: m.Vnode<null, {}>): m.Children {
    return <div>
      <FlashMessage type={this.messageType()!} message={this.message()}/>
      <EnvironmentsWidget environments={this.environments}
                          deleteEnvironment={this.deleteEnvironment.bind(this)}/>
    </div>;
  }

  pageName(): string {
    return "Environments";
  }

  deleteEnvironment(env: EnvironmentWithOrigin) {
    const self = this;
    return env.delete().then((result) => {
      result.do(
        () => {
          self.message(`The environment '${env.name()}' was deleted successfully!`);
          self.messageType(MessageType.success);
        }, (errorResponse) => {
          self.message(JSON.parse(errorResponse.body!).message);
          self.messageType(MessageType.alert);
        }
      );
      //@ts-ignore
    }).then(self.fetchData.bind(self));
  }

  fetchData(vnode: m.Vnode<null, {}>): Promise<any> {
    return EnvironmentsAPIs.all().then((result) =>
                                         result.do((successResponse) => {
                                           this.pageState = PageState.OK;
                                           this.environments(successResponse.body);
                                         }, this.setErrorState));
  }
}
