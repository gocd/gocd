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
import {bind} from "classnames/bind";
import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {PipelineInstance, Stages} from "models/compare/pipeline_instance";
import s from "underscore.string";
import {TextField} from "views/components/forms/input_fields";
import {Link} from "views/components/link";
import styles from "./index.scss";

const classnames = bind(styles);

type StringOrNumber = string | number;

interface InstanceAttrs {
  instance: PipelineInstance;
}

export class InstanceSelectionWidget extends MithrilViewComponent<InstanceAttrs> {

  static dataTestId(...parts: StringOrNumber[]) {
    return s.slugify(parts.join("-").trim().toLowerCase());
  }

  static stageStatusClass(status: string) {
    if (!status) {
      return;
    }
    switch (status.trim().toLowerCase()) {
      case "building":
        return styles.building;
      case "failed":
        return styles.failed;
      case "failing":
        return styles.failing;
      case "cancelled":
        return styles.cancelled;
      case "passed":
        return styles.passed;
      case "waiting":
        return styles.waiting;
      default:
        return styles.unknown;
    }
  }

  view(vnode: m.Vnode<InstanceAttrs, this>): m.Children | void | null {
    const rows        = this.getStages(vnode.attrs.instance.stages);
    const placeholder = "Search for a pipeline instance by label, committer, date, etc.";
    const helpText    = <span>{placeholder} <br/> or <br/>
    <Link onclick={this.browse.bind(this)}>Browse the timeline</Link></span>;
    return <div
      data-test-id={InstanceSelectionWidget.dataTestId("instance", "selection", "widget", vnode.attrs.instance.counter())}
      class={styles.instanceWrapper}>
      <TextField
        placeholder={placeholder}
        helpText={helpText}
        property={vnode.attrs.instance.counter}/>
      <table data-test-id="stages">
        {rows}
      </table>
    </div>;
  }

  private browse(e: MouseEvent) {
    // console.log("Browser timeline: ", e);
  }

  private getStages(stages: Stream<Stages>) {
    const cells: m.Children = [];
    const rows              = stages().map((stage, index) => {
      cells.push(<td>
            <span data-test-id={InstanceSelectionWidget.dataTestId("stage-status", stage.name())}
                  className={classnames(styles.stage, InstanceSelectionWidget.stageStatusClass(stage.status()))}/>
      </td>);
      if (index !== 0 && (index + 1) % 5 === 0) {
        const temp   = _.clone(cells);
        cells.length = 0;
        return <tr>{temp}</tr>;
      }
    });
    if (cells.length > 0) {
      rows.push(<tr>{cells}</tr>);
    }
    return rows;
  }
}
