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

import {timeFormatter} from "helpers/time_formatter";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {PipelineInstance} from "models/compare/pipeline_instance";
import {dateOrUndefined} from "models/compare/pipeline_instance_json";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {KeyValuePair} from "views/components/key_value_pair";
import styles from "./modal.scss";
import {TimelineModal} from "./timeline_modal";

interface InstanceAttrs {
  instance: PipelineInstance;
}

export class PipelineInstanceWidget extends MithrilViewComponent<InstanceAttrs> {
  view(vnode: m.Vnode<InstanceAttrs, this>): m.Children | void | null {
    if (!vnode.attrs.instance) {
      return <FlashMessage type={MessageType.alert} message="Please select an instance!"/>;
    }
    const scheduledDate = vnode.attrs.instance.stages().getScheduledDate();
    return <div data-test-id="pipeline-instance-widget">
      <h3 class={styles.pipelineInstanceCounter}>{vnode.attrs.instance.counter()}</h3>
      <div class={styles.pipelineInstanceDescription}>
        <table data-test-id="pipeline-instance-stages">
          <tr>
            {vnode.attrs.instance.stages().map((stage) => {
              return <td><span class={TimelineModal.stageStatusClass(stage.status())}/></td>;
            })}
          </tr>
        </table>
        <div data-test-id="triggered-by">
          Triggered
          by {vnode.attrs.instance.buildCause().getApprover()} on {PipelineInstanceWidget.getTimeToDisplay(scheduledDate)}
        </div>
      </div>
      <div data-test-id="instance-material-revisions" class={styles.materialRevisions}>
        {this.getMaterialRevisions(vnode)}
      </div>
    </div>;
  }

  private static getTimeToDisplay(date: dateOrUndefined | null): m.Child {
    return <span title={timeFormatter.formatInServerTime(date)}>{timeFormatter.format(date)}</span>;
  }

  private getMaterialRevisions(vnode: m.Vnode<InstanceAttrs, this>) {
    return vnode.attrs.instance.buildCause().materialRevisions().map((materialRev) => {
      switch (materialRev.material().type().toLowerCase()) {
        case "pipeline":
          return materialRev.modifications().map((modification) => {
            const data: Map<string, m.Children> = new Map<string, m.Children>();
            data.set("Revision", modification.revision());
            data.set("Material", materialRev.material().description());
            data.set("Modified On", PipelineInstanceWidget.getTimeToDisplay(modification.modifiedTime()));
            return <KeyValuePair data={data}/>;
          });
        default:
          return materialRev.modifications().map((modification) => {
            const data: Map<string, m.Children> = new Map<string, m.Children>();
            data.set("Revision", modification.revision());
            data.set("Username", modification.userName());
            data.set("Comment", modification.comment());
            data.set("Modified On", PipelineInstanceWidget.getTimeToDisplay(modification.modifiedTime()));
            return <KeyValuePair data={data}/>;
          });
      }
    });
  }
}
