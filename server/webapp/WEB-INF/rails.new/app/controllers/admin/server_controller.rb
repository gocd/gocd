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

require 'pp'
require 'server_configuration_form'

class Admin::ServerController < AdminController
  include ApplicationHelper

  before_filter :set_tab_name, :only => [:index, :update]

  def index
    set_defaults
    cruise_config = go_config_service.getConfigForEditing()
    @cruise_config_md5 = cruise_config.getMd5()
    @server_configuration_form = ServerConfigurationForm.from_server_config(cruise_config.server())
  end

  def test_email
    result = HttpLocalizedOperationResult.new
    server_configuration_form = ServerConfigurationForm.new(params[:server_configuration_form])
    if server_configuration_form.validate(result)
      server_config_service.sendTestMail(server_configuration_form.to_mail_host, result = HttpLocalizedOperationResult.new)
    end
    render :json => to_operation_result_json(result, l.string("SENT_TEST_MAIL_SUCCESSFULLY"))
  end

  def update
    result = HttpLocalizedOperationResult.new
    @server_configuration_form = ServerConfigurationForm.new(params[:server_configuration_form])
    if @server_configuration_form.validate(result) &&
            update_server_config(@server_configuration_form.artifactsDir,
                                 @server_configuration_form.purgeStart,
                                 @server_configuration_form.purgeUpto,
                                 @server_configuration_form.jobTimeout,
                                 @server_configuration_form.should_allow_auto_login,
                                 @server_configuration_form.to_mail_host,
                                 @server_configuration_form.siteUrl,
                                 @server_configuration_form.secureSiteUrl,
                                 @server_configuration_form.commandRepositoryLocation,
                                 result)
      redirect_with_flash(result.message(localizer), :action => :index, :class => 'success')
    else
      render_index_with_error(result)
    end
  end

  def validate
    @result = server_config_service.validateEmail(params[:email]) if params[:email]
    if params[:port]
      if number?(params[:port])
        @result = server_config_service.validatePort(Integer(params[:port]))
      else
        @result = DefaultLocalizedResult.new()
        @result.invalid("INVALID_PORT");
      end
    end
    @result = server_config_service.validateHostName(params[:hostName]) if params[:hostName]
    render :json => to_operation_result_json(@result, l.string("VALID_VALUE"))
  end

  private
  def update_server_config(artifacts_dir, purgeStart, purgeUpto, jobTimeout, should_allow_auto_login, mail_host, site_url, secure_site_url,commandRepositoryLocation, result)
    server_config_service.updateServerConfig(mail_host, artifacts_dir, purgeStart, purgeUpto, jobTimeout, should_allow_auto_login, site_url, secure_site_url, commandRepositoryLocation, result, params[:cruise_config_md5])
    result.isSuccessful()
  end

  def render_index_with_error(result)
    session[:notice] = FlashMessageModel.new(result.message(localizer), 'error')
    set_defaults
    @cruise_config_md5 = params[:cruise_config_md5]
    render :action => :index
  end

  def set_tab_name
    @tab_name = 'server_configuration'
  end

  def absolute_path_of_command_repo_dir
    system_environment.getCommandRepositoryRootLocation() + File::SEPARATOR
  end

  private
  def set_defaults
    @allow_user_to_turn_off_auto_login = user_service.canUserTurnOffAutoLogin()
    @command_repository_base_dir_location ||= absolute_path_of_command_repo_dir
  end
end