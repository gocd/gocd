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
import Stream from "mithril/stream";
import {ButtonGroup, Cancel, Primary} from "views/components/buttons";
import {Form, FormBody} from "views/components/forms/form";
import {CheckboxField, NumberField} from "views/components/forms/input_fields";
import {OperationState} from "views/pages/page_operations";
import styles from "views/pages/server-configuration/index.scss";
import {JobTimeoutAttrs} from "views/pages/server_configuration";

export class JobTimeoutConfigurationWidget extends MithrilViewComponent<JobTimeoutAttrs> {
  private ajaxOperationMonitor = Stream<OperationState>(OperationState.UNKNOWN);

  view(vnode: m.Vnode<JobTimeoutAttrs>): m.Vnode {
    const jobTimeout = vnode.attrs.defaultJobTimeoutVM().entity();
    return <div data-test-id="job-timeout-management-widget" class={styles.formContainer}>
      <FormBody>
        <div class={styles.formHeader}>
          <h2>Configure your default Job Timeout</h2>
        </div>
        <div class={styles.formFields}>
          <Form compactForm={true}>
            <CheckboxField dataTestId="checkbox-for-job-timeout"
                           property={jobTimeout.neverTimeout}
                           label={"Never job timeout"}
                           onchange={() => jobTimeout.defaultJobTimeout(0)}/>
            <NumberField label="Default Job timeout"
                         helpText="the job will get cancel after the given minutes of inactivity"
                         readonly={jobTimeout.neverTimeout()}
                         property={jobTimeout.defaultJobTimeout}
                         required={true}
                         errorText={jobTimeout.errors().errorsForDisplay("defaultJobTimeout")}/>
          </Form>
        </div>
        <div class={styles.buttons}>
          <ButtonGroup>
            <Cancel data-test-id={"cancel"}
                    ajaxOperationMonitor={this.ajaxOperationMonitor}
                    onclick={() => vnode.attrs.onCancel(vnode.attrs.defaultJobTimeoutVM())}>Cancel</Cancel>
            <Primary data-test-id={"save"}
                     ajaxOperation={() => vnode.attrs.onDefaultJobTimeoutSave(jobTimeout)}
                     ajaxOperationMonitor={this.ajaxOperationMonitor}>Save</Primary>
          </ButtonGroup>
        </div>
      </FormBody>
    </div>;
  }

}
