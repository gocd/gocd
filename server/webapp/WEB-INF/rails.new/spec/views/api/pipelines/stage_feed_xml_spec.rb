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

describe "/api/feeds/index" do
  before(:each) do
    allow(view).to receive(:page_url).and_return("http://test.host/pipelines/pipeline-name/1/stage-name/1")
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

      feed = double(:updated_date => "TIME IN ISO8601", :entries => [@entry2, @entry1], :first => 1, :last => 2)
      assign(:title, "pipeline_name")
      allow(view).to receive(:pipeline_url).and_return("http://test.host/go/api/pipelines/pipeline-name/10.xml")
      allow(view).to receive(:url).and_return("http://test.host/api/feeds/stages.xml")
      allow(view).to receive(:resource_url).and_return("http://test.host/api/feeds/stage.xml")

      assign(:feed, feed)
    end

    it "should have a title" do
      render :template => '/api/pipelines/stage_feed.xml.erb'

      doc = Nokogiri::XML(response.body)
      feed = atom_xpath(doc, "feed")

      expect(feed).to_not be_nil_or_empty
      expect(atom_xpath(feed, "title").text).to eq("pipeline_name")
    end

    it "should render the title of a feed entry" do
      render :template => '/api/pipelines/stage_feed.xml.erb'

      doc = Nokogiri::XML(response.body)
      entry = atom_xpath(doc, "feed", "entry")

      expect(atom_xpath(entry, "title")[0].text).to eq("pipeline-name(2) stage stage-name(stage-counter) Cancelled")
      expect(atom_xpath(entry, "title")[1].text).to eq("pipeline-name(1) stage stage-name(stage-counter) Passed")
    end

    it "should include link to the UI page" do
      render :template => '/api/pipelines/stage_feed.xml.erb'

      doc = Nokogiri::XML(response.body)
      link = atom_xpath(doc, "feed", "entry", "link[@rel='alternate'][@type='text/html'][@href='http://test.host/pipelines/pipeline-name/1/stage-name/1'][@title='stage-name Stage Detail']")

      expect(link).to_not be_nil_or_empty
    end

    it "should list mingle cards with project url" do
      render :template => '/api/pipelines/stage_feed.xml.erb'

      doc = Nokogiri::XML(response.body)
      cancelled_entry = atom_xpath(doc, "feed", "entry")[0]
      passed_entry = atom_xpath(doc, "feed", "entry")[1]

      passed_entry.tap do |entry|
        expect(atom_xpath(entry, "title").text).to eq("pipeline-name(1) stage stage-name(stage-counter) Passed")

        expect(atom_xpath(entry, "link[@href='https://host/api/v2/projects/project-evil/cards/007.xml'][@rel='http://www.thoughtworks-studios.com/ns/go#related'][@type='application/vnd.mingle+xml'][@title='#007']")).to_not be_nil_or_empty
        expect(atom_xpath(entry, "link[@href='https://host/projects/project-evil/cards/007'][@rel='http://www.thoughtworks-studios.com/ns/go#related'][@type='text/html'][@title='#007']")).to_not be_nil_or_empty

        expect(atom_xpath(entry, "link[@href='https://boast/api/v2/projects/project-dead/cards/666.xml'][@rel='http://www.thoughtworks-studios.com/ns/go#related'][@type='application/vnd.mingle+xml'][@title='#666']")).to_not be_nil_or_empty
        expect(atom_xpath(entry, "link[@href='https://boast/projects/project-dead/cards/666'][@rel='http://www.thoughtworks-studios.com/ns/go#related'][@type='text/html'][@title='#666']")).to_not be_nil_or_empty
      end

      cancelled_entry.tap do |entry|
        expect(atom_xpath(entry, "title").text).to eq("pipeline-name(2) stage stage-name(stage-counter) Cancelled")

        expect(atom_xpath(entry, "link[@href='https://ghost/api/v2/projects/project.happy/cards/42.xml'][@rel='http://www.thoughtworks-studios.com/ns/go#related'][@type='application/vnd.mingle+xml'][@title='#42']")).to_not be_nil_or_empty
        expect(atom_xpath(entry, "link[@href='https://ghost/projects/project.happy/cards/42'][@rel='http://www.thoughtworks-studios.com/ns/go#related'][@type='text/html'][@title='#42']")).to_not be_nil_or_empty
      end
    end

    it "should list authors" do
      render :template => '/api/pipelines/stage_feed.xml.erb'

      doc = Nokogiri::XML(response.body)
      passed_entry = atom_xpath(doc, "feed", "entry")[1]

      passed_entry.tap do |entry|
        expect(atom_xpath(entry, "title").text).to eq("pipeline-name(1) stage stage-name(stage-counter) Passed")

        atom_xpath(entry, "author")[0].tap do |author|
          expect(atom_xpath(author, "name").text).to eq("user<loser")
          expect(atom_xpath(author, "email").text).to eq("loser@gmail.com")
        end

        atom_xpath(entry, "author")[1].tap do |author|
          expect(atom_xpath(author, "name").text).to eq("user>boozer")
          expect(atom_xpath(author, "email").text).to eq("boozer@gmail.com")
        end
      end

      cancelled_entry = atom_xpath(doc, "feed", "entry")[0]
      cancelled_entry.tap do |entry|
        expect(atom_xpath(entry, "title").text).to eq("pipeline-name(2) stage stage-name(stage-counter) Cancelled")

        atom_xpath(entry, "author")[0].tap do |author|
          expect(atom_xpath(author, "name").text).to eq("user anonymous")
        end
      end

      expect(atom_xpath(doc, "feed", "email")).to be_nil_or_empty
    end

    it "should include link to the resource" do
      render :template => '/api/pipelines/stage_feed.xml.erb'

      doc = Nokogiri::XML(response.body)
      expect(atom_xpath(doc, "feed", "entry", "link[@rel='alternate'][@type='application/vnd.go+xml'][@href='http://test.host/api/feeds/stage.xml'][@title='stage-name Stage Detail']")).to_not be_nil_or_empty
    end

    it "should include link pipeline resource" do
      allow(view).to receive(:page_url).with(@entry1.getStageIdentifier().getStageLocator()).and_return("entry_1_url")
      allow(view).to receive(:page_url).with(@entry2.getStageIdentifier().getStageLocator()).and_return("entry_2_url")
      allow(view).to receive(:page_url).with(@entry1.getStageIdentifier().getStageLocator(), :action => "pipeline").and_return("pipeline_1_url")
      allow(view).to receive(:page_url).with(@entry2.getStageIdentifier().getStageLocator(), :action => "pipeline").and_return("pipeline_2_url")

      render :template => '/api/pipelines/stage_feed.xml.erb'

      doc = Nokogiri::XML(response.body)
      expect(atom_xpath(doc, "feed", "entry", "link[@rel='http://www.thoughtworks-studios.com/ns/relations/go/pipeline'][@type='application/vnd.go+xml'][@href='http://test.host/api/pipelines/pipeline-name/10.xml'][@title='pipeline-name Pipeline Detail']")).to_not be_nil_or_empty
      expect(atom_xpath(doc, "feed", "entry", "link[@rel='http://www.thoughtworks-studios.com/ns/relations/go/pipeline'][@type='text/html'][@href='pipeline_1_url'][@title='pipeline-name Pipeline Detail']")).to_not be_nil_or_empty
    end

    it "should contain the feed entry state as a content of the entry" do
      render :template => '/api/pipelines/stage_feed.xml.erb'

      doc = Nokogiri::XML(response.body)
      expect(atom_xpath(doc, "feed", "entry", "category[@term='passed']")).to_not be_nil_or_empty
      expect(atom_xpath(doc, "feed", "entry", "category[@term='cancelled']")).to_not be_nil_or_empty
    end

    it "should escape title for xml sanity" do
      assign(:title, "St<ages")

      render :template => '/api/pipelines/stage_feed.xml.erb'

      doc = Nokogiri::XML(response.body)
      expect(atom_xpath(doc, "feed", "title").text).to eq("St<ages")
    end

    it "should have updated as the date of the last updated entry" do
      render :template => '/api/pipelines/stage_feed.xml.erb'

      doc = Nokogiri::XML(response.body)
      expect(atom_xpath(doc, "feed", "updated").text).to eq("TIME IN ISO8601")
    end

    it "should show go:author entry when triggered manually" do
      render :template => '/api/pipelines/stage_feed.xml.erb'

      root = dom4j_root_for(response.body)
      expect(root.valueOf("//go:author/go:name/.")).to eq("loser>boozer")
    end
  end

  describe "feed has no items" do

    before(:each) do
      assign(:feed, double(:updated_date => "TIME IN ISO8601", :entries => [], :first => nil, :last => nil))
      assign(:title, "title")

      allow(view).to receive(:page_url).and_return("http://test.host/pipelines/pipeline-name/1/stage-name/1")
      allow(view).to receive(:url).and_return("http://test.host/api/feeds/stages.xml")
      allow(view).to receive(:resource_url).and_return("http://test.host/api/feeds/stage.xml")
    end

    it "should not include a next link" do
      render :template => '/api/pipelines/stage_feed.xml.erb'

      doc = Nokogiri::XML(response.body)
      expect(atom_xpath(doc, "feed", "link[@rel='next']")).to be_nil_or_empty
    end

    it "should not include a previous link" do
      render :template => '/api/pipelines/stage_feed.xml.erb'

      doc = Nokogiri::XML(response.body)
      expect(atom_xpath(doc, "feed", "link[@rel='previous']")).to be_nil_or_empty
    end
  end

  def atom_xpath document, *tags
    atom_tags = tags.map {|tag| "atom:#{tag}"}.join("/")
    document.xpath(atom_tags, 'atom' => 'http://www.w3.org/2005/Atom')
  end
end
