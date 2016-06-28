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

class ValueStreamMapModel

  attr_accessor :current_pipeline, :current_material, :levels, :error

  NODE_TYPE_FOR_PIPELINE = com.thoughtworks.go.domain.valuestreammap.DependencyNodeType::PIPELINE.to_s
  NODE_TYPE_FOR_MATERIAL = com.thoughtworks.go.domain.valuestreammap.DependencyNodeType::MATERIAL.to_s

  def initialize(vsm, error, localizer, vsm_path_partial = proc do
    ""
  end, vsm_material_path_partial = proc do
    ""
  end, stage_detail_path_partial = proc do
    ""
  end)
    if error
      @error = error
    else
      @current_pipeline = vsm.getCurrentPipeline().getName() unless vsm.getCurrentPipeline() == nil
      @current_material = vsm.getCurrentMaterial().getId() unless vsm.getCurrentMaterial() == nil
      @levels = Array.new
      vsm.getNodesAtEachLevel().each do |nodes|
        level = VSMPipelineDependencyLevelModel.new()
        level.nodes = Array.new
        nodes.each do |node|
          node_type = node.getType().to_s
          if (node_type == NODE_TYPE_FOR_MATERIAL)
            level.nodes << VSMSCMDependencyNodeModel.new(node.getId(), node.getName(), node.getChildren().map { |child| child.getId() },
                                                         node.getParents().map { |parent| parent.getId() }, node.getMaterialType().upcase,
                                                         node.getDepth(), node.getMaterialNames(), vsm_material_path_partial, node.getMaterialRevisions(), node.getViewType(), node.getMessageString(localizer))
          elsif (node_type == NODE_TYPE_FOR_PIPELINE)
            level.nodes << VSMPipelineDependencyNodeModel.new(node.getId(), node.getName(), node.getChildren().map { |child| child.getId() },
                                                              node.getParents().map { |parent| parent.getId() }, node_type,
                                                              node.getDepth(), node.revisions(), node.getMessageString(localizer), node.getViewType(), vsm_path_partial, stage_detail_path_partial)
          else
            level.nodes << VSMDependencyNodeModel.new(node.getId(), node.getName(), node.getChildren().map { |child| child.getId() },
                                                 node.getParents().map { |parent| parent.getId() }, node_type,
                                                 node.getDepth())
          end
        end
        @levels << level
      end
    end
  end
end

class VSMPipelineDependencyLevelModel
  attr_accessor :nodes
end

class VSMDependencyNodeModel
  attr_accessor :name, :id, :dependents, :parents, :node_type, :depth, :instances, :locator

  def initialize(id, name, dependents, parents, node_type, depth)
    @id = id
    @name = name
    @dependents = dependents
    @parents = parents
    @node_type = node_type
    @depth = depth
    @instances = []
    @locator = ""
  end
end

class VSMPipelineDependencyNodeModel < VSMDependencyNodeModel
  attr_accessor :name, :id, :dependents, :parents, :node_type, :depth, :instances, :locator, :message, :view_type

  def initialize(id, name, dependents, parents, node_type, depth, revisions, message, view_type, pdg_path_partial, stage_detail_path_partial)
    super(id, name, dependents, parents, node_type, depth)

    @instances = revisions.map { |revision| VSMPipelineInstanceModel.new(name, revision.getLabel(), revision.getCounter(), revision.getStages() || [], pdg_path_partial, stage_detail_path_partial) } unless revisions == nil
    @locator = "/go/tab/pipeline/history/#{name}" if  view_type == nil
    @message = message unless  message == nil
    @view_type = view_type.to_s unless view_type == nil
  end
end

class VSMSCMDependencyNodeModel < VSMDependencyNodeModel
  attr_accessor :name, :id, :dependents, :parents, :node_type, :depth, :material_names, :locator, :view_type, :message, :material_revisions

  def initialize(id, name, dependents, parents, node_type, depth, material_names, vsm_material_path_partial, material_revisions, view_type, message)
    super(id, name, dependents, parents, node_type, depth)

    @material_names = material_names.map { |material_name| String.new(material_name) } unless material_names.isEmpty()
    @material_revisions = material_revisions.map { |revision| VSMSCMMaterialRevisionsModel.new(id, revision, vsm_material_path_partial) }
    @view_type = view_type.to_s unless view_type == nil
    @message = message unless  message == nil
  end
end

class VSMPipelineInstanceModel
  attr_accessor :label, :counter, :locator, :stages

  def initialize(name, label, counter, stages, pdg_path_partial, stage_detail_path_partial)
    @label = label
    @counter = counter
    @locator = ""
    @locator = pdg_path_partial.call name, counter unless counter == 0
    @stages = stages.map { |stage| VSMPipelineInstanceStageModel.new(stage.getName(), stage.getState().to_s, stage.getCounter(), name, counter, stage_detail_path_partial) }
  end
end

class VSMSCMMaterialRevisionsModel
  include RailsLocalizer

  attr_accessor :modifications

  def initialize(material_fingerprint, revision, vsm_material_path_partial)
    @modifications = revision.getModifications.map {|modification| VSMSCMModificationsModel.new(material_fingerprint, modification, vsm_material_path_partial) }
  end
end

class VSMSCMModificationsModel
  include RailsLocalizer

  attr_accessor :revision, :user, :comment, :modified_time, :locator

  def initialize(material_fingerprint, modification, vsm_material_path_partial)
    @revision = modification.getRevision
    @user = modification.getUserName
    @comment = modification.getComment()
    @modified_time=com.thoughtworks.go.util.TimeConverter.convert(modification.getModifiedTime).default_message
    @locator = vsm_material_path_partial.call material_fingerprint, modification.getRevision
  end
end

class VSMPipelineInstanceStageModel
  attr_accessor :name, :status, :locator

  def initialize(name, status, counter, pipeline_name, pipeline_counter, stage_detail_path_partial)
    @name = name
    @status = status
    @locator = ""
    @locator = stage_detail_path_partial.call pipeline_name, pipeline_counter, name, counter unless com.thoughtworks.go.domain.StageState::Unknown.to_s == status
  end
end
