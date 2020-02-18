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
import {Page} from "views/pages/page";
import {ServerInfoWidget} from "views/pages/server_info/server_info_widget.tsx";

export interface MetaJSON {
  database_schema_version: string;
  go_server_version: string;
  jvm_version: string;
  pipeline_count: number;
  usable_space_in_artifacts_repository: number;
  os_information: string;
}

export class ServerInfoPage extends Page<null, {}> {
  componentToDisplay(vnode: m.Vnode<null, {}>): m.Children {
    const metaInformation = JSON.parse(document.body.getAttribute("data-meta") || "{}") as MetaJSON;
    return <ServerInfoWidget meta={metaInformation}/>;
  }

  pageName(): string {
    return "Server Details";
  }

  fetchData(vnode: m.Vnode<null, {}>): Promise<any> {
    return Promise.resolve();
  }
}
