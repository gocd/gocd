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

      expect(commentRenderWidget).toHaveHtml('<p>Fix issues <a href="http://example.com/8076" target="story_tracker">#8076</a> and <a href="http://example.com/8077" target="story_tracker">#8077</a></p>');
    });

    it('should render using regex without a prefix', () => {
      const trackingTool = {"link": "http://example.com/${ID}", "regex": "(\\d+)"};
      const text         = "Fix issues 8076";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');

      expect(commentRenderWidget).toHaveHtml('<p>Fix issues <a href="http://example.com/8076" target="story_tracker">8076</a></p>');
    });

    it('should render blank for an empty comment', () => {
      const trackingTool = {"link": "http://example.com/${ID}", "regex": "(\\d+)"};
      const text         = "";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');

      expect(commentRenderWidget).toHaveHtml('<p/>');
    });

    it('should render string without specified regex and link if has groups and none materialize', () => {
      const trackingTool = {
        "link":  "http://mingle05/projects/cce/cards/${ID}",
        "regex": "evo-(\\d+)|evo-"
      };
      const text         = "evo-abc: checkin message";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');

      expect(commentRenderWidget).toHaveHtml('<p>evo-abc: checkin message</p>');
    });

    it('should render string with specified regex and link if has groups and other than first materializes', () => {
      const trackingTool = {
        "link":  "http://mingle05/projects/cce/cards/${ID}",
        "regex": "evo-(\\d+)|evo-(ab)"
      };
      const text         = "evo-abc: checkin message";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml('<p><a href="http://mingle05/projects/cce/cards/ab" target="story_tracker">evo-ab</a>c: checkin message</p>');
    });

    it('should render string with regex that has sub select', () => {
      const trackingTool = {
        "link":  "http://mingle05/projects/cce/cards/${ID}",
        "regex": "evo-(\\d+)"
      };
      const text         = "evo-111: checkin message";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml('<p><a href="http://mingle05/projects/cce/cards/111" target="story_tracker">evo-111</a>: checkin message</p>');
    });

    it('should return matched string if regex does not have group', () => {
      const trackingTool = {
        "link":  "http://mingle05/projects/cce/cards/${ID}",
        "regex": "\\d+"
      };
      const text         = "evo-1020: checkin message";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml('<p>evo-<a href="http://mingle05/projects/cce/cards/1020" target="story_tracker">1020</a>: checkin message</p>');
    });

    it('should return matched string from first group if multiple groups are defined', () => {
      const trackingTool = {
        "link":  "http://mingle05/projects/cce/cards/${ID}",
        "regex": "(\\d+)-(evo\\d+)"
      };
      const text         = "1020-evo1: checkin message";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml('<p><a href="http://mingle05/projects/cce/cards/1020" target="story_tracker">1020-evo1</a>: checkin message</p>');
    });

    it('should return original string if regex does not match', () => {
      const trackingTool = {
        "link":  "http://mingle05/projects/cce/cards/${ID}",
        "regex": "evo-(\\d+)"
      };
      const text         = "evo-abc: checkin message";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml('<p>evo-abc: checkin message</p>');
    });

    it('should return original string if regex is illegal', () => {
      const trackingTool = {
        "link":  "http://mingle05/projects/cce/cards/${ID}",
        "regex": "++"
      };
      const text         = "evo-abc: checkin message";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml('<p>evo-abc: checkin message</p>');
    });

    it('should render using fixed url if link does not contain variable', () => {
      const trackingTool = {
        "link":  "http://mingle05/projects/cce/cards/wall-E",
        "regex": "(evo-\\d+)"
      };
      const text         = "evo-111: checkin message";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml('<p><a href="http://mingle05/projects/cce/cards/wall-E" target="story_tracker">evo-111</a>: checkin message</p>');
    });

    it('should use link from configuration regardless of its validity', () => {
      const trackingTool = {
        "link":  "aaa${ID}",
        "regex": "\\d+"
      };
      const text         = "111: checkin message";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml('<p><a href="aaa111" target="story_tracker">111</a>: checkin message</p>');
    });

    it('should replace based on regex instead of pure string replacement', () => {
      const trackingTool = {
        "link":  "http://mingle05/projects/cce/cards/${ID}",
        "regex": "evo-(\\d+)"
      };
      const text         = "Replace evo-1994.  Don't replace 1994";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml('<p>Replace <a href="http://mingle05/projects/cce/cards/1994" target="story_tracker">evo-1994</a>.  Don\'t replace 1994</p>');
    });

    it('should support UTF8', () => {
      const trackingTool = {
        "link":  "http://mingle05/projects/cce/cards/${ID}",
        "regex": "#(\\d+)"
      };
      const text         = "The story #111 is fixed by 德里克. #122 is also related to this";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml('<p>The story <a href="http://mingle05/projects/cce/cards/111" target="story_tracker">#111</a> is fixed by 德里克. <a href="http://mingle05/projects/cce/cards/122" target="story_tracker">#122</a> is also related to this');
    });

    it('should escape the whole comment if none is matched', () => {
      const trackingTool = {
        "link":  "",
        "regex": ""
      };
      const text         = "some <string>";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml('<p>some &lt;string&gt;</p>');
    });

    it('should escape dynamic link without id group', () => {
      const trackingTool = {
        "link":  "http://jira.example.com/${ID}?hello&gocd=true",
        "regex": "^ABC-[^ ]+"
      };
      const text         = "ABC-\"><svg/onload=\"alert(1)";
      mountView(text, trackingTool);
      expect(helper.q('.comment a')).toHaveAttr('href', "http://jira.example.com/ABC-%22%3E%3Csvg%2Fonload%3D%22alert(1)?hello&gocd=true");
      expect(helper.q('.comment')).toHaveHtml('<p><a href="http://jira.example.com/ABC-%22%3E%3Csvg%2Fonload%3D%22alert(1)?hello&amp;gocd=true" target="story_tracker">ABC-&quot;&gt;&lt;svg/onload=&quot;alert(1)</a></p>');
    });

    it('should escape dynamic link with id group', () => {
      const trackingTool = {
        "link":  "http://jira.example.com/${ID}?hello&gocd=true",
        "regex": "ABC-([^ ]+)"
      };
      const text         = "ABC-\"><svg/onload=\"alert(1)";
      mountView(text, trackingTool);
      expect(helper.q('.comment a')).toHaveAttr('href', "http://jira.example.com/%22%3E%3Csvg%2Fonload%3D%22alert(1)?hello&gocd=true");
      expect(helper.q('.comment')).toHaveHtml('<p><a href="http://jira.example.com/%22%3E%3Csvg%2Fonload%3D%22alert(1)?hello&amp;gocd=true" target="story_tracker">ABC-&quot;&gt;&lt;svg/onload=&quot;alert(1)</a></p>');
    });

    it('should URI encode identifiers', () => {
      const trackingTool = {
        "link":  "http://website.com/${ID}",
        "regex": "ABC-[^ ]+"
      };
      const text         = "ABC-1/2德";
      mountView(text, trackingTool);
      expect(helper.q('.comment a')).toHaveAttr('href', "http://website.com/ABC-1%2F2%E5%BE%B7");
      expect(helper.q('.comment')).toHaveHtml('<p><a href="http://website.com/ABC-1%2F2%E5%BE%B7" target="story_tracker">ABC-1/2德</a></p>');
    });

    it('should URI encode identifiers with id group', () => {
      const trackingTool = {
        "link":  "http://website.com/${ID}",
        "regex": "ABC-([^ ]+)"
      };
      const text         = "ABC-1/2德";
      mountView(text, trackingTool);
      expect(helper.q('.comment a')).toHaveAttr('href', "http://website.com/1%2F2%E5%BE%B7");
      expect(helper.q('.comment')).toHaveHtml('<p><a href="http://website.com/1%2F2%E5%BE%B7" target="story_tracker">ABC-1/2德</a></p>');
    });

    it('should URI encode identifiers into query', () => {
      const trackingTool = {
        "link":  "http://website.com/?id=${ID}",
        "regex": "ABC-[^ ]+"
      };
      const text         = "ABC-1?=2";
      mountView(text, trackingTool);
      expect(helper.q('.comment a')).toHaveAttr('href', "http://website.com/?id=ABC-1%3F%3D2");
      expect(helper.q('.comment')).toHaveHtml('<p><a href="http://website.com/?id=ABC-1%3F%3D2" target="story_tracker">ABC-1?=2</a></p>');
    });

    it('should URI encode identifiers with id group into query', () => {
      const trackingTool = {
        "link":  "http://website.com/?id=${ID}",
        "regex": "ABC-([^ ]+)"
      };
      const text         = "ABC-1?=2";
      mountView(text, trackingTool);
      expect(helper.q('.comment a')).toHaveAttr('href', "http://website.com/?id=1%3F%3D2");
      expect(helper.q('.comment')).toHaveHtml('<p><a href="http://website.com/?id=1%3F%3D2" target="story_tracker">ABC-1?=2</a></p>');
    });

    it('does not double URI encode link', () => {
      const trackingTool = {
        "link":  "http://website.com/${ID}?encoded=%2F%22",
        "regex": "ABC-\\d+"
      };
      const text         = "ABC-123";
      mountView(text, trackingTool);
      expect(helper.q('.comment a')).toHaveAttr('href', "http://website.com/ABC-123?encoded=%2F%22");
      expect(helper.q('.comment')).toHaveHtml('<p><a href="http://website.com/ABC-123?encoded=%2F%22" target="story_tracker">ABC-123</a></p>');
    });

    it('does not double URI encode link with id group', () => {
      const trackingTool = {
        "link":  "http://website.com/${ID}?encoded=%2F%22",
        "regex": "ABC-(\\d+)"
      };
      const text         = "ABC-123";
      mountView(text, trackingTool);
      expect(helper.q('.comment a')).toHaveAttr('href', "http://website.com/123?encoded=%2F%22");
      expect(helper.q('.comment')).toHaveHtml('<p><a href="http://website.com/123?encoded=%2F%22" target="story_tracker">ABC-123</a></p>');
    });

    it('should render escaped entities correctly if they match the project management regex', () => {
      const trackingTool = {
        "link":  "http://jira.example.com/${ID}",
        "regex": "#(\\d+)"
      };
      const text         = "Don't render the apostrophe as a link";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml('<p>Don\'t render the apostrophe as a link</p>');
    });

    it('should escape text when no match is found', () => {
      const trackingTool = {
        "link":  "http://jira.example.com/${ID}",
        "regex": "#(\\d+)"
      };
      const text         = "<b>This should not be bold</b>";
      mountView(text, trackingTool);
      const commentRenderWidget = helper.q('.comment');
      expect(commentRenderWidget).toHaveHtml('<p>&lt;b&gt;This should not be bold&lt;/b&gt;</p>');
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
        expect(commentRenderWidget).toHaveHtml('<p><a href="http://mingle05/projects/cce/cards/111" target="story_tracker">Task 111</a>: checkin message</p>');
      });

      it('should render comment with bug', () => { //no pun intended
        const text = "Bug 111: checkin message";
        mountView(text, trackingTool);
        const commentRenderWidget = helper.q('.comment');
        expect(commentRenderWidget).toHaveHtml('<p><a href="http://mingle05/projects/cce/cards/111" target="story_tracker">Bug 111</a>: checkin message</p>');
      });

      it('should render comment with #', () => {
        const text = "#111: checkin message";
        mountView(text, trackingTool);
        const commentRenderWidget = helper.q('.comment');
        expect(commentRenderWidget).toHaveHtml('<p><a href="http://mingle05/projects/cce/cards/111" target="story_tracker">#111</a>: checkin message</p>');
      });
    });
  });

});
