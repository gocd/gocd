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

import classnames from "classnames";
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import * as Prism from "prismjs";
import defaultStyles from "views/pages/pac/styles.scss";

import "prismjs/components/prism-json";
import "prismjs/components/prism-yaml";
import "prismjs/plugins/custom-class/prism-custom-class";

Prism.plugins.customClass.map(defaultStyles);

interface Attrs {
  content: () => string;
  mimeType: () => string;
}

interface LanguageSpec {
  grammar: Prism.Grammar;
  name: string;
  comment: string;
}

export class PreviewPane extends MithrilComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const language = Language.from(vnode.attrs.mimeType());
    return <pre class={classnames(defaultStyles.previewPane, `language-${language.name}`)}><code>
      {this.content(language, vnode.attrs.content())}
    </code></pre>;
  }

  content(language: LanguageSpec, content: string) {
    return content ?
      m.trust(Prism.highlight(content, language.grammar, language.name)) :
      this.comments(language, "Your Pipelines as Code definition\nwill automatically update here");
  }

  comments(language: LanguageSpec, ...lines: string[]) {
    const len = lines.length;
    const classes = classnames(defaultStyles.token, defaultStyles.comment);
    const token = language.comment;

    return <span class={defaultStyles.initialMessage}>{
    lines.reduce((memo, line, i) => {
      const parts = line.split("\n");

      for (let k = 0, plen = parts.length; k < plen; k++) {
        memo.push(<span class={classes}>{`${token} ${parts[k]}`}</span>);
        if (k + 1 < plen) {
          memo.push("\n");
        }
      }

      if (i + 1 < len) {
        memo.push("\n");
      }

      return memo;
    }, [] as m.ChildArray)}</span>;
  }
}

class Language {
  static from(mime: string): LanguageSpec {
    switch (mime) {
      case "application/x-yaml":
        return {
          grammar: Prism.languages.yaml,
          name: "yaml",
          comment: "#"
        };
      case "application/json":
      default:
        return {
          grammar: Prism.languages.json,
          name: "json",
          comment: "//" // yeah, I know JSON doesn't really support comments.
        };
    }
  }
}
