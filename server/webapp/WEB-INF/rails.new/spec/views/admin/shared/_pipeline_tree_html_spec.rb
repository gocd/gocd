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
load File.join(File.dirname(__FILE__), 'stage_parent_tree_examples.rb')

describe "template_tree" do
  include TaskMother
  include FormUI
  include GoUtil

  before(:each) do
    @stage_parent = "templates"
    @stage_parent_edit_path = template_edit_path(:pipeline_name => "pipeline", :stage_parent => "templates", :current_tab => 'general')
    @pipeline = PipelineTemplateConfig.new(CaseInsensitiveString.new("pipeline"), [stage_with_a_job("stage1"), stage_with_a_job("stage2"), stage_with_a_job("stage3")].to_java(com.thoughtworks.go.config.StageConfig));
  end

  def stage_with_a_job stage_name
    StageConfigMother.custom(stage_name, ["dev"].to_java(java.lang.String))
  end

  it_should_behave_like :stage_parent_tree
end


describe "pipeline_tree" do
  include TaskMother
  include FormUI
  include GoUtil

  before(:each) do
    @stage_parent = "pipelines"
    @stage_parent_edit_path = pipeline_edit_path(:pipeline_name => "pipeline", :current_tab => "general")
    @pipeline = PipelineConfigMother.createPipelineConfigWithStages("pipeline", ["stage1", "stage2", "stage3"].to_java(java.lang.String))
    @security_service = double("security_service")
    view.stub(:security_service).and_return(@security_service)
  end

  it "should show link for template if the pipeline uses template and the user is admin" do
    pipeline_with_template = PipelineConfigMother.pipelineConfigWithTemplate("pipeline_with_temp", "new-template")
    in_params(:stage_parent => @stage_parent)
    in_params(:current_tab=>"general")
    assign(:user, Username.new(CaseInsensitiveString.new("admin")))
    view.stub(:is_user_an_admin?).and_return(true)

    render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => pipeline_with_template, :stage_parent => @stage_parent}}

    Capybara.string(response.body).find('ul.pipeline').tap do |ul_1|
      expect(ul_1).to have_selector("ul.template li", :text => "new-template")
      expect(ul_1).to have_selector("ul.template li a[href='#{template_edit_path(:pipeline_name => "new-template", :stage_parent => "templates", :current_tab => 'general')}']")
    end
  end

  it "should show link for template if the pipeline uses template and the user is template admin" do
    pipeline_with_template = PipelineConfigMother.pipelineConfigWithTemplate("pipeline_with_temp", "new-template")
    in_params(:stage_parent => @stage_parent)
    in_params(:current_tab=>"general")
    template_admin = Username.new(CaseInsensitiveString.new("template-admin"))
    assign(:user, template_admin)
    view.stub(:is_user_an_admin?).and_return(false)
    view.stub(:current_user).and_return(template_admin)
    @security_service.should_receive(:isAuthorizedToEditTemplate).with("new-template", template_admin).and_return(true)

    render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => pipeline_with_template, :stage_parent => @stage_parent}}

    Capybara.string(response.body).find('ul.pipeline').tap do |ul_1|
      expect(ul_1).to have_selector("ul.template li", :text => "new-template")
      expect(ul_1).to have_selector("ul.template li a[href='#{template_edit_path(:pipeline_name => "new-template", :stage_parent => "templates", :current_tab => 'general')}']")
    end
  end

  it "should show only template name if the pipeline uses template and if the user is not an admin or template admin" do
    pipeline_with_template = PipelineConfigMother.pipelineConfigWithTemplate("pipeline_with_temp", "new-template")
    in_params(:stage_parent => @stage_parent)
    in_params(:current_tab=>"general")
    assign(:user, Username.new(CaseInsensitiveString.new("admin")))
    view.stub(:is_user_an_admin?).and_return(false)
    view.stub(:is_user_a_template_admin_for_template?).and_return(false)

    render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => pipeline_with_template, :stage_parent => @stage_parent}}

    Capybara.string(response.body).find('ul.pipeline').tap do |ul_1|
      expect(ul_1).to have_selector("ul.template li", :text => "new-template")
      expect(ul_1).not_to have_selector("ul.template li a[href='#{template_edit_path(:pipeline_name => "new-template", :stage_parent => "templates", :current_tab => 'general')}']")
    end
  end

  it "should show template preview link if the pipeline uses template and if the user is not an admin adn template admin" do
    pipeline_with_template = PipelineConfigMother.pipelineConfigWithTemplate("pipeline_with_temp", "new-template")
    in_params(:stage_parent => @stage_parent)
    in_params(:current_tab=>"general")
    assign(:user, Username.new(CaseInsensitiveString.new("user")))
    view.stub(:is_user_an_admin?).and_return(false)
    view.stub(:is_user_a_template_admin_for_template?).and_return(false)

    render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => pipeline_with_template, :stage_parent => @stage_parent}}

    Capybara.string(response.body).find('ul.pipeline').tap do |ul_1|
      ul_1.find("ul.template li", "new-template") do |li_1|
        expect(li_1).to have_selector("a.preview_icon_wobg.action_icon.skip_dirty_stop.view_template_tree_link[href='new-template']")
      end

      expect(ul_1).not_to have_selector("ul.template li a[href='#{template_edit_path(:pipeline_name => "new-template", :stage_parent => "templates", :current_tab => 'general')}']")
    end
  end

  it "should show template preview link if the pipeline uses template and the user is admin" do
    pipeline_with_template = PipelineConfigMother.pipelineConfigWithTemplate("pipeline_with_temp", "new-template")
    in_params(:stage_parent => @stage_parent)
    in_params(:current_tab=>"general")
    assign(:user, Username.new(CaseInsensitiveString.new("admin")))
    view.stub(:is_user_an_admin?).and_return(true)

    render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => pipeline_with_template, :stage_parent => @stage_parent}}

    Capybara.string(response.body).find('ul.pipeline').tap do |ul_1|
      ul_1.find("ul.template li", "new-template") do |li_1|
        expect(li_1).to have_selector("a[href='#{template_edit_path(:pipeline_name => "new-template", :stage_parent => "templates", :current_tab => 'general')}']")
        expect(li_1).to have_selector("a.preview_icon_wobg.action_icon.skip_dirty_stop.view_template_tree_link[href='new-template']")
      end
    end
  end

  it "should show template preview link if the pipeline uses template and the user is  template admin" do
    pipeline_with_template = PipelineConfigMother.pipelineConfigWithTemplate("pipeline_with_temp", "new-template")
    in_params(:stage_parent => @stage_parent)
    in_params(:current_tab=>"general")
    template_admin = Username.new(CaseInsensitiveString.new("template-admin"))
    assign(:user, template_admin)
    view.stub(:is_user_an_admin?).and_return(false)
    view.stub(:current_user).and_return(template_admin)
    @security_service.should_receive(:isAuthorizedToEditTemplate).with("new-template", template_admin).and_return(true)

    render :partial => "admin/shared/pipeline_tree.html", :locals=> {:scope=> {:pipeline => pipeline_with_template, :stage_parent => @stage_parent}}

    Capybara.string(response.body).find('ul.pipeline').tap do |ul_1|
      ul_1.find("ul.template li", "new-template") do |li_1|
        expect(li_1).to have_selector("a[href='#{template_edit_path(:pipeline_name => "new-template", :stage_parent => "templates", :current_tab => 'general')}']")
        expect(li_1).to have_selector("a.preview_icon_wobg.action_icon.skip_dirty_stop.view_template_tree_link[href='new-template']")
      end
    end
  end


  it_should_behave_like :stage_parent_tree
end
