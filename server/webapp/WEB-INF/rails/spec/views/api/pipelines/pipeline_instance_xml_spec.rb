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

describe "/api/pipelines/pipeline_instance" do

  include GoUtil
  include StageModelMother

  before do
    template.class_eval do
      include MaterialsHelper
      include Api::FeedsHelper
    end

    @pipeline_config = PipelineMother.twoBuildPlansWithResourcesAndHgMaterialsAtUrl("uat", "default-stage", "http://foo:bar@baz.com:8000")
    set_pipeline(@pipeline_config, PipelineHistoryMother.pipelineHistory(@pipeline_config, @schedule_time = java.util.Date.new(), @modified_time = java.util.Date.new()).first)

    @pipeline2_config = PipelineMother.createPipelineConfig("downstream", MaterialConfigs.new([MaterialConfigsMother.dependencyMaterialConfig("uat", "default-stage")]), ["other-stage"].to_java(:string))
    @dependent_pipeline = PipelineHistoryMother.pipelineHistory(@pipeline2_config, @schedule_time2 = java.util.Date.new(), @modified_time2 = java.util.Date.new(), "uat/1/default-stage/1").first
    @dependent_pipeline.setId(12)
    @dependent_pipeline.setMaterialConfigs(@pipeline2_config.materialConfigs())

    template.stub(:stage_url_from_identifier).with("uat/1/default-stage/1").and_return("url_to_default_stage")
    @finder = stage_finder
    @context = XmlWriterContext.new("http://test.host/go", nil, nil, nil, @finder)
    assigns[:doc] = PipelineXmlViewModel.new(@pipeline).toXml(@context)
  end

  it "should contain details for a dependent pipeline" do
    assigns[:doc] = PipelineXmlViewModel.new(@dependent_pipeline).toXml(@context)

    render '/api/pipelines/pipeline_instance.xml'

    response.body.should have_tag("pipeline[name='downstream']") do
      with_tag "materials" do
        with_tag "material[type='DependencyMaterial'][pipelineName='uat'][stageName='default-stage'][materialUri=?]", "http://test.host/go/api/materials/10.xml" do
          with_tag "modifications" do
            with_tag "changeset[changesetUri=?]", "http://test.host/go/api/stages/#{@finder.findStageWithIdentifier(nil).getId()}.xml" do
              without_tag "user"
              with_tag "checkinTime", @modified_time2.iso8601
              with_tag "revision", "uat/1/default-stage/1"
              without_tag "message"
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

    assigns[:doc] = PipelineXmlViewModel.new(@pipeline).toXml(@context)

    render '/api/pipelines/pipeline_instance.xml'

    response.body.should have_tag("pipeline[name='uat'][counter='1'][label='1']") do
      with_tag "scheduleTime", @schedule_time.iso8601

      with_tag "materials" do
        with_tag "material[type='HgMaterial'][url='http://foo:******@baz.com:8000'][materialUri=?]", "http://test.host/go/api/materials/10.xml" do
          with_tag "modifications" do
            with_tag "changeset[changesetUri=?]", "http://test.host/go/api/materials/10/changeset/#{PipelineHistoryMother::REVISION}.xml" do
              with_tag "user", "user"
              with_tag "checkinTime", @modified_time.iso8601
              with_tag "revision", PipelineHistoryMother::REVISION
              with_tag "message", "Comment"
              with_tag "file[name='file'][action='added']"
            end
          end
        end
      end

      with_tag "approvedBy", "changes"
    end

    response.body.should have_tag("pipeline[name='uat'][counter='1'][label='1']") do |pipeline|
      pipeline.should have_tag("stages stage[href='http://test.host/go/api/stages/1.xml']")
      pipeline.should_not have_tag("stage[href='http://test.host/go/api/stages/0.xml']")
    end
  end

  it "should escape invalid xml attributes" do
    @pipeline_config = PipelineMother.twoBuildPlansWithResourcesAndHgMaterialsAtUrl("<uat", "<default-stage", "file:///opt/foo<bar/baz", "foo<bar")
    set_pipeline(@pipeline_config, PipelineHistoryMother.pipelineHistory(@pipeline_config, @schedule_time = java.util.Date.new(), @modified_time = java.util.Date.new(), "< junk",
                                                                         "user<less", "fixed < tag", "foo<@bar.com", "file <\" name", "dir < with angle", "pipeline<label").first)

    assigns[:doc] = PipelineXmlViewModel.new(@pipeline).toXml(@context)

    template.stub(:api_pipeline_instance_url) do |options|
      "url-#{options.map {|key, value| "#{key}=#{value}"}.join(",")}".gsub(/>/, '&gt;').gsub(/</, '&lt;')
    end

    render '/api/pipelines/pipeline_instance.xml'

    root = dom4j_root_for(response.body)
    root.valueOf("//pipeline/@name").should == "<uat"
    root.valueOf("//pipeline/@label").should == "pipeline<label"
    root.valueOf("//file/@name").should == "file <\" name"
    root.valueOf("//revision/.").should == "< junk"
    root.valueOf("//material/@url").should == "file:///opt/foo<bar/baz"
    root.valueOf("//user/.").should == "user<less"
    root.valueOf("//message/.").should == "fixed < tag"
  end

  it "should escape invalid xml attributes for dependent materials as well" do
    @dependent_pipeline = PipelineHistoryMother.pipelineHistory(@pipeline2_config, @schedule_time2 = java.util.Date.new(), @modified_time2 = java.util.Date.new(), "ua<t/1/def\"ault<-stage/1").first
    @dependent_pipeline.setId(12)
    @dependent_pipeline.setMaterialConfigs(@pipeline2_config.materialConfigs())

    assigns[:doc] = PipelineXmlViewModel.new(@dependent_pipeline).toXml(@context)


    template.stub(:stage_url_from_identifier).with("ua<t/1/def\"ault<-stage/1").and_return("a_url")

    render '/api/pipelines/pipeline_instance.xml'

    root = dom4j_root_for(response.body)
    root.valueOf("//revision/.").should == "ua<t/1/def\"ault<-stage/1"
  end

  it "should wrap fields with externaly fed data into ctags" do
    render '/api/pipelines/pipeline_instance.xml'
    response.body.should =~ cdata_wraped_regexp_for("user")
    response.body.should =~ cdata_wraped_regexp_for("Comment")
    response.body.should =~ cdata_wraped_regexp_for("changes")
  end

  it "should have a self referencing link" do
    render '/api/pipelines/pipeline_instance.xml'
    response.body.should have_tag "link[rel='self'][href='http://test.host/go/api/pipelines/uat/10.xml']"
    response.body.should have_tag "id", "urn:x-go.studios.thoughtworks.com:job-id:uat:1"
  end

  it "should not add before if insertedbefore is null" do
    @pipeline.setPipelineBefore(PipelineTimelineEntry.new("uat", 9, 1, nil))
    @pipeline.setPipelineAfter(PipelineTimelineEntry.new("uat", 11, 3, nil))
    assigns[:doc] = PipelineXmlViewModel.new(@pipeline).toXml(@context)

    render '/api/pipelines/pipeline_instance.xml'

    response.body.should have_tag "link[rel='insertedBefore'][href='http://test.host/go/api/pipelines/uat/11.xml']"
    response.body.should have_tag "link[rel='insertedAfter'][href='http://test.host/go/api/pipelines/uat/9.xml']"
    response.body.should have_tag "link[rel='self'][href='http://test.host/go/api/pipelines/uat/10.xml']"
  end

  it "should not add before if insertedBefore is null" do
    @pipeline.setPipelineBefore(PipelineTimelineEntry.new("uat", 9, 1, nil))
    assigns[:doc] = PipelineXmlViewModel.new(@pipeline).toXml(@context)

    render '/api/pipelines/pipeline_instance.xml'
    response.body.should have_tag "link[rel='insertedAfter'][href='http://test.host/go/api/pipelines/uat/9.xml']"
    response.body.should_not have_tag "link[rel='insertedBefore'][href='http://test.host/go/api/pipelines/uat/11.xml']"
  end

  it "should not add before if insertedAfter is null" do
    @pipeline.setPipelineAfter(PipelineTimelineEntry.new("uat", 11, 3, nil))
    assigns[:doc] = PipelineXmlViewModel.new(@pipeline).toXml(@context)

    render '/api/pipelines/pipeline_instance.xml'
    response.body.should have_tag "link[rel='insertedBefore'][href='http://test.host/go/api/pipelines/uat/11.xml']"
    response.body.should_not have_tag "link[rel='insertedAfter'][href='http://test.host/go/api/pipelines/uat/9.xml']"
  end

  #TODO: do material helper test
end
