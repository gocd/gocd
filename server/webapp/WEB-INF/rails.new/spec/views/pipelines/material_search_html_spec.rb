##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
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
##########################GO-LICENSE-END##################################

require 'spec_helper'

describe "pipelines/material_search.html.erb" do
  describe "for scm & dependency material" do
    before(:each)  do
      @material_type = 'GitMaterial'
      @commit_date1 = java.util.Date.new
      @commit_date2 = java.util.Date.new
      @match1 = MatchedRevision.new('search-string', 'rev-1', 'rev-1-long', 'user1', @commit_date1, 'comment1')
      @match2 = MatchedRevision.new('search-string', 'rev-2', 'rev-2-long', 'user2', @commit_date2, 'comment2')
    end

    it "should display revision number, time and material name/url" do
      assign(:matched_revisions, [@match1, @match2])

      render

      Capybara.string(response.body).find('ul li#matched-revision-0').tap do |li|
        expect(li).to have_selector('div.revision', :text => 'rev-1')
        expect(li).to have_selector("div.revision[title='rev-1-long']")
        expect(li).to have_selector('div.user', :text => 'user1')
        expect(li).to have_selector('div.date', :text => @commit_date1.display_time.to_s)
        expect(li).to have_selector('div.comment', :text => 'comment1')
      end

      Capybara.string(response.body).find('ul li#matched-revision-1').tap do |li|
        expect(li).to have_selector('div.revision', :text => 'rev-2')
        expect(li).to have_selector("div.revision[title='rev-2-long']")
        expect(li).to have_selector('div.user', :text => 'user2')
        expect(li).to have_selector('div.date', :text => @commit_date2.display_time.to_s)
        expect(li).to have_selector('div.comment', :text => 'comment2')
      end
    end
  end

  describe "for package material" do
    before(:each)  do
      @material_type = 'PackageMaterial'
      @commit_date1 = java.util.Date.new
      @commit_date2 = java.util.Date.new
      @match1 = MatchedRevision.new('search-string', 'rev-1', 'rev-1-long', 'user1', @commit_date1, '{"TYPE":"PACKAGE_MATERIAL","TRACKBACK_URL":"http://foo"}')
      @match2 = MatchedRevision.new('search-string', 'rev-2', 'rev-2-long', nil, @commit_date2, '{"TYPE":"PACKAGE_MATERIAL","COMMENT":"Built on blrstdgobgr03.","TRACKBACK_URL":"/go/tab/build/detail/go-packages/244/dist/2/rpm"}')
    end

    it "should display revision number, time and material name/url" do
      assign(:material_type, @material_type)
      assign(:matched_revisions, [@match1, @match2])

      render

      Capybara.string(response.body).find('ul li#matched-revision-0').tap do |li|
        expect(li).to have_selector('div.revision', :text => 'rev-1')
        expect(li).to have_selector("div.revision[title='rev-1-long']")
        expect(li).to have_selector('div.user', :text => 'user1')
        expect(li).to have_selector('div.date', :text => @commit_date1.display_time.to_s)
        expect(li).to have_selector('div.comment', :text => "Trackback: http://foo")
      end

      Capybara.string(response.body).find('ul li#matched-revision-1').tap do |li|
        expect(li).to have_selector('div.revision', :text => 'rev-2')
        expect(li).to have_selector("div.revision[title='rev-2-long']")
        expect(li).to have_selector('div.user', :text => 'anonymous')
        expect(li).to have_selector('div.date', :text => @commit_date2.display_time.to_s)
        expect(li).to have_selector('div.comment', :text => 'Built on blrstdgobgr03.')
      end
    end
  end
end
