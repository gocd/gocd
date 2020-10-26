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
import _ from "lodash";
import m from "mithril";
import {Accessor, bidirectionalTransform} from "models/base/accessor";
import {Errors} from "models/mixins/errors";
import {RadioField} from "views/components/forms/input_fields";

type AutoUpdateRadioOptions = "auto" | "manual";

interface Attrs {
  noun?:     string; // i.e., the speciic word used to describe this material; default is "repository"
  disabled?: boolean;
  toggle: Accessor<boolean>;
  errors?: Errors;
}

export class MaterialAutoUpdateToggle extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const { noun, toggle, errors } = withDefaults(vnode.attrs);
    const autoUpdate = this.radioState(toggle);

    return <RadioField<AutoUpdateRadioOptions>
      label={`${_.capitalize(noun)} polling behavior:`}
      property={autoUpdate}
      readonly={!!vnode.attrs.disabled}
      errorText={errors?.errorsForDisplay("autoUpdate")}
      dataTestId="material-auto-update"
      possibleValues={[
        {label: `Regularly fetch updates to this ${noun}`, value: "auto"},
        {label: `Fetch updates to this ${noun} only on webhook or manual trigger`, value: "manual"},
      ]}/>;
  }

  radioState(backing: Accessor<boolean>) {
    return bidirectionalTransform(backing, MaterialAutoUpdateToggle.optsToBool, MaterialAutoUpdateToggle.boolToOpts);
  }

  private static optsToBool = (v: AutoUpdateRadioOptions) => (v || "auto").toLowerCase() === "auto"; // "auto" => true; "manual" => false
  private static boolToOpts: (v: boolean) => AutoUpdateRadioOptions = (v) => (v === void 0 || v) ? "auto" : "manual";
}

export class DependencyIgnoreSchedulingToggle extends MithrilViewComponent<Omit<Attrs, "noun">> {
  view(vnode: m.Vnode<Omit<Attrs, "noun">>) {
    const { toggle, errors } = vnode.attrs;
    const ignoreForScheduling = this.radioState(toggle);

    return <RadioField<AutoUpdateRadioOptions>
      label={["Whenever the ", <em>upstream pipeline</em>, " passes:"]}
      property={ignoreForScheduling}
      readonly={!!vnode.attrs.disabled}
      errorText={errors?.errorsForDisplay("ignoreForScheduling")}
      dataTestId="material-ignore-for-scheduling"
      possibleValues={[
        {label: "Run this pipeline", value: "auto"},
        {label: "Do not run this pipeline", value: "manual"},
      ]}/>;
  }

  radioState(backing: Accessor<boolean>) {
    return bidirectionalTransform(backing, DependencyIgnoreSchedulingToggle.optsToBool, DependencyIgnoreSchedulingToggle.boolToOpts);
  }

  private static optsToBool = (v: AutoUpdateRadioOptions) => (v || "manual").toLowerCase() === "manual"; // "auto" => false; "manual" => true
  private static boolToOpts: (v: boolean) => AutoUpdateRadioOptions = (v) => (v === void 0 || !v) ? "auto" : "manual";
}

function withDefaults(attrs: Attrs): Attrs {
  return _.assign({ noun: DEFAULT_NOUN }, attrs);
}

const DEFAULT_NOUN = "repository";
