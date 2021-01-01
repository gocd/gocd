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
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {FlashMessage, MessageType} from "views/components/flash_message";

export interface Attrs {
  message?: string;
}

export class PageLoadError extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return (
      <FlashMessage type={MessageType.alert} message={vnode.attrs.message} dismissible={false}>
        <p>Refresh <a href="javascript: window.location.reload()">this page</a> in some time, and if the problem
          persists, check the server logs.</p>
      </FlashMessage>
    );
  }
}
