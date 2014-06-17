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

class Admin::PackageRepositoriesController < AdminController
  include ApplicationHelper

  layout "admin"

  before_filter :load_config_for_edit, :only => [:new, :list ,:create, :edit, :update, :destroy, :plugin_config, :plugin_config_for_repo]

  before_filter :package_repository_list, :only => [:new, :list, :edit]

  before_filter :package_to_pipeline_map, :only => [:new, :list, :edit]

  before_filter :set_tab_name, :only => [:new, :list, :edit, :destroy]

  def new
    @package_repository = PackageRepository.new
  end

  def list
    @package_repository = PackageRepository.new
    render :new
  end

  def edit
    @package_repository = @cruise_config.getPackageRepositories().find(params[:id])
    if @package_repository.nil?
      render_error_template(l.string("PACKAGE_REPOSITORY_NOT_FOUND", [params[:id]].to_java(java.lang.String)),404) and return
    end
    set_repository_configuration @package_repository, @package_repository.getPluginConfiguration().getId()
  end

  def create
    create_response = save_or_update_repo()
    render :json => create_response.toJson(), :status => create_response.getStatusCode()
  end

  def update
    update_response = save_or_update_repo()
    render :json => update_response.toJson(), :status => update_response.getStatusCode()
  end

  def save_or_update_repo
    @package_repository = PackageRepository.new
    @package_repository.setConfigAttributes(params[:package_repository])
    update_result = package_repository_service.savePackageRepositoryToConfig(@package_repository, params[:config_md5], current_user)
    if update_result.isSuccessful
      update_result.setRedirectUrl(package_repositories_edit_path(:id => update_result.getSubjectIdentifier()))
      flash[:success] = update_result.getMessage()
      response.location = update_result.getRedirectUrl()
    else
      response.headers[GO_CONFIG_ERROR_HEADER] = update_result.getMessage()
    end
    update_result
  end

  def plugin_config
    set_repository_configuration nil, params[:plugin]
    @plugin_id = params[:plugin]
    @isNewRepo = true
    render :layout => false
  end

  def plugin_config_for_repo
    set_repository_configuration @cruise_config.getPackageRepositories().find(params[:id]), params[:plugin]
    @plugin_id = params[:plugin]
    @isNewRepo = false
    render :layout => false
  end

  def check_connection
    package_repository = PackageRepository.new
    package_repository.setConfigAttributes(params[:package_repository])
    result = HttpLocalizedOperationResult.new
    package_repository_service.checkConnection(package_repository, result)
    render :json => to_operation_result_json(result)
  end

  def destroy
    save_page(params[:config_md5], package_repositories_list_path, {:action => :edit, :id => params[:id], :layout=> "admin"}, Class.new(::ConfigUpdate::SaveAsGroupAdmin) do
      def node(cruise_config)
        cruise_config
      end

      def subject(cruise_config)
        cruise_config.getPackageRepositories()
      end

      def update(cruise_config)
        cruise_config.removePackageRepository(params[:id])
      end
    end.new(params, current_user, security_service)) do
      unless @update_result.isSuccessful()
        load_config_for_edit
        package_repository_list
        package_to_pipeline_map
        @package_repository = @cruise_config.getPackageRepositories().find(params[:id])
        set_repository_configuration @package_repository, @package_repository.getPluginConfiguration().getId()
      end
    end
  end

  def load_config_for_edit
    assert_load(:cruise_config, get_cloner_instance.deepClone(go_config_service.getConfigForEditing()))
  end


  private
    def set_repository_configuration repo, plugin_id
      repo_configurations = RepositoryMetadataStore.getInstance().getMetadata(plugin_id)
      @repository_configuration = RepoViewModel.new(repo_configurations, repo, plugin_id)
      @errors = @repository_configuration.errors.getAll().to_a
    end

    def package_repository_list
      @package_repositories =  @cruise_config.getPackageRepositories()
    end

    def package_to_pipeline_map
      @package_to_pipeline_map = @cruise_config.getGroups().getPackageUsageInPipelines()
    end

    def get_cloner_instance
      @cloner ||= Cloner.new
    end

    def set_tab_name
      @tab_name = "package-repositories"
    end

end
