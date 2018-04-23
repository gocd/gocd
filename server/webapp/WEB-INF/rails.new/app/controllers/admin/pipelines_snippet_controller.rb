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

class Admin::PipelinesSnippetController < AdminController
  before_action :set_tab_name
  before_action :load_config_for_edit, :only => [:edit]

  CLONER = Cloner.new

  def show
    set_group_data
    render_localized_operation_result @result and return unless @result.isSuccessful
  end

  def edit
    set_group_data
    render_localized_operation_result @result and return unless @result.isSuccessful
    @config_md5 = @cruise_config.getMd5()
  end

  def index
    @modifiable_groups = security_service.modifiableGroupsForUser(current_user)
    redirect_to pipelines_snippet_show_path(@modifiable_groups.first)
  end

  def update
    result = HttpLocalizedOperationResult.new
    cruise_config_repsonse = pipeline_configs_service.updateXml(params[:group_name], params[:group_xml], params[:config_md5], current_user, result)
    pipeline_configs = cruise_config_repsonse.getConfigElement()
    validity = cruise_config_repsonse.getValidity()
    @errors = [validity.errorMessage()] if (validity.isMergeConflict() || validity.isPostValidationError())
    localizer = Spring.bean('localizer')

    redirect_to pipelines_snippet_show_path(pipeline_configs.get_group, :fm => set_flash_message(result.message(localizer),'success')) and return if result.isSuccessful()

    flash.now[:error] = result.message(localizer)
    @modifiable_groups = security_service.modifiableGroupsForUser(current_user)
    @group_name = params[:group_name]
    @group_as_xml = params[:group_xml]
    @config_md5 = params[:config_md5]
    render :edit
  end

  private

  def set_group_data
    @modifiable_groups = security_service.modifiableGroupsForUser(current_user)
    @result = set_group_xml params[:group_name]
    @group_name = params[:group_name]
  end

  def set_group_xml group_name
    result = HttpLocalizedOperationResult.new
    @group_as_xml = pipeline_configs_service.getXml(group_name, current_user, result)
    return result
  end

  def set_tab_name
    @tab_name = "pipelines-snippet"
  end

  def load_config_for_edit
    assert_load(:cruise_config, go_config_service.getConfigForEditing())
  end
end