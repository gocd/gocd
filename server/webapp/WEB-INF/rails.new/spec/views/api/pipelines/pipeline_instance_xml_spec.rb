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

describe "/api/pipelines/pipeline_instance" do
  include GoUtil
  include StageModelMother

  before do
    view.class_eval do
      include MaterialsHelper
      include Api::FeedsHelper
    end

    @pipeline_config = PipelineMother.twoBuildPlansWithResourcesAndHgMaterialsAtUrl("uat", "default-stage", "http://foo:bar@baz.com:8000")
    set_pipeline(@pipeline_config, PipelineHistoryMother.pipelineHistory(@pipeline_config, @schedule_time = java.util.Date.new(), @modified_time = java.util.Date.new()).first)

    @pipeline2_config = PipelineMother.createPipelineConfig("downstream", MaterialConfigs.new([MaterialConfigsMother.dependencyMaterialConfig("uat", "default-stage")]), ["other-stage"].to_java(:string))
    @dependent_pipeline = PipelineHistoryMother.pipelineHistory(@pipeline2_config, @schedule_time2 = java.util.Date.new(), @modified_time2 = java.util.Date.new(), "uat/1/default-stage/1").first
    @dependent_pipeline.setId(12)
    @dependent_pipeline.setMaterialConfigs(@pipeline2_config.materialConfigs())

    allow(view).to receive(:stage_url_from_identifier).with("uat/1/default-stage/1").and_return("url_to_default_stage")

    @finder = stage_finder
    @context = XmlWriterContext.new("http://test.host/go", nil, nil, nil, @finder)
    assign(:doc, PipelineXmlViewModel.new(@pipeline).toXml(@context))
  end

  it "should contain details for a dependent pipeline" do
    assign(:doc, PipelineXmlViewModel.new(@dependent_pipeline).toXml(@context))

    render :template => '/api/pipelines/pipeline_instance.xml.erb'

    doc = Nokogiri::XML(response.body)
    doc.xpath("pipeline[@name='downstream']").tap do |pipeline|
      pipeline.xpath("materials").tap do |materials|
        materials.xpath("material[@type='DependencyMaterial'][@pipelineName='uat'][@stageName='default-stage'][@materialUri='http://test.host/go/api/materials/10.xml']").tap do |dep_material|
          dep_material.xpath("modifications").tap do |modifications|
            modifications.xpath("changeset[@changesetUri='http://test.host/go/api/stages/#{@finder.findStageWithIdentifier(nil).getId()}.xml']").tap do |changeset|
              expect(changeset.xpath("user")).to be_nil_or_empty
              expect(changeset.xpath("checkinTime").text).to eq(DateUtils.formatISO8601(@modified_time2))
              expect(changeset.xpath("revision").text).to eq("uat/1/default-stage/1")
              expect(changeset.xpath("message")).to be_nil_or_empty
            end
          end
        end
      end
    end
  end

  it "should contain stage details" do
    @pipeline.getStageHistory().get(0).setId(1)
    @pipeline_config.add(StageConfigMother.stageConfig("stage2"))
    @pipeline.getStageHistory().add(NullStageHistoryItem.new("stage2"))

    assign(:doc, PipelineXmlViewModel.new(@pipeline).toXml(@context))

    render :template => '/api/pipelines/pipeline_instance.xml.erb'

    doc = Nokogiri::XML(response.body)
    doc.xpath("pipeline[@name='uat'][@counter='1'][@label='1']").tap do |pipeline|
      expect(pipeline.xpath("scheduleTime").text).to eq(DateUtils.formatISO8601(@pipeline.getScheduledDate()))

      expect(pipeline.xpath("stages/stage[@href='http://test.host/go/api/stages/1.xml']")).to_not be_nil_or_empty
      expect(pipeline.xpath("stages/stage[@href='http://test.host/go/api/stages/0.xml']")).to be_nil_or_empty
      expect(pipeline.xpath("stage[@href='http://test.host/go/api/stages/0.xml']")).to be_nil_or_empty

      pipeline.xpath("materials").tap do |materials|
        materials.xpath("material[@type='HgMaterial'][@url='http://foo:******@baz.com:8000'][@materialUri='http://test.host/go/api/materials/10.xml']").tap do |dep_material|
          dep_material.xpath("modifications").tap do |modifications|
            modifications.xpath("changeset[@changesetUri='http://test.host/go/api/materials/10/changeset/#{PipelineHistoryMother::REVISION}.xml']").tap do |changeset|
              expect(changeset.xpath("user").text).to eq("user")
              expect(changeset.xpath("checkinTime").text).to eq(DateUtils.formatISO8601(@modified_time))
              expect(changeset.xpath("revision").text).to eq(PipelineHistoryMother::REVISION)
              expect(changeset.xpath("message").text).to eq("Comment")
              expect(changeset.xpath("file[@name='file'][@action='added']")).to_not be_nil_or_empty
            end
          end
        end
      end
    end
  end

  it "should escape invalid xml attributes" do
    @pipeline_config = PipelineMother.twoBuildPlansWithResourcesAndHgMaterialsAtUrl("<uat", "<default-stage", "file:///opt/foo<bar/baz", "foo<bar")
    set_pipeline(@pipeline_config, PipelineHistoryMother.pipelineHistory(@pipeline_config, @schedule_time = java.util.Date.new(), @modified_time = java.util.Date.new(), "< junk",
                                                                         "user<less", "fixed < tag", "foo<@bar.com", "file <\" name", "dir < with angle", "pipeline<label").first)

    assign(:doc, PipelineXmlViewModel.new(@pipeline).toXml(@context))

    allow(view).to receive(:api_pipeline_instance_url) do |options|
      "url-#{options.map {|key, value| "#{key}=#{value}"}.join(",")}".gsub(/>/, '&gt;').gsub(/</, '&lt;')
    end

    render :template => '/api/pipelines/pipeline_instance.xml.erb'

    root = Nokogiri::XML(response.body)
    expect(root.xpath("//pipeline/@name").text).to eq("<uat")
    expect(root.xpath("//pipeline/@label").text).to eq("pipeline<label")
    expect(root.xpath("//file/@name").text).to eq("file <\" name")
    expect(root.xpath("//revision/.").text).to eq("< junk")
    expect(root.xpath("//material/@url").text).to eq("file:///opt/foo<bar/baz")
    expect(root.xpath("//user/.").text).to eq("user<less")
    expect(root.xpath("//message/.").text).to eq("fixed < tag")
  end

  it "should escape invalid xml attributes for dependent materials as well" do
    @dependent_pipeline = PipelineHistoryMother.pipelineHistory(@pipeline2_config, @schedule_time2 = java.util.Date.new(), @modified_time2 = java.util.Date.new(), "ua<t/1/def\"ault<-stage/1").first
    @dependent_pipeline.setId(12)
    @dependent_pipeline.setMaterialConfigs(@pipeline2_config.materialConfigs())

    assign(:doc, PipelineXmlViewModel.new(@dependent_pipeline).toXml(@context))

    allow(view).to receive(:stage_url_from_identifier).with("ua<t/1/def\"ault<-stage/1").and_return("a_url")

    render :template => '/api/pipelines/pipeline_instance.xml.erb'

    root = Nokogiri::XML(response.body)
    expect(root.xpath("//revision/.").text).to eq("ua<t/1/def\"ault<-stage/1")
  end

  it "should wrap fields with externaly fed data into ctags" do
    render :template => '/api/pipelines/pipeline_instance.xml.erb'
    expect(response.body =~ cdata_wraped_regexp_for("user")).to be_true
    expect(response.body =~ cdata_wraped_regexp_for("Comment")).to be_true
    expect(response.body =~ cdata_wraped_regexp_for("changes")).to be_true
  end

  it "should have a self referencing link" do
    render :template => '/api/pipelines/pipeline_instance.xml.erb'

    root = Nokogiri::XML(response.body)
    expect(root.xpath("pipeline/link[@rel='self'][@href='http://test.host/go/api/pipelines/uat/10.xml']")).to_not be_nil_or_empty
    expect(root.xpath("pipeline/id").text).to eq("urn:x-go.studios.thoughtworks.com:job-id:uat:1")
  end

  it "should not add before if insertedbefore is null" do
    @pipeline.setPipelineBefore(PipelineTimelineEntry.new("uat", 9, 1, nil))
    @pipeline.setPipelineAfter(PipelineTimelineEntry.new("uat", 11, 3, nil))
    assign(:doc, PipelineXmlViewModel.new(@pipeline).toXml(@context))

    render :template => '/api/pipelines/pipeline_instance.xml.erb'

    root = Nokogiri::XML(response.body)
    expect(root.xpath("pipeline/link[@rel='insertedBefore'][@href='http://test.host/go/api/pipelines/uat/11.xml']")).to_not be_nil_or_empty
    expect(root.xpath("pipeline/link[@rel='insertedAfter'][@href='http://test.host/go/api/pipelines/uat/9.xml']")).to_not be_nil_or_empty
    expect(root.xpath("pipeline/link[@rel='self'][@href='http://test.host/go/api/pipelines/uat/10.xml']")).to_not be_nil_or_empty
  end

  it "should not add before if insertedBefore is null" do
    @pipeline.setPipelineBefore(PipelineTimelineEntry.new("uat", 9, 1, nil))
    assign(:doc, PipelineXmlViewModel.new(@pipeline).toXml(@context))

    render :template => '/api/pipelines/pipeline_instance.xml.erb'

    root = Nokogiri::XML(response.body)
    expect(root.xpath("pipeline/link[@rel='insertedAfter'][@href='http://test.host/go/api/pipelines/uat/9.xml']")).to_not be_nil_or_empty
    expect(root.xpath("pipeline/link[@rel='insertedBefore'][@href='http://test.host/go/api/pipelines/uat/11.xml']")).to be_nil_or_empty
  end

  it "should not add before if insertedAfter is null" do
    @pipeline.setPipelineAfter(PipelineTimelineEntry.new("uat", 11, 3, nil))
    assign(:doc, PipelineXmlViewModel.new(@pipeline).toXml(@context))

    render :template => '/api/pipelines/pipeline_instance.xml.erb'

    root = Nokogiri::XML(response.body)
    expect(root.xpath("pipeline/link[@rel='insertedBefore'][@href='http://test.host/go/api/pipelines/uat/11.xml']")).to_not be_nil_or_empty
    expect(root.xpath("pipeline/link[@rel='insertedAfter'][@href='http://test.host/go/api/pipelines/uat/9.xml']")).to be_nil_or_empty
  end

  def set_pipeline(pipeline_config, pipeline)
    @pipeline = pipeline
    @pipeline.getPipelineIdentifier
    @pipeline.setId(10)
    @pipeline.setMaterialConfigs(pipeline_config.materialConfigs())
  end

  def stage_finder
    Class.new() do
      include com.thoughtworks.go.domain.StageFinder
      include StageModelMother

      def initialize
        @stage = stage(1)
      end

      def findStageWithIdentifier(id)
        @stage
      end
    end.new
  end
end
