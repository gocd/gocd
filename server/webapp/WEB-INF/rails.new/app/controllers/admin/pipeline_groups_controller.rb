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

class Admin::PipelineGroupsController < AdminController
  CLONER = Cloner.new()

  before_filter :load_config_for_edit
  before_filter :load_groups_for_edit, :only => [:edit, :show]
  before_filter :autocomplete_for_permissions_and_tab, :only => [:edit, :update, :show]

  layout "admin", :except => [:new, :create]

  def new
    assert_load :group, BasicPipelineConfigs.new
    render layout: false
  end

  def show
    render :edit
  end
  
  def index
    load_groups_and_can_delete
  end

  def create
    assert_load :group, BasicPipelineConfigs.new
    save_popup(params[:config_md5], Class.new(::ConfigUpdate::SaveAsSuperAdmin) do
      include ::ConfigUpdate::CruiseConfigNode

      def initialize params, user, security_service, group
        super(params, user, security_service)
        @group = group
      end

      def subject(cruise_config)
        group_name = params[:group][:group]
        cruise_config.findGroup(group_name)
      end

      def update(cruise_config)
        @group.setConfigAttributes(params[:group])
        cruise_config.getGroups().add(@group)
      end
    end.new(params, current_user, security_service, @group), {:action => :new, :layout => false}) do
      assert_load :group, @subject
    end
  end

  def destroy
    save_page(params[:config_md5], pipeline_groups_url, {:action => :index}, Class.new(::ConfigUpdate::SaveAsPipelineAdmin) do
      include ::ConfigUpdate::CruiseConfigNode

      def initialize params, user, security_service
        super(params, user, security_service)
      end

      def subject(cruise_config)
        cruise_config.pipelineConfigByName(pipeline_name) if cruise_config.hasPipelineNamed(pipeline_name)
      end

      def update(cruise_config)
        load_pipeline_group_config_for_pipeline(cruise_config).remove(load_pipeline(cruise_config))
      end

    end.new(params, current_user.getUsername(), security_service)) do
      load_groups_and_can_delete
    end
  end

  def destroy_group
    save_page(params[:config_md5], pipeline_groups_url, {:action => :index}, Class.new(::ConfigUpdate::SaveAsPipelineAdmin) do
      include ::ConfigUpdate::CruiseConfigNode
      include ::ConfigUpdate::GroupsGroupSubject

      def initialize params, user, security_service
        super(params, user, security_service)
      end

      def update(cruise_config)
        load_all_pipeline_groups(cruise_config).remove(load_pipeline_group_config(cruise_config))
      end

    end.new(params, current_user.getUsername(), security_service)) do
      load_groups_and_can_delete
    end
  end

  def move
    save_page(params[:config_md5], pipeline_groups_url, {:action => :index}, Class.new(::ConfigUpdate::SaveAsPipelineAdmin) do
      include ::ConfigUpdate::CruiseConfigNode
      include ::ConfigUpdate::LoadConfig

      def initialize params, user, security_service
        super(params, user, security_service)
      end

      def subject(cruise_config)
        cruise_config.pipelineConfigByName(pipeline_name)
      end

      def update(cruise_config)
        to_group = load_pipeline_group_config(cruise_config)
        from_group = cruise_config.findGroup(load_pipeline_group(cruise_config))
        pipeline_config = load_pipeline(cruise_config)
        from_group.remove(pipeline_config)
        to_group.add(pipeline_config)
      end

    end.new(params, current_user.getUsername(), security_service), "Pipeline moved successfully.") do
      load_groups_and_can_delete
    end
  end

  def update
    new_group_name =  params[:group][:group]
    save_page(params[:config_md5], nil, {:action => :edit}, Class.new(::ConfigUpdate::SaveAsPipelineAdmin) do
      include ::ConfigUpdate::PipelineGroupNode
      include ::ConfigUpdate::NodeAsSubject
      include ::ConfigUpdate::LoadConfig

      def initialize params, user, security_service
        super(params, user, security_service)
      end

      def update(group)
        group.setConfigAttributes(params[:group])
      end
    end.new(params, current_user.getUsername(), security_service)) do
      assert_load :group, @cruise_config.getGroups().findGroup(new_group_name)
      set_save_redirect_url(pipeline_group_edit_path(@group.getGroup())) if @update_result.isSuccessful()
    end
  end

  def possible_groups
    load_groups
    match = go_config_service.doesMd5Match(params[:config_md5])
    pipeline_to_be_moved = CaseInsensitiveString.new(params[:pipeline_name])
    target_groups = []
    @groups.each do |group|
      target_groups.push(group.getGroup()) if group.findBy(pipeline_to_be_moved).nil?
    end

    @possible_groups = target_groups
    @pipeline_name = pipeline_to_be_moved.toString()
    @md5_match = match

    render(:layout => false)
  end

  private
  def is_group_admin(group)
    security_service.isUserAdminOfGroup(current_user.getUsername(), group)
  end

  def load_groups
    assert_load(:groups, @cruise_config.getGroups().select do |group|
      is_group_admin(group.getGroup())
    end)
  end

  def load_groups_and_can_delete
    load_groups()
    assert_load :pipeline_to_can_delete, pipeline_config_service.canDeletePipelines()
  end

  def load_groups_for_edit
    result = HttpLocalizedOperationResult.new
    group_for_edit = go_config_service.loadGroupForEditing(params[:group_name], current_user, result)
    unless result.isSuccessful()
      render_localized_operation_result result
      return
    end
    assert_load :group, group_for_edit.getConfig()
  end

  def autocomplete_for_permissions_and_tab
    @in_pipeline_group_edit = true
    @tab_name = "pipeline-groups"
    @autocomplete_users = user_service.allUsernames().to_json
    @autocomplete_roles = user_service.allRoleNames(@cruise_config).to_json
  end

  def load_config_for_edit
    assert_load(:cruise_config, go_config_service.getMergedConfigForEditing())
  end
end