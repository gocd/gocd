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

describe "admin/pipeline_groups/index.html.erb" do

  include ReflectiveUtil

  before(:each) do
    assign(:groups, groups("group_foo", "group_bar", "group_quux"))
    assign(:user, Username.new(CaseInsensitiveString.new("loser")))
    view.stub(:tab_with_display_name).and_return("tab_link")
    view.stub(:mycruise_available?).and_return(false)
    view.stub(:can_view_admin_page?).and_return(true)
    assign(:cruise_config, cruise_config = BasicCruiseConfig.new)
    set(cruise_config, "md5", "abcd1234")
    assign(:pipeline_to_can_delete, {CaseInsensitiveString.new("pipeline_in_group_foo") => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                                        CaseInsensitiveString.new("pipeline_2_in_group_foo") => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                                        CaseInsensitiveString.new("pipeline_with_template_in_group_foo") => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                                        CaseInsensitiveString.new("pipeline_in_group_bar") => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                                        CaseInsensitiveString.new("pipeline_2_in_group_bar") => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                                        CaseInsensitiveString.new("pipeline_with_template_in_group_bar") => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                                        CaseInsensitiveString.new("pipeline_in_group_quux") => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                                        CaseInsensitiveString.new("pipeline_2_in_group_quux") => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                                        CaseInsensitiveString.new("pipeline_with_template_in_group_quux") => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE"))
    })
    view.stub(:is_user_an_admin?).and_return(true)
  end

  def groups(*named)
    named.map do |name|
      pipeline_one = PipelineConfigMother.pipelineConfig("pipeline_in_#{name}")
      pipeline_two = PipelineConfigMother.pipelineConfig("pipeline_2_in_#{name}")
      pipeline_three = PipelineConfigMother.pipelineConfigWithTemplate("pipeline_with_template_in_#{name}", "some_template")
      BasicPipelineConfigs.new(name, Authorization.new, [pipeline_one, pipeline_two, pipeline_three].to_java(PipelineConfig))
    end
  end

  it "should set tab and page title" do
    render

    view.instance_variable_get('@tab_name').should == "pipeline-groups"
  end

  it "should display a message if the pipeline group is empty" do
    assign(:groups, [BasicPipelineConfigs.new("group", Authorization.new, [].to_java(PipelineConfig))])
    assign(:pipeline_to_can_delete, {})

    render

    Capybara.string(response.body).find('div.group_pipelines').tap do |div|
      div.find("div.group") do |group|
        expect(group).to have_selector("h2.group_name", :text => "group")
        expect(group).to have_selector("div.information", :text => "No pipelines associated with this group")

        expect(group).not_to have_selector("table")
      end
    end
  end


  describe "create new group" do
    it "should remove the add new group for anyone other than super admin" do
      view.stub(:is_user_an_admin?).and_return(false)

      render

      Capybara.string(response.body).find('.group_pipelines').tap do |div|
        expect(div).not_to have_selector("a[href='#'][class='add_link']", :text => "Add New Pipeline Group")
      end
    end
  end

  it "should display all pipelines with delete link" do
    render

    Capybara.string(response.body).find('div.group_pipelines').tap do |div|
      div.all("div.group") do |groups|
        expect(groups[0]).to have_selector("h2.group_name", :text => "group_foo")
        expect(groups[0]).to have_selector("a[href='#{pipeline_group_edit_path(:group_name => "group_foo")}']")
        groups[0].find("table") do |table|
          table.find("thead tr.pipeline") do |tr|
            expect(tr).to have_selector("th.name", :text => "Pipeline")
            expect(tr).to have_selector("th.actions", :text => "Actions")
          end
          table.find("tbody") do |tbody|
            tbody.find("tr.pipeline") do |tr|
              expect(tr).to have_selector("td.name a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_in_group_foo", :current_tab => "general")}']", "pipeline_in_group_foo")
              tr.find("td.actions") do |td|
                td.find("ul") do |ul|
                  expect(ul).to have_selector("li span.delete_parent")
                end
              end
            end

            table.find("tr.pipeline") do |tr|
              expect(tr).to have_selector("td.name a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_2_in_group_foo", :current_tab => "general")}']", "pipeline_2_in_group_foo")
              tr.find("td.actions") do |td|
                td.find("ul") do |ul|
                  expect(ul).to have_selector("li span.delete_parent")
                end
              end
            end
          end
        end
      end
    end
  end

  describe "delete pipeline" do
    it "should display delete link next to an empty pipeline group" do
      assign(:groups, [BasicPipelineConfigs.new("empty_group", Authorization.new, [].to_java(PipelineConfig))])
      render

      Capybara.string(response.body).find('div.group_pipelines').tap do |div|
        div.find("div.group") do |group|
          expect(group).to have_selector("h2.group_name", :text => "empty_group")
          group.find("form[id='delete_group_empty_group'][method='post'][action='#{pipeline_group_delete_path(:group_name => 'empty_group')}'][title='Delete this pipeline group']") do |form|
            expect(form).to have_selector("input[name='_method'][value='delete']")
            expect(form).to have_selector("span#trigger_delete_group_empty_group")
            expect(form).to have_selector("script[type='text/javascript']", /Util.escapeDotsFromId\('trigger_delete_group_empty_group #warning_prompt'\)/)
            expect(form).to have_selector("div#warning_prompt[style='display:none;']", /Are you sure you want to delete the pipeline group 'empty_group' \?/)
          end
        end
      end
    end

    it "should not display delete link if user is group admin" do
      assign(:groups, [BasicPipelineConfigs.new("empty_group", Authorization.new, [].to_java(PipelineConfig))])
      view.stub(:is_user_an_admin?).and_return(false)

      render

      expect(response.body).not_to have_selector("form#delete_group_empty_group")
      expect(response.body).not_to have_selector("span.icon_cannot_remove")
    end

    it "should display disabled delete link next to a non-empty pipeline group" do
      render

      Capybara.string(response.body).find('div.group_pipelines').tap do |div|
        div.all("div.group") do |groups|
          expect(groups[0]).to have_selector("h2.group_name", :text => "group_foo")
          expect(groups[0]).to have_selector("span.delete_icon_disabled[title='Move or Delete all pipelines within this group in order to delete it.']")

          expect(groups[0]).not_to have_selector("form[id='delete_group_group_foo'][method='post'][action='#{pipeline_group_delete_path(:group_name => 'group_foo')}']")
        end
      end
    end

    it "should have unique random id for delete pipeline link" do
      view.stub(:random_dom_id).and_return("some_random_id")

      render

      Capybara.string(response.body).find("form[action='/admin/pipelines/pipeline_in_group_foo'][id='some_random_id_form']").tap do |form|
        expect(form).to have_selector("span#trigger_some_random_id")
      end
      Capybara.string(response.body).find("form[action='/admin/pipelines/pipeline_2_in_group_foo'][id='some_random_id_form']").tap do |form|
        expect(form).to have_selector("span#trigger_some_random_id")
      end
      Capybara.string(response.body).find("form[action='/admin/pipelines/pipeline_in_group_bar'][id='some_random_id_form']").tap do |form|
        expect(form).to have_selector("span#trigger_some_random_id")
      end
      Capybara.string(response.body).find("form[action='/admin/pipelines/pipeline_2_in_group_bar'][id='some_random_id_form']").tap do |form|
        expect(form).to have_selector("span#trigger_some_random_id")
      end
    end

    it "should display all pipelines with delete link" do
      render

      Capybara.string(response.body).find('div.group_pipelines').tap do |div|
        div.all("div.group") do |groups|
          expect(groups[0]).to have_selector("h2.group_name", "group_foo")
          expect(groups[0]).to have_selector("a[href='#{pipeline_group_edit_path(:group_name => "group_foo")}']")
          groups[0].find("table") do |table|
            table.find("thead tr.pipeline") do |tr|
              expect(tr).to have_selector("th.name", :text => "Pipeline")
              expect(tr).to have_selector("th.actions", :text => "Actions")
            end
            table.find("tbody") do |tbody|
              tbody.find("tr.pipeline") do |tr|
                expect(tr).to have_selector("td.name a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_in_group_foo", :current_tab => "general")}']", "pipeline_in_group_foo")
                tr.find("td.actions") do |td|
                  td.find("ul") do |ul|
                    expect(ul).to have_selector("li span.delete_parent")
                  end
                end
              end

              tbody.find("tr.pipeline") do |tr|
                expect(tr).to have_selector("td.name a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_2_in_group_foo", :current_tab => "general")}']", "pipeline_2_in_group_foo")
                tr.find("td.actions") do |td|
                  td.find("ul") do |ul|
                    expect(ul).to have_selector("li span.delete_parent")
                  end
                end
              end
            end
          end
        end

        expect(div).to have_selector("td.name a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_in_group_bar", :current_tab => "general")}']", "pipeline_in_group_bar")
        expect(div).to have_selector("td.name a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_2_in_group_bar", :current_tab => "general")}']", "pipeline_2_in_group_bar")
      end
    end

    it "should wire delete action" do
      render

      Capybara.string(response.body).find("form[action='/admin/pipelines/pipeline_in_group_foo'][method='post']").tap do |form|
        expect(form).to have_selector("span.delete_parent")
        expect(form).to have_selector("input[name='_method'][value='delete']")
      end
      expect(response.body).to have_selector("form[action='/admin/pipelines/pipeline_2_in_group_foo'] span.delete_parent")

      expect(response.body).to have_selector("form[action='/admin/pipelines/pipeline_in_group_bar'] span.delete_parent")
      expect(response.body).to have_selector("form[action='/admin/pipelines/pipeline_2_in_group_bar'] span.delete_parent")
    end

    it "should not show delete action if the pipeline cannot be deleted" do
      assign(:pipeline_to_can_delete, {CaseInsensitiveString.new("pipeline_in_group_foo") => CanDeleteResult.new(false, LocalizedMessage.string("CANNOT_DELETE_PIPELINE_IN_ENVIRONMENT", ["pipeline_in_group_foo", "env"])),
                                          CaseInsensitiveString.new("pipeline_2_in_group_foo") => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                                          CaseInsensitiveString.new("pipeline_with_template_in_group_foo") => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                                          CaseInsensitiveString.new("pipeline_in_group_bar") => CanDeleteResult.new(false, LocalizedMessage.string("CANNOT_DELETE_PIPELINE_USED_AS_MATERIALS", ["pipeline_in_group_bar", "pipeline_in_group_foo"])),
                                          CaseInsensitiveString.new("pipeline_2_in_group_bar") => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                                          CaseInsensitiveString.new("pipeline_with_template_in_group_bar") => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                                          CaseInsensitiveString.new("pipeline_in_group_quux") => CanDeleteResult.new(false, LocalizedMessage.string("CANNOT_DELETE_PIPELINE_USED_AS_MATERIALS", ["pipeline_in_group_bar", "pipeline_in_group_foo"])),
                                          CaseInsensitiveString.new("pipeline_2_in_group_quux") => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE")),
                                          CaseInsensitiveString.new("pipeline_with_template_in_group_quux") => CanDeleteResult.new(true, LocalizedMessage.string("CAN_DELETE_PIPELINE"))
      })

      render

      Capybara.string(response.body).find("form[action='/admin/pipelines/pipeline_in_group_foo'][method='post']").tap do |form|
        expect(form.find("span.delete_icon_disabled")['title']).to eq("Cannot delete pipeline 'pipeline_in_group_foo' as it is present in environment 'env'")
      end

      Capybara.string(response.body).find("form[action='/admin/pipelines/pipeline_in_group_bar'][method='post']").tap do |form|
        expect(form.find("span.delete_icon_disabled")['title']).to eq("Cannot delete pipeline 'pipeline_in_group_bar' as pipeline 'pipeline_in_group_foo' depends on it")
      end

      Capybara.string(response.body).find("form[action='/admin/pipelines/pipeline_2_in_group_foo'][method='post']").tap do |form|
        expect(form.find("span.delete_icon")['title']).to eq("Delete this pipeline")
      end

      Capybara.string(response.body).find("form[action='/admin/pipelines/pipeline_2_in_group_bar'][method='post']").tap do |form|
        expect(form.find("span.delete_icon")['title']).to eq("Delete this pipeline")
      end
    end
  end

  it "should render move control only when more than one pipeline-groups are present" do
    assign(:groups, groups("group_foo"))

    render

    expect(response.body).not_to have_selector(".hidden#move_pipeline_from_group_group_foo_pipeline_in_group_foo")

    expect(response.body).not_to have_selector"li form[action='#{move_pipeline_to_group_path(:pipeline_name => 'pipeline_in_group_foo')}'][method='post']"
  end

  describe "move pipeline" do
    it "should render move control only when more than one pipeline-groups are present" do
      assign(:groups, groups("group_foo"))

      render

      expect(response.body).not_to have_selector(".hidden#move_pipeline_from_group_group_foo_pipeline_in_group_foo")

      expect(response.body).not_to have_selector "li form[action='#{move_pipeline_to_group_path(:pipeline_name => 'pipeline_in_group_foo')}'][method='post']"
    end
  end

  describe "new pipeline wizard links" do

    it "should display new-pipeline-links which take you to the new wizard" do
      assign(:groups, groups("group_foo"))

      render

      Capybara.string(response.body).find('div.group_pipelines').tap do |div|
        expect(div).to have_selector("a.add_link.add_pipeline_to_group[href='#{pipeline_new_path(:group => "group_foo")}']")
      end
    end

    it "should display link when no pipelines exist for a group" do
      assign(:groups, [BasicPipelineConfigs.new("some-group", Authorization.new, [].to_java(PipelineConfig))])

      render

      Capybara.string(response.body).find('div.add_first_pipeline_in_group').tap do |div|
        expect(div).to have_selector("a.add_link.add_pipeline_to_group[href='#{pipeline_new_path(:group => "some-group")}']")
      end
    end

    it "should display link to add first pipeline" do
      assign(:groups, [])

      render

      Capybara.string(response.body).find('span.title_secondary_info').tap do |span|
        expect(span).to have_selector("a.add_link.add_pipeline_to_group[href='#{pipeline_new_path(:group => "defaultGroup")}']")
      end
    end

  end

  describe "extract template" do
    it "should have extract template button for each pipeline" do
      view.stub(:is_user_an_admin?).and_return(true)

      render

      Capybara.string(response.body).all("li a[href='#'][class='add_link']").tap do |lis|
        expect(lis[0]['onclick']).to eq("Util.ajax_modal('/admin/templates/new?pipelineToExtractFrom=pipeline_in_group_foo', {overlayClose: false, title: 'Extract Template'}, function(text){return text})")
        expect(lis[1]['onclick']).to eq("Util.ajax_modal('/admin/templates/new?pipelineToExtractFrom=pipeline_2_in_group_foo', {overlayClose: false, title: 'Extract Template'}, function(text){return text})")
        expect(lis[2]['onclick']).to eq("Util.ajax_modal('/admin/templates/new?pipelineToExtractFrom=pipeline_in_group_bar', {overlayClose: false, title: 'Extract Template'}, function(text){return text})")
        expect(lis[3]['onclick']).to eq("Util.ajax_modal('/admin/templates/new?pipelineToExtractFrom=pipeline_2_in_group_bar', {overlayClose: false, title: 'Extract Template'}, function(text){return text})")
        expect(lis[4]['onclick']).to eq("Util.ajax_modal('/admin/templates/new?pipelineToExtractFrom=pipeline_in_group_quux', {overlayClose: false, title: 'Extract Template'}, function(text){return text})")
        expect(lis[5]['onclick']).to eq("Util.ajax_modal('/admin/templates/new?pipelineToExtractFrom=pipeline_2_in_group_quux', {overlayClose: false, title: 'Extract Template'}, function(text){return text})")
      end
    end

    it "should not show extract template link if user is not admin" do
      view.stub(:is_user_an_admin?).and_return(false)

      render

      expect(response.body).not_to have_selector("li a[class='add_link']", :text => "Extract Template")
      expect(response.body).not_to have_selector("span[class='action_icon add_icon_disabled']", :text => "Extract Template")
    end

    it "should disable extract template button for pipeline already using a template" do
      render

      expect(response.body).to have_selector("li span.add_icon_disabled.extract_template_for_pipeline_pipeline_with_template_in_group_foo", :text => "Extract Template")
      expect(response.body).to have_selector("li span.add_icon_disabled.extract_template_for_pipeline_pipeline_with_template_in_group_bar", :text => "Extract Template")
      expect(response.body).to have_selector("li span.add_icon_disabled.extract_template_for_pipeline_pipeline_with_template_in_group_quux", :text => "Extract Template")
      expect(response.body).not_to have_selector("li span.add_icon_disabled.extract_template_for_pipeline_pipeline_in_group_foo", :text => "Extract Template")
      expect(response.body).not_to have_selector("li span.add_icon_disabled.extract_template_for_pipeline_pipeline_2_in_group_bar", :text => "Extract Template")
      expect(response.body).not_to have_selector("li span.add_icon_disabled.extract_template_for_pipeline_pipeline_in_group_quux", :text => "Extract Template")
    end
  end

  it "should wire edit button" do
    render

    expect(response.body).to have_selector("li a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_in_group_bar", :current_tab => "general")}']")
    expect(response.body).to have_selector("li a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_2_in_group_bar", :current_tab => "general")}']")
    expect(response.body).to have_selector("li a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_in_group_foo", :current_tab => "general")}']")
    expect(response.body).to have_selector("li a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_2_in_group_foo", :current_tab => "general")}']")
    expect(response.body).to have_selector("li a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_in_group_quux", :current_tab => "general")}']")
    expect(response.body).to have_selector("li a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_2_in_group_quux", :current_tab => "general")}']")

  end

  it "should wire clone button" do
    render

    expect(response.body).to have_selector("li a.clone_button_for_pipeline_in_group_bar")
    expect(response.body).to have_selector("li a.clone_button_for_pipeline_2_in_group_bar")
    expect(response.body).to have_selector("li a.clone_button_for_pipeline_in_group_foo")
    expect(response.body).to have_selector("li a.clone_button_for_pipeline_2_in_group_foo")
    expect(response.body).to have_selector("li a.clone_button_for_pipeline_in_group_quux")
    expect(response.body).to have_selector("li a.clone_button_for_pipeline_2_in_group_quux")
  end

end
