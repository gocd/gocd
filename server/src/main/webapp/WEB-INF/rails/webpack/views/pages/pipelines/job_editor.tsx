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
import {Job} from "models/pipeline_configs/job";
import {IdentifierInputField} from "views/components/forms/common_validating_inputs";
import {Form, FormBody} from "views/components/forms/form";
import {IDENTIFIER_FORMAT_HELP_MESSAGE} from "./messages";

interface Attrs {
  job: Job;
}

export class JobEditor extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return <FormBody>
      <Form last={true} compactForm={true}>
        <IdentifierInputField label="Job Name" helpText={IDENTIFIER_FORMAT_HELP_MESSAGE} placeholder="e.g., run-unit-tests" property={vnode.attrs.job.name} errorText={vnode.attrs.job.errors().errorsForDisplay("name")} required={true}/>
      </Form>
    </FormBody>;
  }
}
