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
import {bind} from "classnames/bind";
import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import s from "underscore.string";
import styles from "./index.scss";

const classnames = bind(styles);

export interface Attrs {
  data: Map<string, m.Children>;
  inline?: boolean;
  "data-test-id"?: string;
}

export class KeyValuePair extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const isInline = vnode.attrs.inline;

    const elements: m.Children[] = [];
    vnode.attrs.data.forEach((value, key) => {

      const dataTestIdForKey   = s.slugify(`key-value-key-${key}`);
      const dataTestIdForValue = s.slugify(`key-value-value-${key}`);

      elements.push(<li class={classnames(styles.keyValueItem, {[styles.keyValueInlineItem]: isInline})} key={key}>
        <label data-test-id={dataTestIdForKey} title={key} class={styles.key}>{key}</label>
        <span data-test-id={dataTestIdForValue}
              class={styles.value}>{KeyValuePair.renderedValue(value)}</span>
      </li>);
    });
    return (
      <ul data-test-id={vnode.attrs['data-test-id']}
          class={classnames(styles.keyValuePair, {[styles.keyValuePairInline]: isInline})}>
        {elements}
      </ul>
    );
  }

  private static renderedValue(value: m.Children) {
    // check booleans, because they're weird in JS :-/
    if (_.isBoolean(value)) {
      // toString() because `false` values will not be rendered
      return (<pre>{value.toString()}</pre>);
    }

    if (_.isNumber(value)) {
      return (<pre>{value}</pre>);
    }

    if (_.isNil(value) || _.isEmpty(value)) {
      return this.unspecifiedValue();
    }

    if (_.isString(value) && s.isBlank(value)) {
      return this.unspecifiedValue();
    }

    // performat some "primitive" types
    if (_.isString(value)) {
      return (<pre>{value}</pre>);
    }
    return value;
  }

  private static unspecifiedValue() {
    return (<em>(Not specified)</em>);
  }
}

export interface KeyValueTitleAttrs {
  title: m.Children;
  image: m.Children;
  titleTestId?: string;
  inline?: boolean;
}

export class KeyValueTitle extends MithrilViewComponent<KeyValueTitleAttrs> {
  view(vnode: m.Vnode<KeyValueTitleAttrs>) {
    const inlineClass = vnode.attrs.inline ? styles.titleInline : '';
    return [
      vnode.attrs.image,
      <h4 data-test-id={vnode.attrs.titleTestId} class={classnames(styles.title, inlineClass)}>{vnode.attrs.title}</h4>
    ];
  }
}
