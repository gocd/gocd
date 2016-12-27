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

class Admin::TemplatesController < AdminController

  before_filter :load_templates_from_service, :only => :index
  before_filter :load_cruise_config, :only => [:new, :edit, :index, :destroy, :edit_permissions]
  before_filter :autocomplete_for_permissions, :only => [:edit_permissions]

  layout "admin", :except => [:new, :create]

  def new
    pipelineName = CaseInsensitiveString.new(params[:pipelineToExtractFrom])
    if pipelineName.empty? || (!pipelineName.empty? && @cruise_config.hasPipelineNamed(pipelineName))
      assert_load :pipeline, create_empty_template_view_model
      render :layout => false
    else
      render_error_template(l.string("RESOURCE_NOT_FOUND", 'pipeline', [pipelineName]), 404)
    end
  end

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

  def create
    assert_load :pipeline, create_empty_template_view_model
    @pipeline.setConfigAttributes(params[:pipeline])
    template_name = params[:pipeline][:template][:name]
    save_popup(params[:config_md5], Class.new(::ConfigUpdate::SaveAsSuperAdmin) do
      include ::ConfigUpdate::CruiseConfigNode

      def initialize params, user, security_service, template_view_model
        super(params, user, security_service)
        @view_model = template_view_model
        @template = template_view_model.templateConfig()
      end

      def subject(cruise_config)
        @template
      end

      def update(cruise_config)
        templates = cruise_config.getTemplates()
        templates.add(@template)
        cruise_config.makePipelineUseTemplate(CaseInsensitiveString.new(@view_model.selectedPipelineName), @template.name()) if @view_model.useExistingPipeline
      end
    end.new(params, current_user, security_service, @pipeline), {:action => :new, :layout => false}, {:action => "edit", :stage_parent => "templates", :pipeline_name => template_name, :current_tab => "general"}) do
      assert_load :pipeline, @pipeline
    end
  end

  def destroy
    template_name = params[:pipeline_name]
    load_templates_from_service
    dependent_pipelines = @template_to_pipelines[CaseInsensitiveString.new(template_name)]
    redirect_to templates_path(:fm => set_error_flash("TEMPLATE_HAS_DEPENDENT_PIPELINES_ERROR", template_name)) and return if !dependent_pipelines.empty?
    save_page(params[:config_md5], templates_path, {:action => :index}, Class.new(::ConfigUpdate::SaveAsSuperAdmin) do
      include ::ConfigUpdate::TemplatesNode
      include ::ConfigUpdate::TemplatesTemplateSubject

      def update(templates)
        templates.removeTemplateNamed(CaseInsensitiveString.new(template_name))
      end
    end.new(params, current_user, security_service)) do
      load_templates_from_service
    end
  end

  def stages

  end

  private

  def load_templates_from_service
    load_templates template_config_service
  end

  def load_templates from
    assert_load :template_to_pipelines, from.templatesWithPipelinesForUser(current_user.getUsername.toString)
  end

  def load_cruise_config
    result = HttpLocalizedOperationResult.new
    assert_load :cruise_config, go_config_service.loadCruiseConfigForEdit(current_user, result)
    unless result.isSuccessful()
      render_localized_operation_result result
    end
  end

  def create_empty_template_view_model
    PipelineTemplateConfigViewModel.new(PipelineTemplateConfig.new(), params[:pipelineToExtractFrom], template_config_service.allPipelinesNotUsingTemplates(current_user, HttpLocalizedOperationResult.new))
  end

  helper_method :default_url_options, :allow_pipeline_selection?
  def default_url_options(options = nil)
    super.reverse_merge(params.only(:allow_pipeline_selection).symbolize_keys)
  end

  TRUE = true.to_s

  def allow_pipeline_selection?
    params[:allow_pipeline_selection] == TRUE
  end

  def autocomplete_for_permissions
    @autocomplete_users = user_service.allUsernames().to_json
  end
end
