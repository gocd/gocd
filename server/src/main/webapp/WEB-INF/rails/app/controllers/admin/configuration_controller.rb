#
# Copyright Thoughtworks, Inc.
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

class Admin::ConfigurationController < AdminController
  before_action :check_admin_user_and_403
  before_action :fetch_config , :only => [:show, :edit]
  before_action :tab_name, :page_title

  def show
    fetch_cruise_config_revision @go_config.md5
  end

  def edit
    fetch_cruise_config_revision @go_config.md5
  end

  def update
    result = HttpLocalizedOperationResult.new
    config_validity = admin_service.updateConfig(params[:go_config], result)
    unless config_validity.isValid()
      flash.now[:error] = 'Save failed, see errors below'
      @errors = [config_validity.errorMessage()]
      fetch_config
      if config_edit_conflicted?(config_validity)
        flash.now[:error] = 'Someone has modified the configuration and your changes are in conflict. Please review, amend and retry.'
        @conflicted_config = GoConfig.new(params[:go_config])
        fetch_cruise_config_revision @go_config.md5
        render :split_pane and return
      else
        @go_config = GoConfig.new(params[:go_config])
        fetch_cruise_config_revision @go_config.md5
        @echo_user_submission = true
        render :edit and return
      end
    end
    flash[:success] = config_validity.wasMerged() ? "Saved successfully. The configuration was modified by someone else, but your changes were merged successfully." : 'Saved successfully.'
    redirect_to config_view_path
  end

  private
  def fetch_config
    config = config_repository.getRevision('current')
    @go_config = GoConfig.new(content: config.getContent, md5: config.getMd5)
  end

  def fetch_cruise_config_revision md5
    @go_config_revision = config_repository.getRevision(md5)
  end

  def config_edit_conflicted? config_validity
    !config_validity.isValid() && (config_validity.isMergeConflict() || config_validity.isPostValidationError())
  end

  def tab_name
    @tab_name = 'configuration-xml'
  end

  def page_title
    @view_title = 'Administration'
  end
end

