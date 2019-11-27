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

import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {ButtonGroup, Cancel, Primary} from "views/components/buttons";
import {Form, FormBody} from "views/components/forms/form";
import {CheckboxField, NumberField} from "views/components/forms/input_fields";
import {OperationState} from "views/pages/page_operations";
import styles from "views/pages/server-configuration/index.scss";
import {JobTimeoutAttrs} from "views/pages/server_configuration";

interface State {
  isAllowedToCancel: boolean;
}

export class JobTimeoutConfigurationWidget extends MithrilComponent<JobTimeoutAttrs, State> {
  private ajaxOperationMonitor = Stream<OperationState>(OperationState.UNKNOWN);

  view(vnode: m.Vnode<JobTimeoutAttrs, State>): m.Vnode {
    return <div data-test-id="job-timeout-management-widget" class={styles.formContainer}>
      <FormBody>
        <div class={styles.formHeader}>
          <h2>Configure your default Job Timeout</h2>
        </div>
        <div class={styles.formFields}>
          <Form compactForm={true}>
            <CheckboxField dataTestId="checkbox-for-job-timeout"
                           property={vnode.attrs.defaultJobTimeout().neverTimeout}
                           label={"Never job timeout"}
                           onchange={(e) => this.enableSetJobTimeout(e, vnode)}/>
            <NumberField label="Default Job timeout"
                         helpText="the job will get cancel after the given minutes of inactivity"
                         readonly={vnode.attrs.defaultJobTimeout().neverTimeout()}
                         property={vnode.attrs.defaultJobTimeout().defaultJobTimeout}
                         required={true}
                         errorText={vnode.attrs.defaultJobTimeout().errors().errorsForDisplay("defaultJobTimeout")}
                         onchange={() => vnode.state.isAllowedToCancel = true}
            />
          </Form>
        </div>
        <div class={styles.buttons}>
          <ButtonGroup>
            <Cancel data-test-id={"cancel"} ajaxOperation={this.onCancel.bind(this, vnode)}
                    ajaxOperationMonitor={this.ajaxOperationMonitor}
                    disabled={!vnode.state.isAllowedToCancel}>Cancel</Cancel>
            <Primary data-test-id={"save"} ajaxOperation={this.onSave.bind(this, vnode)}
                     ajaxOperationMonitor={this.ajaxOperationMonitor}>Save</Primary>
          </ButtonGroup>
        </div>
      </FormBody>
    </div>;
  }

  onCancel(vnode: m.Vnode<JobTimeoutAttrs, State>) {
    vnode.state.isAllowedToCancel = false;
    return vnode.attrs.onCancel();
  }

  onSave(vnode: m.Vnode<JobTimeoutAttrs, State>) {
    if (vnode.attrs.defaultJobTimeout().isValid()) {
      vnode.state.isAllowedToCancel = false;
      return vnode.attrs.onDefaultJobTimeoutSave(vnode.attrs.defaultJobTimeout());
    }
    return Promise.resolve();
  }

  private enableSetJobTimeout(e: Event, vnode: m.Vnode<JobTimeoutAttrs, State>) {
    vnode.attrs.defaultJobTimeout().defaultJobTimeout(0);
    vnode.state.isAllowedToCancel = true;
  }
}
