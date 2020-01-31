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
import m from "mithril";
import {Stage, Stages} from "models/compare/pipeline_instance";
import styles from "./stages.scss";

const classnames = bind(styles);

interface Attrs {
  stages: Stages;
  onClick?: (stage: Stage) => void;
}

export class StagesWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>): m.Children {
    const stages = vnode.attrs.stages.map((stage) => {
      const title = `${stage.name()} (${stage.status()})`;
      if (vnode.attrs.onClick) {
        const onclick = (e: MouseEvent) => {
          e.stopPropagation();
          vnode.attrs.onClick!(stage);
        };
        return <td title={title}
                   className={classnames(styles.stage, StagesWidget.stageStatusClass(stage.status()), styles.clickable)}
                   onclick={onclick}/>;
      }
      return <td title={title}
                 class={classnames(styles.stage, StagesWidget.stageStatusClass(stage.status()))}/>;
    });

    return <table data-test-id="stages" class={styles.stagesContainer}>
      <tr>{stages}</tr>
    </table>;
  }

  private static stageStatusClass(status: string) {
    if (!status) {
      return styles.unknown;
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
      default:
        return styles.unknown;
    }
  }
}
