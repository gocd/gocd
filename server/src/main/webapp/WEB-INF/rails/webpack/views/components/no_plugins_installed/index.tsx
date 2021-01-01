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
import {AbstractExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {ExtensionJSON} from "models/shared/plugin_infos_new/serialization";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Link} from "../link";

interface Attrs<T extends ExtensionJSON> {
  extensionType: AbstractExtensionType<T>;
}

export class NoPluginsOfTypeInstalled<T extends ExtensionJSON> extends MithrilViewComponent<Attrs<T>> {
  view(vnode: m.Vnode<Attrs<T>, this>): m.Children | void | null {
    const message = (
      <div>
        To use this page, you must ensure that there are one or
        more {vnode.attrs.extensionType.humanReadableName()} plugins installed. Please see <Link
        href={vnode.attrs.extensionType.linkForDocs()} target="_blank" externalLinkIcon={true}>this page</Link> for a
        list of supported plugins.
      </div>
    );
    return <FlashMessage type={MessageType.warning} message={message}/>;
  }
}
