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

class Admin::PackageDefinitionsController < AdminController
  include ApplicationHelper

  layout false

  before_filter :load_config_for_edit, :only => [:show, :show_for_new_pipeline_wizard, :show_with_repository_list, :new, :new_for_new_pipeline_wizard, :pipelines_used_in]
  before_filter :initialize_variables, :only => [:show, :show_for_new_pipeline_wizard, :new, :new_for_new_pipeline_wizard, :pipelines_used_in]

  def show
    return if @errors
    @package_configuration = PackageViewModel.new(@metadata, @repo.findPackage(params[:package_id])).filterSecureProperties!
  end

  def show_for_new_pipeline_wizard
    return if @errors
    @package_configuration = PackageViewModel.new(@metadata, @repo.findPackage(params[:package_id])).filterSecureProperties!
  end

  def show_with_repository_list
    @tab_name = "package-repositories"
    @repo = @cruise_config.getPackageRepositories().find(params[:repo_id])

    if @repo.nil? || @repo.findPackage(params[:package_id]).nil?
      render_error_template(l.string("PACKAGE_NOT_FOUND", [params[:package_id], params[:repo_id]].to_java(java.lang.String)),404) and return
    end

    plugin_id = @repo.getPluginConfiguration().getId()
    @metadata = PackageMetadataStore.getInstance().getMetadata(plugin_id)
    @errors = l.string("ASSOCIATED_PLUGIN_NOT_FOUND", [plugin_id].to_java(java.lang.String)) unless @metadata

    @package_configuration = PackageViewModel.new(@metadata, @repo.findPackage(params[:package_id])).filterSecureProperties!
    package_repository_list
    package_to_pipeline_map
    render :layout => "admin"
  end

  def new
    return if @errors
    @package_configuration = PackageViewModel.new(@metadata, PackageDefinition.new)
  end

  def new_for_new_pipeline_wizard
    return if @errors
    @package_configuration = PackageViewModel.new(@metadata, PackageDefinition.new)
  end

  def pipelines_used_in
    package_to_pipeline_map
    @pipelines_with_group= @package_to_pipeline_map.get(params[:package_id])
  end

  def destroy
    package_delete_command = Class.new(::ConfigUpdate::SaveAsGroupAdmin) do
      def node(cruise_config)
        cruise_config
      end

      def subject(cruise_config)
        cruise_config.getPackageRepositories()
      end

      def update(cruise_config)
        packageRepository = cruise_config.getPackageRepositories().findPackageRepositoryWithPackageIdOrBomb(params[:package_id])
        packageRepository.removePackage(params[:package_id])
      end
    end.new(params, current_user, security_service)

    save_page(params[:config_md5], package_repositories_list_path, {:action => :show_with_repository_list, :package_id => params[:package_id], :layout => "admin"}, package_delete_command) do
      initialize_variables
      package = @repo.findPackage(params[:package_id])
      if package != nil
        @package_configuration = PackageViewModel.new(@metadata, package).filterSecureProperties!
      end
      package_repository_list
      package_to_pipeline_map
    end
  end

  def check_connection
    package_material_params = params[:material]
    package_repository = go_config_service.getCurrentConfig().getPackageRepositories().find(package_material_params[:package_definition][:repositoryId])
    package_definition = package_repository.findOrCreatePackageDefinition(package_material_params)
    result = HttpLocalizedOperationResult.new
    package_definition_service.check_connection(package_definition, result)
    render :json => to_operation_result_json(result)
  end

  private

  def initialize_variables
    @repo = @cruise_config.getPackageRepositories().find(params[:repo_id])
    @errors = l.string("PACKAGE_REPOSITORY_NOT_FOUND", [params[:repo_id]].to_java(java.lang.String)) and return unless @repo
    plugin_id = @repo.getPluginConfiguration().getId()
    @metadata = PackageMetadataStore.getInstance().getMetadata(plugin_id)
    @errors = l.string("ASSOCIATED_PLUGIN_NOT_FOUND", [plugin_id].to_java(java.lang.String)) and return unless @metadata
  end

  def load_config_for_edit
    assert_load(:cruise_config, go_config_service.getConfigForEditing())
  end

  def package_repository_list
    @package_repositories = @cruise_config.getPackageRepositories()
  end

  def package_to_pipeline_map
    @package_to_pipeline_map = @cruise_config.getGroups().getPackageUsageInPipelines()
  end

end
