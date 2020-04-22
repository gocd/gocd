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

class Admin::TemplatesController < AdminController
  helper Admin::TemplatesHelper

  before_action :check_admin_user_and_403, only: [:edit_permissions, :update_permissions]
  before_action :check_admin_or_template_admin_and_403, only: [:edit]
  before_action :load_cruise_config, :only => [:edit, :edit_permissions]
  before_action :autocomplete_for_permissions, :only => [:edit_permissions]

  layout "admin", :except => [:create]

  def edit
    assert_load_eval :pipeline do
      @cruise_config.getTemplateByName(CaseInsensitiveString.new(params[:pipeline_name]))
    end
    render :layout => "templates/details", :action => params[:current_tab]
  end

  def edit_permissions
    @tab_name = "templates"
    assert_load_eval :pipeline do
      @cruise_config.getTemplateByName(CaseInsensitiveString.new(params[:template_name]))
    end
  end

  def update_permissions
    save_page(params[:config_md5], edit_template_permissions_path(params[:template_name]), {:action => :edit_permissions}, Class.new(::ConfigUpdate::SaveAsSuperAdmin) do
      include ::ConfigUpdate::LoadConfig

      def initialize params, user, security_service
        super(params, user, security_service)
      end

      def node(cruise_config)
        cruise_config.getTemplateByName(CaseInsensitiveString.new(params[:template_name]))
      end

      def update(template)
        template.setConfigAttributes(params[:template])
      end

      def subject(template)
        template
      end
    end.new(params, current_user, security_service)) do
      if @update_result.isSuccessful()
        set_save_redirect_url(edit_template_permissions_path(params[:template_name]))
      else
        assert_load(:pipeline, @cruise_config.getTemplateByName(CaseInsensitiveString.new(params[:template_name])))
        autocomplete_for_permissions
      end
    end
  end

  private

  def load_cruise_config
    assert_load :cruise_config, go_config_service.getConfigForEditing()
  end

  def autocomplete_for_permissions
    @autocomplete_users = user_service.allUsernames().to_json
    @autocomplete_roles = user_service.allRoleNames().to_json
  end
end
