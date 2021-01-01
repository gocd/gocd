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

import { asSelector } from "helpers/css_proxies";
import m from "mithril";
import copyStyles from "views/components/click_2_copy/index.scss";
import tabStyles from "views/components/tab/index.scss";
import styles from "views/pages/config_repos/index.scss";
import { TestHelper } from "views/pages/spec/test_helper";
import { WebhookUrlGenerator } from "../config_repo_view_model";
import { WebhookSuggestions } from "../webhook_suggestions";

const sel = asSelector(styles);
const tabs = asSelector(tabStyles);
const cp = asSelector(copyStyles);

describe("<WebhookSuggestions/>", () => {
  const helper = new TestHelper();

  afterEach(() => helper.unmount());

  it("renders", () => {
    helper.mount(() => <WebhookSuggestions repoId="my-repo" page={new MockPage()} />);

    expect(helper.q(sel.webhookSuggestions)).toBeInDOM();
    expect(helper.textAll(tabs.tabHead).map((s) => s.toLowerCase())).toEqual(["github", "gitlab", "bitbucket", "self-hosted bitbucket"]);
    expect(helper.text(tabs.tabHead + tabs.active)).toBe("GitHub");
    expect(helper.q(cp.snipNClip)).toBeInDOM();
    expect(helper.text(cp.snippet)).toBe("http://web.hook/github/my-repo");
  });

  it("warns when site urls are not configured", () => {
    helper.mount(() => <WebhookSuggestions repoId="my-repo" page={new MockPage(false)} />);

    expect(helper.byTestId("flash-message-warning")).toBeInDOM();
    expect(helper.textByTestId("flash-message-warning")).toBe(
      "You have not configured your server site URLs " +
      "so the auto-detected host, port, and protocol may " +
      "not be accessible to your git provider. Please make any " +
      "necessary corrections to the URL."
    );
  });

  it("has no warning when site urls are configured", () => {
    helper.mount(() => <WebhookSuggestions repoId="my-repo" page={new MockPage(true)} />);

    expect(helper.byTestId("flash-message-warning")).not.toBeInDOM();
  });

  it("content updates based on selected provider", () => {
    helper.mount(() => <WebhookSuggestions repoId="my-repo" page={new MockPage(true)} />);

    const t = [].slice.call(helper.qa(tabs.tabHead)) as HTMLElement[];

    let index: number = -1;
    const bb = t.find((el, i) => (index = i, "Bitbucket" === el.textContent));

    expect(bb).toBeInDOM();
    expect(index).not.toBe(-1);

    helper.click(bb!);

    const content = helper.qa(tabs.tabContent).item(index);

    expect(helper.text(tabs.tabHead + tabs.active)).toBe("Bitbucket");
    expect(content.classList).not.toContain(tabs.hide);
    expect(helper.text(cp.snippet, content)).toBe("http://web.hook/bitbucket/my-repo");
  });
});

class MockPage implements WebhookUrlGenerator {
  configured = true;

  constructor(configured = true) {
    this.configured = configured;
  }

  webhookUrlFor(type: string, id: string) {
    return `http://web.hook/${type}/${id}`;
  }

  siteUrlsConfigured() {
    return this.configured;
  }
}
