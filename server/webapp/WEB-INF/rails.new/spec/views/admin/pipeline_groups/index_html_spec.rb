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

require File.join(File.dirname(__FILE__), "..", "..", "..", "spec_helper")

describe "admin/pipeline_groups/index.html.erb" do

  include ReflectiveUtil

  before(:each) do
    assign(:groups, groups("group_foo", "group_bar", "group_quux"))
    assign(:user, Username.new(CaseInsensitiveString.new("loser")))
    template.stub(:tab_with_display_name).and_return("tab_link")
    template.stub(:mycruise_available?).and_return(false)
    template.stub(:can_view_admin_page?).and_return(true)
    assign(:cruise_config, cruise_config = CruiseConfig.new)
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
    template.stub(:is_user_an_admin?).and_return(true)
  end

  def groups(*named)
    named.map do |name|
      pipeline_one = PipelineConfigMother.pipelineConfig("pipeline_in_#{name}")
      pipeline_two = PipelineConfigMother.pipelineConfig("pipeline_2_in_#{name}")
      pipeline_three = PipelineConfigMother.pipelineConfigWithTemplate("pipeline_with_template_in_#{name}", "some_template")
      PipelineConfigs.new(name, Authorization.new, [pipeline_one, pipeline_two, pipeline_three].to_java(PipelineConfig))
    end
  end

  it "should set tab and page title" do
    render

    template.instance_variable_get('@tab_name').should == "pipeline-groups"
  end

  it "should display a message if the pipeline group is empty" do
    assign(:groups, [PipelineConfigs.new("group", Authorization.new, [].to_java(PipelineConfig))])
    assign(:pipeline_to_can_delete, {})

    render

    response.body.should have_tag("div.group_pipelines") do
      with_tag("div.group") do
        with_tag("h2.group_name", "group")
        with_tag("div.information", "No pipelines associated with this group")
        without_tag("table")
      end
    end
  end


  describe "create new group" do
    it "should remove the add new group for anyone other than super admin" do
      template.stub(:is_user_an_admin?).and_return(false)

      render

      response.body.should have_tag(".group_pipelines") do
        without_tag("a[href='#'][class='add_link']", "Add New Pipeline Group")
      end
    end
  end

  it "should display all pipelines with delete link" do
    render

    response.body.should have_tag("div.group_pipelines") do
      with_tag("div.group") do
        with_tag("h2.group_name", "group_foo")
        with_tag("a[href=?]", pipeline_group_edit_path(:group_name => "group_foo"))
        with_tag("table") do
          with_tag("thead tr.pipeline") do
            with_tag("th.name", "Pipeline")
            with_tag("th.actions", "Actions")
          end
          with_tag("tbody") do
            with_tag("tr.pipeline") do
              with_tag("td.name a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_in_group_foo", :current_tab => "general")}']", "pipeline_in_group_foo")
              with_tag("td.actions") do
                with_tag("ul") do
                  with_tag("li span.delete_parent")
                end
              end
            end

            with_tag("tr.pipeline") do
              with_tag("td.name a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_2_in_group_foo", :current_tab => "general")}']", "pipeline_2_in_group_foo")
              with_tag("td.actions") do
                with_tag("ul") do
                  with_tag("li span.delete_parent")
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
      assign(:groups, [PipelineConfigs.new("empty_group", Authorization.new, [].to_java(PipelineConfig))])
      render

      response.body.should have_tag("div.group_pipelines") do
        with_tag("div.group") do
          with_tag("h2.group_name", "empty_group")
          with_tag("form[id='delete_group_empty_group'][method='post'][action='#{pipeline_group_delete_path(:group_name => 'empty_group')}'][title=?]", "Delete this pipeline group") do
            with_tag("input[name='_method'][value='delete']")
            with_tag("span#trigger_delete_group_empty_group")
            with_tag("script[type='text/javascript']", /Util.escapeDotsFromId\('trigger_delete_group_empty_group #warning_prompt'\)/)
            with_tag("div#warning_prompt[style='display:none;']", /Are you sure you want to delete the pipeline group 'empty_group' \?/)
          end
        end
      end
    end

    it "should not display delete link if user is group admin" do
      assign(:groups, [PipelineConfigs.new("empty_group", Authorization.new, [].to_java(PipelineConfig))])
      template.stub(:is_user_an_admin?).and_return(false)

      render

      response.body.should_not have_tag("form#delete_group_empty_group")
      response.body.should_not have_tag("span.icon_cannot_remove")
    end

    it "should display disabled delete link next to a non-empty pipeline group" do
      render 'admin/pipeline_groups/index.html'

      response.body.should have_tag("div.group_pipelines") do
        with_tag("div.group") do
          with_tag("h2.group_name", "group_foo")
          with_tag("span.delete_icon_disabled[title=?]", "Move or Delete all pipelines within this group in order to delete it.")
          without_tag("form[id='delete_group_group_foo'][method='post'][action='#{pipeline_group_delete_path(:group_name => 'group_foo')}']")
        end
      end
    end

    it "should have unique random id for delete pipeline link" do
      template.stub(:random_dom_id).and_return("some_random_id")

      render

      response.body.should have_tag("form[action='/admin/pipelines/pipeline_in_group_foo'][id='some_random_id_form']") do
        with_tag("span#trigger_some_random_id")
      end
      response.body.should have_tag("form[action='/admin/pipelines/pipeline_2_in_group_foo'][id='some_random_id_form']") do
        with_tag("span#trigger_some_random_id")
      end
      response.body.should have_tag("form[action='/admin/pipelines/pipeline_in_group_bar'][id='some_random_id_form']") do
        with_tag("span#trigger_some_random_id")
      end
      response.body.should have_tag("form[action='/admin/pipelines/pipeline_2_in_group_bar'][id='some_random_id_form']") do
        with_tag("span#trigger_some_random_id")
      end
    end

    it "should display all pipelines with delete link" do
      render

      response.body.should have_tag("div.group_pipelines") do
        with_tag("div.group") do
          with_tag("h2.group_name", "group_foo")
          with_tag("a[href=?]", pipeline_group_edit_path(:group_name => "group_foo"))
          with_tag("table") do
            with_tag("thead tr.pipeline") do
              with_tag("th.name", "Pipeline")
              with_tag("th.actions", "Actions")
            end
            with_tag("tbody") do
              with_tag("tr.pipeline") do
                with_tag("td.name a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_in_group_foo", :current_tab => "general")}']", "pipeline_in_group_foo")
                with_tag("td.actions") do
                  with_tag("ul") do
                    with_tag("li span.delete_parent")

                  end
                end
              end

              with_tag("tr.pipeline") do
                with_tag("td.name a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_2_in_group_foo", :current_tab => "general")}']", "pipeline_2_in_group_foo")
                with_tag("td.actions") do
                  with_tag("ul") do
                    with_tag("li span.delete_parent")
                    
                  end
                end
              end
            end
          end
        end

        with_tag("td.name a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_in_group_bar", :current_tab => "general")}']", "pipeline_in_group_bar")
        with_tag("td.name a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_2_in_group_bar", :current_tab => "general")}']", "pipeline_2_in_group_bar")
      end
    end

    it "should wire delete action" do
      render

      response.body.should have_tag("form[action='/admin/pipelines/pipeline_in_group_foo'][method='post']") do
        with_tag("span.delete_parent")
        with_tag("input[name='_method'][value='delete']")
      end
      response.body.should have_tag("form[action='/admin/pipelines/pipeline_2_in_group_foo'] span.delete_parent")

      response.body.should have_tag("form[action='/admin/pipelines/pipeline_in_group_bar'] span.delete_parent")
      response.body.should have_tag("form[action='/admin/pipelines/pipeline_2_in_group_bar'] span.delete_parent")
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

      response.body.should have_tag("form[action='/admin/pipelines/pipeline_in_group_foo'][method='post']") do
        with_tag("span.delete_icon_disabled[title=?]", "Cannot delete pipeline 'pipeline_in_group_foo' as it is present in environment 'env'")
      end

      response.body.should have_tag("form[action='/admin/pipelines/pipeline_in_group_bar'][method='post']") do
        with_tag("span.delete_icon_disabled[title=?]", "Cannot delete pipeline 'pipeline_in_group_bar' as pipeline 'pipeline_in_group_foo' depends on it")
      end

      response.body.should have_tag("form[action='/admin/pipelines/pipeline_2_in_group_foo'][method='post']") do
        with_tag("span.delete_icon[title=?]", "Delete this pipeline")
      end

      response.body.should have_tag("form[action='/admin/pipelines/pipeline_2_in_group_bar'][method='post']") do
        with_tag("span.delete_icon[title=?]", "Delete this pipeline")
      end
    end
  end

  it "should render move control only when more than one pipeline-groups are present" do
    assign(:groups, groups("group_foo"))

    render

    response.body.should_not have_tag(".hidden#move_pipeline_from_group_group_foo_pipeline_in_group_foo")

    response.body.should_not have_tag"li form[action='#{move_pipeline_to_group_path(:pipeline_name => 'pipeline_in_group_foo')}'][method='post']"
  end

  describe "move pipeline" do
    it "should render move control only when more than one pipeline-groups are present" do
      assign(:groups, groups("group_foo"))

      render

      response.body.should_not have_tag(".hidden#move_pipeline_from_group_group_foo_pipeline_in_group_foo")

      response.body.should_not have_tag "li form[action='#{move_pipeline_to_group_path(:pipeline_name => 'pipeline_in_group_foo')}'][method='post']"
    end
  end

  describe "new pipeline wizard links" do

    it "should display new-pipeline-links which take you to the new wizard" do
      assign(:groups, groups("group_foo"))

      render

      response.body.should have_tag("div.group_pipelines") do
        with_tag("a.add_link.add_pipeline_to_group[href=?]", pipeline_new_path(:group => "group_foo"))
      end
    end

    it "should display link when no pipelines exist for a group" do
      assign(:groups, [PipelineConfigs.new("some-group", Authorization.new, [].to_java(PipelineConfig))])

      render

      response.body.should have_tag("div.add_first_pipeline_in_group") do
        with_tag("a.add_link.add_pipeline_to_group[href=?]", pipeline_new_path(:group => "some-group"))
      end
    end

    it "should display link to add first pipeline" do
      assign(:groups, [])

      render

      response.body.should have_tag("span.title_secondary_info") do
        with_tag("a.add_link.add_pipeline_to_group[href=?]", pipeline_new_path(:group => "defaultGroup"))
      end
    end

  end

  describe "extract template" do
    it "should have extract template button for each pipeline" do
      template.stub(:is_user_an_admin?).and_return(true)

      render

      response.body.should have_tag("li a[href='#'][class='add_link'][onclick=?]", "Util.ajax_modal('/admin/templates/new?pipelineToExtractFrom=pipeline_in_group_foo', {overlayClose: false, title: 'Extract Template'}, function(text){return text})")
      response.body.should have_tag("li a[href='#'][class='add_link'][onclick=?]", "Util.ajax_modal('/admin/templates/new?pipelineToExtractFrom=pipeline_2_in_group_bar', {overlayClose: false, title: 'Extract Template'}, function(text){return text})")
      response.body.should have_tag("li a[href='#'][class='add_link'][onclick=?]", "Util.ajax_modal('/admin/templates/new?pipelineToExtractFrom=pipeline_in_group_foo', {overlayClose: false, title: 'Extract Template'}, function(text){return text})")
      response.body.should have_tag("li a[href='#'][class='add_link'][onclick=?]", "Util.ajax_modal('/admin/templates/new?pipelineToExtractFrom=pipeline_2_in_group_foo', {overlayClose: false, title: 'Extract Template'}, function(text){return text})")
      response.body.should have_tag("li a[href='#'][class='add_link'][onclick=?]", "Util.ajax_modal('/admin/templates/new?pipelineToExtractFrom=pipeline_in_group_quux', {overlayClose: false, title: 'Extract Template'}, function(text){return text})")
      response.body.should have_tag("li a[href='#'][class='add_link'][onclick=?]", "Util.ajax_modal('/admin/templates/new?pipelineToExtractFrom=pipeline_2_in_group_quux', {overlayClose: false, title: 'Extract Template'}, function(text){return text})")
    end

    it "should not show extract template link if user is not admin" do
      template.stub(:is_user_an_admin?).and_return(false)

      render

      response.body.should_not have_tag("li a[class='add_link']", "Extract Template")
      response.body.should_not have_tag("span[class='action_icon add_icon_disabled']", "Extract Template")
    end

    it "should disable extract template button for pipeline already using a template" do
      render

      response.body.should have_tag("li span.add_icon_disabled.extract_template_for_pipeline_pipeline_with_template_in_group_foo", "Extract Template")
      response.body.should have_tag("li span.add_icon_disabled.extract_template_for_pipeline_pipeline_with_template_in_group_bar", "Extract Template")
      response.body.should have_tag("li span.add_icon_disabled.extract_template_for_pipeline_pipeline_with_template_in_group_quux", "Extract Template")
      response.body.should.should_not have_tag("li span.add_icon_disabled.extract_template_for_pipeline_pipeline_in_group_foo", "Extract Template")
      response.body.should.should_not have_tag("li span.add_icon_disabled.extract_template_for_pipeline_pipeline_2_in_group_bar", "Extract Template")
      response.body.should.should_not have_tag("li span.add_icon_disabled.extract_template_for_pipeline_pipeline_in_group_quux", "Extract Template")
    end
  end

  it "should wire edit button" do
    render

    response.body.should have_tag("li a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_in_group_bar", :current_tab => "general")}']")
    response.body.should have_tag("li a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_2_in_group_bar", :current_tab => "general")}']")
    response.body.should have_tag("li a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_in_group_foo", :current_tab => "general")}']")
    response.body.should have_tag("li a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_2_in_group_foo", :current_tab => "general")}']")
    response.body.should have_tag("li a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_in_group_quux", :current_tab => "general")}']")
    response.body.should have_tag("li a[href='#{pipeline_edit_path(:pipeline_name => "pipeline_2_in_group_quux", :current_tab => "general")}']")

  end

  it "should wire clone button" do
    render

    response.body.should have_tag("li a.clone_button_for_pipeline_in_group_bar")
    response.body.should have_tag("li a.clone_button_for_pipeline_2_in_group_bar")
    response.body.should have_tag("li a.clone_button_for_pipeline_in_group_foo")
    response.body.should have_tag("li a.clone_button_for_pipeline_2_in_group_foo")
    response.body.should have_tag("li a.clone_button_for_pipeline_in_group_quux")
    response.body.should have_tag("li a.clone_button_for_pipeline_2_in_group_quux")
  end

end
