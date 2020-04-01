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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {MaterialRevisions} from "models/compare/compare";
import {Table} from "views/components/table";
import styles from "./index.scss";
import {PipelineInstanceWidget} from "./pipeline_instance_widget";

interface MaterialRevisionsAttrs {
  result: MaterialRevisions;
}

export class MaterialRevisionsWidget extends MithrilViewComponent<MaterialRevisionsAttrs> {
  view(vnode: m.Vnode<MaterialRevisionsAttrs, this>): m.Children | void | null {
    const data = vnode.attrs.result.map((materialRev) => {
      return [
        <div>{materialRev.revisionSha}</div>
        , <div>{materialRev.modifiedBy}</div>
        , <div>{PipelineInstanceWidget.getTimeToDisplay(materialRev.modifiedAt)}</div>
        , <div className={styles.commitMsg}>{materialRev.commitMessage}</div>];
    });
    return <div data-test-id="material-revisions-widget" className={styles.materialModifications}>
      <Table headers={MaterialRevisionsWidget.headers()} data={data}/>
    </div>;
  }

  private static headers() {
    return [
      "Revision"
      , "Modified By"
      , "Modified At"
      , "Comment"
    ];
  }
}
