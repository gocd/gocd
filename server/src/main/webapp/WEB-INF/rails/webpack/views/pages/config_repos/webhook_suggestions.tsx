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

import { showIf } from "helpers/utils";
import { MithrilComponent } from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import { CopySnippet } from "views/components/click_2_copy";
import { FlashMessage, MessageType } from "views/components/flash_message";
import { Link } from "views/components/link";
import { Tabs } from "views/components/tab";
import { WebhookUrlGenerator } from "./config_repo_view_model";
import styles from "./index.scss";

/** Represents a git provider choice with a display name and underlying ID */
interface Choice {
  id: string;
  name: string;
}

interface PaneAttrs {
  provider: Choice; // the git provider
  events: string[]; // suggested events used to set up the webhook
  repoId: string;   // the config repo ID
  page: WebhookUrlGenerator;

  onlyForPR(event: string): boolean; // whether or not the event is only required for PR support
}

interface Attrs {
  repoId: string;
  page: WebhookUrlGenerator;
}

export class WebhookSuggestionPane extends MithrilComponent<PaneAttrs> {
  view(vnode: m.Vnode<PaneAttrs>) {
    const { provider, events, repoId, page, onlyForPR } = vnode.attrs;
    const url = page.webhookUrlFor(provider.id, repoId);
    return <div>
      {showIf(!page.siteUrlsConfigured(), () => <FlashMessage
        type={MessageType.warning}
        message={[
          "You have not configured your server site URLs so the auto-detected host, port, ",
          "and protocol may not be accessible to your ", <code>git</code>,
          " provider. Please make any necessary corrections to the URL."
        ]} />)}

      <h3>1. Configure your {provider.name} webhook to <code>POST</code> to this URL:</h3>

      <CopySnippet reader={() => url} />

      <dl>
        <dt><h3>2. Ensure your webhook sends the payload as:</h3></dt>
        <dd>
          <code>Content-Type: application/json</code>
        </dd>
      </dl>

      <h3>3. Configure your webhook to trigger on these events:</h3>

      <ul>
        {_.map(events, (ev) => <li><code>{ev}{showIf(onlyForPR(ev), () => <sup class={styles.ref}>*</sup>)}</code></li>)}
      </ul>

      <p class={styles.footnote}>
        <span class={styles.sigil}>*</span> Only required if you have configured your plugin to use <Link target="_blank"
          externalLinkIcon={true}
          href="https://github.com/gocd-contrib/gocd-groovy-dsl-config-plugin#support-for-branches-and-prs">
          pull request/branch support
        </Link>
      </p>
    </div>;
  }
}

export class WebhookSuggestions extends MithrilComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const configs = this.paneConfigs(vnode.attrs);

    return <div class={styles.webhookSuggestions}>
      <h2>Webhook Configuration <span class={styles.btw}>(select your provider(s) below for specific instructions)</span></h2>

      <Tabs
        tabs={_.map(configs, (c) => c.provider.name)}
        contents={_.map(configs, (c) => <WebhookSuggestionPane {...c} />)}
      />

    </div>;
  }

  private paneConfigs({ repoId, page }: Attrs): PaneAttrs[] {
    return [
      {
        provider: { name: "GitHub", id: "github" },
        events: ["push", "pull_request"],
        onlyForPR(event: string) { return "push" !== event; },
        repoId, page
      },
      {
        provider: { name: "GitLab", id: "gitlab" },
        events: ["Push Hook", "Merge Request Hook"],
        onlyForPR(event: string) { return "Push Hook" !== event; },
        repoId, page
      },
      {
        provider: { name: "Bitbucket", id: "bitbucket" },
        events: ["repo:push", "pullrequest:created", "pullrequest:fulfilled", "pullrequest:rejected"],
        onlyForPR(event: string) { return "repo:push" !== event; },
        repoId, page
      },
      {
        provider: { name: "Self-hosted Bitbucket", id: "hosted_bitbucket" },
        events: ["repo:refs_changed", "pr:opened", "pr:merged", "pr:declined", "pr:delete"],
        onlyForPR(event: string) { return "repo:refs_changed" !== event; },
        repoId, page
      },
    ];
  }
}
