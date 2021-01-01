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
import {Table} from "views/components/table";
import {MetaJSON} from "views/pages/server_info";
import styles from "./index.scss";

import filesize from "filesize";

interface Attrs {
  meta: MetaJSON;
}

export class ServerInfoWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const meta      = vnode.attrs.meta;
    const data = [
      [<div class={styles.key}>Go Server Version:</div>, meta.go_server_version],
      [<div class={styles.key}>JVM version:</div>, meta.jvm_version],
      [<div class={styles.key}>OS Information:</div>, meta.os_information],
      [<div class={styles.key}>Usable space in artifacts repository:</div>, filesize(meta.usable_space_in_artifacts_repository)],
      [<div class={styles.key}>Pipelines Count:</div>, meta.pipeline_count],
    ];

    return <div class={styles.aboutPage} data-test-id="about-page">
      <h1>Server Info</h1>
      <Table headers={[] as any} data={data}/>
    </div>;
  }
}
