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

import classNames from "classnames/bind";
import {timeFormatter} from "helpers/time_formatter";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {MaterialRevision, MaterialRevisions, PipelineInstance, PipelineInstances} from "models/compare/pipeline_instance";
import {Dropdown, DropdownAttrs} from "views/components/buttons";
import {HelpText, SearchField} from "views/components/forms/input_fields";
import {Link} from "views/components/link";
import {Spinner} from "views/components/spinner";
import spinnerCss from "../agents/spinner.scss";
import styles from "./index.scss";
import {ApiService, InstanceAttrs, InstanceSelectionWidget} from "./instance_selection_widget";
import {PipelineInstanceWidget} from "./pipeline_instance_widget";
import {StagesWidget} from "./stages/stages_widget";
import {TimelineModal} from "./timeline_modal";

const classnames = classNames.bind(styles);

interface Attrs extends InstanceAttrs {
  apiService: ApiService;
}

export class SelectInstanceWidget extends Dropdown<DropdownAttrs & Attrs> {
  private pattern: Stream<string>                      = Stream();
  private operationInProgress: Stream<boolean>         = Stream();
  private matchingInstances: Stream<PipelineInstances> = Stream();

  oninit(vnode: m.Vnode<DropdownAttrs & Attrs>) {
    super.oninit(vnode);
    this.pattern(vnode.attrs.instance.counter() + "");
  }

  protected doRenderButton(vnode: m.Vnode<DropdownAttrs & Attrs>): m.Children {
    const placeholder = "Search for a pipeline instance by label, committer, date, etc.";
    const helpText    = <Link onclick={this.browse.bind(this, vnode)}>Browse the timeline</Link>;
    return <div>
      <label class={styles.label}>{placeholder}</label>
      <SearchField
        placeholder={placeholder}
        property={this.pattern}
        onchange={() => this.onPatternChange(vnode)}/>
      <HelpText helpText={helpText} helpTextId={"help-text"}/>
    </div>;
  }

  protected doRenderDropdownContent(vnode: m.Vnode<DropdownAttrs & Attrs>): m.Children {
    if (!vnode.attrs.show()) {
      return;
    }
    if (this.operationInProgress()) {
      return <div class={styles.dropdownContentWrapper}>
        <div data-test-id="spinner-div" className={classnames(styles.dropdownContent, styles.spinnerWrapper)}>
          <Spinner small={true} css={spinnerCss}/>
        </div>
      </div>;
    }

    if (this.matchingInstances().length === 0) {
      return <div className={styles.dropdownContentWrapper}>
        <div data-test-id="no-results-div" className={styles.dropdownContent}>
          <div class={styles.info}>No matching results!</div>
        </div>
      </div>;
    }
    return <div class={styles.dropdownContentWrapper}>
      <div data-test-id="matching-instances-results" class={styles.dropdownContent}>
        <ul class={styles.instancesList}>
          {this.matchingInstances().map((instance) => {
            return <li data-test-id={InstanceSelectionWidget.dataTestId("instance", instance.counter())}
                       class={styles.listItem} onclick={this.onInstanceSelection.bind(this, instance, vnode)}>
              <div>
                <h5>{instance.label()}</h5>
                <div>
                  <StagesWidget stages={instance.stages()}/>
                  <span data-test-id="triggered-by" class={styles.label}>
                    Triggered
                    by {instance.buildCause().getApprover()} on {PipelineInstanceWidget.getTimeToDisplay(instance.scheduledDate())}
                  </span>
                  {this.materialRevisions(instance.buildCause().materialRevisions().filterRevision(this.pattern()))}
                </div>
              </div>
            </li>;
          })}
        </ul>
      </div>
    </div>;
  }

  private onInstanceSelection(instance: PipelineInstance, vnode: m.Vnode<DropdownAttrs & Attrs>, e: MouseEvent) {
    e.stopPropagation();
    vnode.attrs.onInstanceChange(instance.counter());
  }

  private materialRevisions(materialRevison: MaterialRevisions) {
    return materialRevison.map((revision) => {
      if (revision.material().type().toLowerCase() !== "pipeline") {
        return this.materialRevision(revision);
      } else {
        return this.pipelineRevision(revision);
      }
    });
  }

  private materialRevision(revision: MaterialRevision) {
    return revision.modifications().map((modification, index) => {
      return <table data-test-id={InstanceSelectionWidget.dataTestId("modification", index)}
                    class={styles.modification}>
        <tr>
          <th>Revision</th>
          <td>{modification.revision()}</td>
        </tr>
        <tr>
          <th>Comment</th>
          <td>{modification.comment()}</td>
        </tr>
        <tr>
          <th>Modified by</th>
          <td>{`${modification.userName()} on ${timeFormatter.format(modification.modifiedTime())}`}</td>
        </tr>
      </table>;
    });
  }

  private pipelineRevision(revision: MaterialRevision) {
    return revision.modifications().map((modification, index) => {
      return <table data-test-id={InstanceSelectionWidget.dataTestId("pipeline-modification", index)}
                    class={styles.modification}>
        <tr>
          <th>Revision</th>
          <td>{modification.revision()}</td>
        </tr>
        <tr>
          <th>Comment</th>
          <td>{modification.pipelineLabel()}</td>
        </tr>
        <tr>
          <th>Modified On</th>
          <td>{timeFormatter.format(modification.modifiedTime())}</td>
        </tr>
      </table>;
    });
  }

  private onPatternChange(vnode: m.Vnode<DropdownAttrs & Attrs>): any {
    if (_.isEmpty(this.pattern())) {
      vnode.attrs.show(false);
      return;
    }
    vnode.attrs.show(true);
    this.operationInProgress(true);

    vnode.attrs.apiService.getMatchingInstances(vnode.attrs.instance.name(), this.pattern()
      , (successResponse) => {
        this.matchingInstances(successResponse);
        this.operationInProgress(false);
      }
      , (errorResponse) => {
        this.operationInProgress(false);
      });
  }

  private browse(vnode: m.Vnode<DropdownAttrs & Attrs>, e: MouseEvent) {
    e.stopPropagation();
    new TimelineModal(vnode.attrs.instance.name(), vnode.attrs.onInstanceChange).render();
  }
}
