/*
 * Copyright Thoughtworks, Inc.
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
(function (c) {
  "use strict";

  class CrelAnsiUp extends AnsiUp {
    constructor() {
      super();
      super.use_classes = true;
    }

    ansi_to_crel(txt) {
      const blocks = this.render_nodes_to_crel(this.ansi_to_structured(txt));
      return blocks.length === 1 ? blocks[0] : blocks;
    }

    render_nodes_to_crel(nodes) {
      return nodes
        .map((node) => this.render_node_to_crel(node))
        .filter((n) => n !== null);
    }

    render_node_to_crel(node) {
      if (node.type === 'text') {
        return node.text;
      } else if (node.type === 'styled') {
        return this.styled_node_to_crel(node);
      } else if (node.type === 'link') {
        return this.hyperlink_to_crel(node);
      }
      return null;
    }

    styled_node_to_crel(node) {
      if (!this.has_styling(node.attrs)) {
        return this.render_nodes_to_crel(node.children);
      }
      const { styles, classes } = this.attrs_to_styles_classes(node.attrs);

      const node_attrs = {};

      if (classes && classes.length) {
        node_attrs["class"] = classes.join(' ');
      }

      if (styles && styles.length) {
        node_attrs["style"] = styles.join('; ');
      }

      return c("span", node_attrs, this.render_nodes_to_crel(node.children));
    }

    hyperlink_to_crel(node) {
      return c("a", { "href": node.url, "target": "_blank" }, this.render_nodes_to_crel(node.children));
    }
  }

  window.CrelAnsiUp = CrelAnsiUp;
})(crel);
