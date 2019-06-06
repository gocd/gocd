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

module ApiV1
  module Admin
    module Internal
      class MaterialTestController < ::ApiV1::BaseController
        include_package 'com.thoughtworks.go.server.service'
        include_package 'com.thoughtworks.go.config.preprocessor'
        java_import com.thoughtworks.go.config.exceptions.RecordNotFoundException

        before_action :check_admin_user_or_group_admin_user_and_403

        def test
          material_config = ApiV1::Admin::Pipelines::Materials::MaterialRepresenter.new(ApiV1::Admin::Pipelines::Materials::MaterialRepresenter.get_material_type(params[:type]).new).from_hash(params)
          group_name = params[:pipeline_group]
          if group_name == nil
            group_name = go_config_service.findGroupNameByPipeline(CaseInsensitiveString.new(params[:pipeline_name]))
          end
          material_config.validateConcreteScmMaterial(PipelineConfigSaveValidationContext.forChain(false, group_name, go_config_service.cruise_config(), material_config))
          if material_config.errors.any?
            json = ApiV1::Admin::Pipelines::Materials::MaterialRepresenter.new(material_config).to_hash(url_builder: self)
            return render_message(validation_errors_as_error_message(material_config.errors), 422, {data: json})
          end

          material_config.ensureEncrypted() if material_config.respond_to?(:ensureEncrypted)
          perform_param_expansion(material_config) unless params[:pipeline_name].blank?

          material = MaterialConfigConverter.new.toMaterial(material_config)
          if material.respond_to?(:checkConnection)
            validation_bean = check_connection_for_material material
            if validation_bean.isValid
              render_message('Connection OK.', :ok)
            else
              render_message(validation_bean.getError, :unprocessable_entity)
            end
          else
            render_message("The material of type `#{params[:type]}` does not support connection testing.", :unprocessable_entity)
          end
        end

        def perform_param_expansion(material_config)
          existing_pipeline = go_config_service.pipelineConfigNamed(CaseInsensitiveString.new(params[:pipeline_name]))

          pipeline_config = PipelineConfig.new
          pipeline_config.name = params[:pipeline_name]
          pipeline_config.params = GoConfigCloner.new.deepClone(existing_pipeline.getParams)
          pipeline_config.material_configs << material_config

          ConfigParamPreprocessor.new.process(pipeline_config)
        rescue RecordNotFoundException
          raise ApiV1::UnprocessableEntity, "The specified pipeline `#{params[:pipeline_name]}` was not found!"
        end

        private

        cattr_accessor :check_connection_execution_context

        def validation_errors_as_error_message(errors)
          combined_error_message = errors.map {|field, error| "- #{field}: #{error.join(", ")}"}.join("\n")
          "There was an error with the material configuration.\n" + combined_error_message
        end

        def check_connection_for_material material
          begin
            secret_param_resolver.resolve(material.getSecretParams()) if material.is_a?(ScmMaterial)
            material.checkConnection(subprocess_execution_context)
          rescue com.thoughtworks.go.config.exceptions.UnresolvedSecretParamException, com.thoughtworks.go.plugin.access.exceptions.SecretResolutionFailureException => e
            com.thoughtworks.go.domain.materials.ValidationBean.notValid(e.message);
          end
        end

        def subprocess_execution_context
          if self.class.check_connection_execution_context.nil?
            self.class.check_connection_execution_context = CheckConnectionSubprocessExecutionContext.new system_environment
          end
          self.class.check_connection_execution_context
        end
      end
    end
  end
end
