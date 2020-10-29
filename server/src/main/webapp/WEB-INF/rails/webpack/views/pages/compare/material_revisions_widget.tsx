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

import {TrackingTool} from "helpers/render_comment";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {MaterialRevisions} from "models/compare/compare";
import {PipelineConfig, TrackingTool as Tool} from "models/pipeline_configs/pipeline_config";
import {Table} from "views/components/table";
import {CommentRenderWidget} from "views/dashboard/comment_render_widget";
import styles from "./index.scss";
import {PipelineInstanceWidget} from "./pipeline_instance_widget";

interface MaterialRevisionsAttrs {
  result: MaterialRevisions;
  pipelineConfig: PipelineConfig;
}

export class MaterialRevisionsWidget extends MithrilViewComponent<MaterialRevisionsAttrs> {
  view(vnode: m.Vnode<MaterialRevisionsAttrs, this>): m.Children | void | null {
    const data = vnode.attrs.result.map((materialRev) => {
      return [
        <div className={styles.truncate}><span title={materialRev.revisionSha}>{materialRev.revisionSha}</span></div>
        , <div>{materialRev.modifiedBy}</div>
        , <div>{PipelineInstanceWidget.getTimeToDisplay(materialRev.modifiedAt)}</div>
        , <div className={styles.commitMsg}>
          <CommentRenderWidget text={materialRev.commitMessage} trackingTool={this.createTrackingTool(vnode.attrs.pipelineConfig.trackingTool())}/>
        </div>];
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

  private createTrackingTool(tool: Tool) {
    let trackingTool: TrackingTool;
    if (tool) {
      trackingTool = {link: tool.urlPattern(), regex: escapeDoubleHash(tool.regex())};
    } else {
      trackingTool = {link: "", regex: ""};
    }
    return trackingTool;
  }
}

function escapeDoubleHash(regex: string) {
  if (regex) {
    return regex.replace(/(##)/g, '#');
  }
  return regex;
}
