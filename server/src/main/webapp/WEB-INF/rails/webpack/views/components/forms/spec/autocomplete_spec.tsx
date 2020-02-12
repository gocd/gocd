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

import _ from "lodash";
import m from "mithril";
import "mithril/promise/promise";
import Stream from "mithril/stream";
import * as events from "simulate-event";
import {AutocompleteField, SuggestionProvider} from "views/components/forms/autocomplete";
import {TestHelper} from "views/pages/spec/test_helper";
import css from "../autocomplete.scss";

describe("AutocompleteField", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("generates element structure", () => {
    const model: Stream<string> = Stream();
    helper.mount(() => {
      return <AutocompleteField label="Business Speak" property={model} provider={new CrazyBusinessSpeakProvider()}/>;
    });
    expect(q(helper, "label")).toBeTruthy();
    expect(q(helper, "label").textContent).toBe("Business Speak");
    expect(q(helper, "input[type='text']")).toBeTruthy();
    expect(q(helper, `.${css.awesomplete}`)).toBeTruthy();
    expect(q(helper, `[role="listbox"]`)).toBeTruthy();
    expect(q(helper, `[role="listbox"]`).textContent).toBe("");
  });

  it("binds a model", () => {
    const model: Stream<string> = Stream();
    helper.mount(() => {
      return <AutocompleteField label="Business Speak" property={model} provider={new CrazyBusinessSpeakProvider()}/>;
    });
    const input = q(helper, "input") as HTMLInputElement;
    input.value = "synergy";
    events.simulate(input, "input");
    expect(model()).toBe("synergy");
  });

  it("suggests an item by partial match", (done) => {
    const model: Stream<string> = Stream();
    const provider = new CrazyBusinessSpeakProvider();

    provider.onFinally(() => {
      expect(q(helper, `[role="listbox"]`).textContent).toBe("Synergy");
      expect(q(helper, `[role="listbox"] mark`).textContent).toBe("Syn"); // highlight matching char
      done();
    });

    helper.mount(() => {
      return <AutocompleteField label="Business Speak" property={model} provider={provider}/>;
    });

    const input = q(helper, "input") as HTMLInputElement;
    expect(q(helper, `[role="listbox"]`).textContent).toBe("");

    input.value = "syn";
    events.simulate(input, "input");
  });
});

class CrazyBusinessSpeakProvider extends SuggestionProvider {
  getData(): Promise<Awesomplete.Suggestion[]> {
    return new Promise<Awesomplete.Suggestion[]>((resolve) => {
      resolve([
        "Alignment",
        "Best of breed",
        "Dynamism",
        "It is what it is",
        "Leverage",
        "Synergy",
        "Team Player",
      ]);
    });
  }
}

function q(helper: TestHelper, selector: string): HTMLElement {
  return helper.root!.querySelector(selector) as HTMLElement;
}
