#
# Copyright 2019 ThoughtWorks, Inc.
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

class Admin::PipelineGroupsController < AdminController
  helper SparkUrlAware

  CLONER = GoConfigCloner.new()

  before_action :load_config_for_edit
  before_action :load_groups_for_edit, :only => [:edit]
  before_action :autocomplete_for_permissions_and_tab, :only => [:edit, :update]

  layout "admin"

  def edit
  end

  def index
    load_groups_and_can_delete
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
