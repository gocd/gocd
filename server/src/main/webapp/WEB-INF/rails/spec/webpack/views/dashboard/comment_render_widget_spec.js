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
import {TestHelper} from "views/pages/spec/test_helper";
import {CommentRenderWidget} from "views/dashboard/comment_render_widget";
import m from "mithril";

describe("Comment Render Widget", () => {

  const helper = new TestHelper();

  describe("With Tracking Tool Information", () => {
    function mountView(text, trackingTool) {
      helper.mount(() =>
        m(CommentRenderWidget, {
          text,
          trackingTool
        }));
    }

    afterEach(helper.unmount.bind(helper));

    it('should render with multiple issue IDs', () => {
      const trackingTool = {"link": "http://example.com/${ID}", "regex": "#(\\d+)"};
      const text         = "Fix issues #8076 and #8077";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');

      expect(commentRenderWidget).toHaveHtml("<p>Fix issues <a target='story_tracker' href='http://example.com/8076'>#8076</a> and <a target='story_tracker' href='http://example.com/8077'>#8077</a></p>");
    });

    it('should render using regex without a prefix', () => {
      const trackingTool = {"link": "http://example.com/${ID}", "regex": "(\\d+)"};
      const text         = "Fix issues 8076";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');

      expect(commentRenderWidget).toHaveHtml("<p>Fix issues <a target='story_tracker' href='http://example.com/8076'>8076</a></p>");
    });

    it('should render blank for an empty comment', () => {
      const trackingTool = {"link": "http://example.com/${ID}", "regex": "(\\d+)"};
      const text         = "";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');

      expect(commentRenderWidget).toHaveHtml("<p/>");
    });

    it('should render string without specified regex and link if has groups and none materialize', () => {
      const trackingTool = {
        "link":  "http://mingle05/projects/cce/cards/${ID}",
        "regex": "evo-(\\d+)|evo-"
      };
      const text         = "evo-abc: checkin message";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');

      expect(commentRenderWidget).toHaveHtml("<p>evo-abc: checkin message</p>");
    });

    it('should render string with specified regex and link if has groups and other than first materializes', () => {
      const trackingTool = {
        "link":  "http://mingle05/projects/cce/cards/${ID}",
        "regex": "evo-(\\d+)|evo-(ab)"
      };
      const text         = "evo-abc: checkin message";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml("<p><a target='story_tracker' href='http://mingle05/projects/cce/cards/ab'>evo-ab</a>c: checkin message</p>");
    });

    it('should render string with regex that has sub select', () => {
      const trackingTool = {
        "link":  "http://mingle05/projects/cce/cards/${ID}",
        "regex": "evo-(\\d+)"
      };
      const text         = "evo-111: checkin message";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml("<p><a target='story_tracker' href='http://mingle05/projects/cce/cards/111'>evo-111</a>: checkin message</p>");
    });

    it('should return matched string if regex does not have group', () => {
      const trackingTool = {
        "link":  "http://mingle05/projects/cce/cards/${ID}",
        "regex": "\\d+"
      };
      const text         = "evo-1020: checkin message";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml("<p>evo-<a target='story_tracker' href='http://mingle05/projects/cce/cards/1020'>1020</a>: checkin message</p>");
    });

    it('should return matched string from first group if multiple groups are defined', () => {
      const trackingTool = {
        "link":  "http://mingle05/projects/cce/cards/${ID}",
        "regex": "(\\d+)-(evo\\d+)"
      };
      const text         = "1020-evo1: checkin message";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml("<p><a target='story_tracker' href='http://mingle05/projects/cce/cards/1020'>1020-evo1</a>: checkin message</p>");
    });

    it('should return original string if regex does not match', () => {
      const trackingTool = {
        "link":  "http://mingle05/projects/cce/cards/${ID}",
        "regex": "evo-(\\d+)"
      };
      const text         = "evo-abc: checkin message";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml("<p>evo-abc: checkin message</p>");
    });

    it('should return original string if regex is illegal', () => {
      const trackingTool = {
        "link":  "http://mingle05/projects/cce/cards/${ID}",
        "regex": "++"
      };
      const text         = "evo-abc: checkin message";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml("<p>evo-abc: checkin message</p>");
    });

    it('should render using fixed url if link does not contain variable', () => {
      const trackingTool = {
        "link":  "http://mingle05/projects/cce/cards/wall-E",
        "regex": "(evo-\\d+)"
      };
      const text         = "evo-111: checkin message";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml("<p><a target='story_tracker' href='http://mingle05/projects/cce/cards/wall-E'>evo-111</a>: checkin message</p>");
    });

    it('should use link from configuration regardless of its validity', () => {
      const trackingTool = {
        "link":  "aaa${ID}",
        "regex": "\\d+"
      };
      const text         = "111: checkin message";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml("<p><a target='story_tracker' href='aaa111'>111</a>: checkin message</p>");
    });

    it('should replace based on regex instead of pure string replacement', () => {
      const trackingTool = {
        "link":  "http://mingle05/projects/cce/cards/${ID}",
        "regex": "evo-(\\d+)"
      };
      const text         = "Replace evo-1994.  Don't replace 1994";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml("<p>Replace <a target='story_tracker' href='http://mingle05/projects/cce/cards/1994'>evo-1994</a>.  Don't replace 1994</p>");
    });

    it('should support UTF8', () => {
      const trackingTool = {
        "link":  "http://mingle05/projects/cce/cards/${ID}",
        "regex": "#(\\d+)"
      };
      const text         = "The story #111 is fixed by 德里克. #122 is also related to this";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml("<p>The story <a target='story_tracker' href='http://mingle05/projects/cce/cards/111'>#111</a> is fixed by 德里克. <a target='story_tracker' href='http://mingle05/projects/cce/cards/122'>#122</a> is also related to this");
    });

    it('should escape the whole comment if none is matched', () => {
      const trackingTool = {
        "link":  "",
        "regex": ""
      };
      const text         = "some <string>";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml("<p>some &lt;string&gt;</p>");
    });

    it('should escape dynamic link', () => {
      const trackingTool = {
        "link":  "http://jira.example.com/${ID}",
        "regex": "^ABC-[^ ]+"
      };
      const text         = "ABC-\"><svg/onload=\"alert(1)";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml("<p><a target='story_tracker' href='http://jira.example.com/ABC-&quot;&gt;&lt;svg/onload=&quot;alert(1)'>ABC-&quot;&gt;&lt;svg/onload=&quot;alert(1)</a></p>");
    });

    it('should render escaped entities correctly if they match the project management regex', () => {
      const trackingTool = {
        "link":  "http://jira.example.com/${ID}",
        "regex": "#(\\d+)"
      };
      const text         = "Don't render the apostrophe as a link";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml("<p>Don't render the apostrophe as a link</p>");
    });

    it('should escape text when no match is found', () => {
      const trackingTool = {
        "link":  "http://jira.example.com/${ID}",
        "regex": "#(\\d+)"
      };
      const text         = "<b>This should not be bold</b>";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml("<p>&lt;b&gt;This should not be bold&lt;/b&gt;</p>");
    });

    describe('should render string with specified regex and link', () => {
      let trackingTool = null;
      beforeEach(() => {
        trackingTool = {
          "link":  "http://mingle05/projects/cce/cards/${ID}",
          "regex": "(?:Task |#|Bug )(\\d+)"
        };
      });

      it('should render comment with task', () => {
        const text = "Task 111: checkin message";
        mountView(text, trackingTool);
        const commentRenderWidget = helper.q('.comment');
        expect(commentRenderWidget).toHaveHtml("<p><a target='story_tracker' href='http://mingle05/projects/cce/cards/111'>Task 111</a>: checkin message</p>");
      });

      it('should render comment with bug', () => { //no pun intended
        const text = "Bug 111: checkin message";
        mountView(text, trackingTool);
        const commentRenderWidget = helper.q('.comment');
        expect(commentRenderWidget).toHaveHtml("<p><a target='story_tracker' href='http://mingle05/projects/cce/cards/111'>Bug 111</a>: checkin message</p>");
      });

      it('should render comment with #', () => {
        const text = "#111: checkin message";
        mountView(text, trackingTool);
        const commentRenderWidget = helper.q('.comment');
        expect(commentRenderWidget).toHaveHtml("<p><a target='story_tracker' href='http://mingle05/projects/cce/cards/111'>#111</a>: checkin message</p>");
      });
    });
  });

});
