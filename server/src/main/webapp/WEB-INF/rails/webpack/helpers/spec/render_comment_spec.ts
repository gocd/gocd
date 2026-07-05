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

import {renderCommentToHtml, TrackingTool} from "../render_comment";

describe("renderCommentToHtml", () => {
  const trackingTool: TrackingTool = {regex: "#(\\d+)", link: "http://tracker/${ID}"};

  describe("html format", () => {
    it("returns the text as-is, trusting the server to have already escaped/rendered it", () => {
      expect(renderCommentToHtml('already <a href="x">safe</a>', "html"))
        .toBe('already <a href="x">safe</a>');
    });
  });

  describe("raw format", () => {
    it("escapes html when there is no tracking tool", () => {
      expect(renderCommentToHtml("<script>alert(1)</script>", "raw"))
        .toBe("&lt;script&gt;alert(1)&lt;/script&gt;");
    });

    it("turns tracking ids into links and escapes the rest", () => {
      expect(renderCommentToHtml("fixed #42 <b>now</b>", "raw", trackingTool))
        .toBe('fixed <a href="http://tracker/42" target="story_tracker">#42</a> &lt;b&gt;now&lt;/b&gt;');
    });
  });

  describe("json format", () => {
    it("renders the package comment and trackback as escaped text", () => {
      const comment = '{"COMMENT":"Built here.","TRACKBACK_URL":"https://tracker.example/42"}';
      expect(renderCommentToHtml(comment, "json"))
        .toBe("Built here.\nTrackback: https://tracker.example/42");
    });

    it("escapes html in the package comment", () => {
      const comment = '{"COMMENT":"<script>alert(1)</script>","TRACKBACK_URL":"https://google.com"}';
      expect(renderCommentToHtml(comment, "json"))
        .toBe("&lt;script&gt;alert(1)&lt;/script&gt;\nTrackback: https://google.com");
    });

    it("does not render the trackback as a link, even for http(s) urls", () => {
      const comment = '{"TRACKBACK_URL":"https://google.com"}';
      expect(renderCommentToHtml(comment, "json")).not.toContain("<a ");
    });

    it("shows 'Not Provided' when the trackback url is missing", () => {
      expect(renderCommentToHtml('{"COMMENT":"c"}', "json"))
        .toBe("c\nTrackback: Not Provided");
    });

    it("omits the comment line when the comment is absent", () => {
      expect(renderCommentToHtml('{"TRACKBACK_URL":"https://google.com"}', "json"))
        .toBe("Trackback: https://google.com");
    });

    it("falls back to escaping the raw text when the payload is not valid JSON", () => {
      expect(renderCommentToHtml("not json <b>", "json"))
        .toBe("not json &lt;b&gt;");
    });
  });
});
