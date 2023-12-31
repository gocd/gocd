#
# Copyright 2024 Thoughtworks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

require 'rails_helper'

describe MaterialsHelper do
  include MaterialsHelper

  before do
    @material = SvnMaterial.new("http://foo:bar@sf.net", "user", "pass", false)
  end

  it "should replace tracking tool text with a link" do
    expect(self).to receive(:go_config_service).at_least(1).times.and_return(service = double("go config service"))
    expect(service).to receive(:getCommentRendererFor).with("pipeline").and_return(TrackingTool.new("http://pavan/${ID}", "\\d+"))

    comment_str = "#42 What is the question sir?"
    expect(render_tracking_tool_link_for_comment(comment_str, 'pipeline')).to eq("<p>#<a href=\"http://pavan/42\" target=\"story_tracker\">42</a> What is the question sir?</p>")
  end

  describe "render_comment_markup_for" do
    it "should display comment and trackback url" do
      comment_str = '{"TYPE":"PACKAGE_MATERIAL","COMMENT":"Built on blrstdgobgr03.","TRACKBACK_URL":"google.com"}'
      expect(render_comment_markup_for(comment_str, 'pipeline')).to eq("Built on blrstdgobgr03.<br>Trackback: <a href=\"google.com\">google.com</a>")
    end

    it "should display trackback url when comment is not provided" do
      comment_str = '{"TYPE":"PACKAGE_MATERIAL", "TRACKBACK_URL":"google.com"}'
      expect(render_comment_markup_for(comment_str, 'pipeline')).to eq("Trackback: <a href=\"google.com\">google.com</a>")
    end

    it "should display trackback url as not provided when trackback_url and comment are not there" do
      comment_str = '{"TYPE":"PACKAGE_MATERIAL"}'
      expect(render_comment_markup_for(comment_str, 'pipeline')).to eq("Trackback: Not Provided")
    end

    it "should render a nil comment as empty" do
      expect(self).to receive(:go_config_service).at_least(1).times.and_return(service = double("go config service"))
      expect(service).to receive(:getCommentRendererFor).with("pipeline").and_return(TrackingTool.new("http://somehost/${ID}", "\\d+"))

      comment_str = nil
      expect(render_comment_markup_for(comment_str, 'pipeline')).to eq("<p></p>")
    end
  end
end
