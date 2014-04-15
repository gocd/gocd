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

require File.expand_path(File.dirname(__FILE__) + '/../../../spec_helper')

describe "/api/feeds/index" do
  before(:each) do
    template.stub(:page_url => "http://test.host/pipelines/pipeline-name/1/stage-name/1")
  end

  describe "stage feed has 2 entries" do
    include GoUtil

    before(:each) do
      @date = java_date_utc(2004, 12, 25, 12, 0, 0)
      @entry1 = com.thoughtworks.go.domain.feed.stage.StageFeedEntry.new(1, 10,
                com.thoughtworks.go.domain.StageIdentifier.new("pipeline-name", 1, 'pipeline-label', 'stage-name', 'stage-counter'), 99, @date, StageResult::Passed, "manual", "loser>boozer")

      @entry1.addAuthor(Author.new("user<loser", "loser@gmail.com"))
      @entry1.addAuthor(Author.new("user>boozer", "boozer@gmail.com"))

      @entry1.addCard(MingleCard.new(MingleConfig.new("https://host", "project-evil"), "007"))
      @entry1.addCard(MingleCard.new(MingleConfig.new("https://boast", "project-dead"), "666"))

      @entry2 = com.thoughtworks.go.domain.feed.stage.StageFeedEntry.new(2, 11,
                com.thoughtworks.go.domain.StageIdentifier.new("pipeline-name", 2, 'pipeline-label', 'stage-name', 'stage-counter'), 100, @date, StageResult::Cancelled, "success", "random_guy")

      @entry2.addAuthor(Author.new("user anonymous", nil))
      @entry2.addCard(MingleCard.new(MingleConfig.new("https://ghost", "project.happy"), "42"))

      feed = stub(:updated_date => "TIME IN ISO8601",
                  :entries => [@entry2, @entry1],
                  :first => 1,
                  :last => 2)
      assigns[:title]="pipeline_name"
      template.stub(:pipeline_url => "http://test.host/go/api/pipelines/pipeline-name/10.xml")
      template.stub(:url=>"http://test.host/api/feeds/stages.xml", :resource_url=>"http://test.host/api/feeds/stage.xml")
      assigns[:feed] = feed
    end

    it "should have a title" do
      render '/api/pipelines/stage_feed.xml'
      response.body.should have_tag("feed") do
        with_tag "title", "pipeline_name"
      end
    end

    it "should render the title of a feed entry" do
      render '/api/pipelines/stage_feed.xml'

      response.body.should have_tag("feed entry") do
        with_tag "title", "pipeline-name(1) stage stage-name(stage-counter) Passed"
      end
    end

    it "should include link to the UI page" do
      render '/api/pipelines/stage_feed.xml'
      response.body.should have_tag("link[rel='alternate'][type='text/html'][href='http://test.host/pipelines/pipeline-name/1/stage-name/1'][title='stage-name Stage Detail']")
    end

    it "should list mingle cards with project url" do
      render '/api/pipelines/stage_feed.xml'

      response.body.should have_tag("entry") do
        with_tag("title", "pipeline-name(1) stage stage-name(stage-counter) Passed")

        with_tag("link[href=?][rel='http://www.thoughtworks-studios.com/ns/go#related'][type='application/vnd.mingle+xml'][title='#007']", "https://host/api/v2/projects/project-evil/cards/007.xml")
        with_tag("link[href=?][rel='http://www.thoughtworks-studios.com/ns/go#related'][type='text/html'][title='#007']", "https://host/projects/project-evil/cards/007")

        with_tag("link[href=?][rel='http://www.thoughtworks-studios.com/ns/go#related'][type='application/vnd.mingle+xml'][title='#666']", "https://boast/api/v2/projects/project-dead/cards/666.xml")
        with_tag("link[href=?][rel='http://www.thoughtworks-studios.com/ns/go#related'][type='text/html'][title='#666']", "https://boast/projects/project-dead/cards/666")
      end

      response.body.should have_tag("entry") do
        with_tag("title", "pipeline-name(2) stage stage-name(stage-counter) Cancelled")

        with_tag("link[href=?][rel='http://www.thoughtworks-studios.com/ns/go#related'][type='application/vnd.mingle+xml'][title='#42']", "https://ghost/api/v2/projects/project.happy/cards/42.xml")
        with_tag("link[href=?][rel='http://www.thoughtworks-studios.com/ns/go#related'][type='text/html'][title='#42']", "https://ghost/projects/project.happy/cards/42")
      end
    end

    it "should list authors" do
      render '/api/pipelines/stage_feed.xml'
      response.body.should have_tag("entry") do
        with_tag("title", "pipeline-name(1) stage stage-name(stage-counter) Passed")

        with_tag("author") do
          with_tag("name", "user<loser")
          with_tag("email", "loser@gmail.com")
        end

        with_tag("author") do
          with_tag("name", "user>boozer")
          with_tag("email", "boozer@gmail.com")
        end
      end

      response.body.should have_tag("entry") do
        with_tag("title", "pipeline-name(2) stage stage-name(stage-counter) Cancelled")

        with_tag("author name", "user anonymous")
      end

      response.should_not have_tag("email", "")
    end

    it "should include link to the resource" do
      render '/api/pipelines/stage_feed.xml'
      response.body.should have_tag("link[rel='alternate'][type='application/vnd.go+xml'][href='http://test.host/api/feeds/stage.xml'][title='stage-name Stage Detail']")
    end

    it "should include link pipeline resource" do
      template.should_receive(:page_url).with(@entry1.getStageIdentifier().getStageLocator()).and_return("entry_1_url")
      template.should_receive(:page_url).with(@entry2.getStageIdentifier().getStageLocator()).and_return("entry_2_url")
      template.should_receive(:page_url).with(@entry1.getStageIdentifier().getStageLocator(), :action => "pipeline").and_return("pipeline_1_url")
      template.should_receive(:page_url).with(@entry2.getStageIdentifier().getStageLocator(), :action => "pipeline").and_return("pipeline_2_url")

      render '/api/pipelines/stage_feed.xml'

      response.body.should have_tag("link[rel='http://www.thoughtworks-studios.com/ns/relations/go/pipeline'][type='application/vnd.go+xml'][href='http://test.host/api/pipelines/pipeline-name/10.xml'][title='pipeline-name Pipeline Detail']")
      response.body.should have_tag("link[rel='http://www.thoughtworks-studios.com/ns/relations/go/pipeline'][type='text/html'][href='pipeline_1_url'][title='pipeline-name Pipeline Detail']")
    end

    it "should contain the feed entry state as a content of the entry" do
      render '/api/pipelines/stage_feed.xml'
      response.body.should have_tag("entry category[term='passed']")
      response.body.should have_tag("entry category[term='cancelled']")
    end


    it "should escape title for xml sanity" do
      assigns[:title] = "St<ages"
      render '/api/pipelines/stage_feed.xml'
      root = dom4j_root_for(response.body)
      root.valueOf("//a:title/.").should == "St<ages"
    end

    it "should have updated as the date of the last updated entry" do
      render '/api/pipelines/stage_feed.xml'
      response.body.should have_tag("updated", "TIME IN ISO8601")
    end

    it "should show go:author entry when triggered manually" do
      render '/api/pipelines/stage_feed.xml'

      root = dom4j_root_for(response.body)
      root.valueOf("//go:author/go:name/.").should == "loser>boozer"
    end
  end

  describe "feed has no items" do

    before(:each) do
      job_feed = stub(:updated_date => "TIME IN ISO8601",
                      :entries => [],
                      :first => nil,
                      :last => nil)
      assigns[:feed] = job_feed
      assigns[:title] = "title"
      template.stub(:url=>"http://test.host/api/feeds/stages.xml",:resource_url=>"http://test.host/api/feeds/stage.xml")
    end

    it "should not include a next link" do
      render '/api/pipelines/stage_feed.xml'
      response.body.should_not have_tag("link[rel='next']")
    end

    it "should not include a previous link" do
      render '/api/pipelines/stage_feed.xml'
      response.body.should_not have_tag("link[rel='previous']")
    end
  end

end
