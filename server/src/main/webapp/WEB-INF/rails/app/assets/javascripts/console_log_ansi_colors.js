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

  // Mirrors the definition in ansi_up.js since it is private :(
  var PacketKind;
  (function (PacketKind) {
    PacketKind[PacketKind["EOS"] = 0] = "EOS";
    PacketKind[PacketKind["Text"] = 1] = "Text";
    PacketKind[PacketKind["Incomplete"] = 2] = "Incomplete";
    PacketKind[PacketKind["ESC"] = 3] = "ESC";
    PacketKind[PacketKind["Unknown"] = 4] = "Unknown";
    PacketKind[PacketKind["SGR"] = 5] = "SGR";
    PacketKind[PacketKind["OSCURL"] = 6] = "OSCURL";
  })(PacketKind || (PacketKind = {}));

  class CrelAnsiUp extends AnsiUp {
    static CLASS = /class="([^"]*)"/;
    static STYLE = /style="([^"]*)"/;

    constructor() {
      super();
      super.use_classes = true;
      super.escape_html = false; // handled by crel
    }

    ansi_to_crel(txt) {
      this.append_buffer(txt);
      const blocks = [];
      while (true) {
        const packet = this.get_next_packet();
        if ((packet.kind === PacketKind.EOS) || (packet.kind === PacketKind.Incomplete)) {
          break;
        } else if ((packet.kind === PacketKind.ESC) || (packet.kind === PacketKind.Unknown)) {
          continue;
        } else if (packet.kind === PacketKind.Text) {
          blocks.push(this.transform_to_crel(this.with_state(packet)));
        } else if (packet.kind === PacketKind.SGR) {
          this.process_ansi(packet);
        } else if (packet.kind === PacketKind.OSCURL) {
          blocks.push(this.process_hyperlink_to_crel(packet));
        }
      }
      return blocks.length === 1 ? blocks[0] : blocks;
    }

    transform_to_crel(fragment) {
      const html = super.transform_to_html(fragment);

      if (!html.startsWith("<span")) {
        return html;
      }

      const node_attrs = {};

      const classes = html.match(CrelAnsiUp.CLASS);
      if (classes.length) {
        node_attrs["class"] = classes[1];
      }

      const styles = html.match(CrelAnsiUp.STYLE);
      if (styles.length) {
        node_attrs.style = styles[1];
      }

      return c("span", node_attrs, fragment.text);
    }

    process_hyperlink_to_crel(pkt) {
      const parts = pkt.url.split(':');
      if (parts.length < 1 || !this._url_allowlist[parts[0]]) {
        return "";
      }

      return c("a", { "href": pkt.url }, pkt.text);
    }
  }

  window.CrelAnsiUp = CrelAnsiUp;
})(crel);
