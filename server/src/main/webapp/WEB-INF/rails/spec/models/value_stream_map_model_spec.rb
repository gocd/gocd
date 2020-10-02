#
# Copyright 2020 ThoughtWorks, Inc.
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

require 'rails_helper'


describe ValueStreamMapModel do

  before :each do
    @result = double("HttpLocalizedOperationResult")
  end

  it "should initialize graph model correctly" do
    allow(@result).to receive(:isSuccessful).and_return(true)
    # git -> p1 -> current
    #  +---- X -----^
    #
    vsm = ValueStreamMap.new(CaseInsensitiveString.new("current"), PipelineRevision.new("current", 1, "current-1"))
    pipeline_dependency_node = PipelineDependencyNode.new(CaseInsensitiveString.new("p1"), "p1")
    pipeline_dependency_node.setCanEdit(true)
    vsm.addUpstreamNode(pipeline_dependency_node, PipelineRevision.new("p1", 1, "p1-1"), CaseInsensitiveString.new("current"))
    vsm.addUpstreamMaterialNode(SCMDependencyNode.new("git", "git", "Git"), CaseInsensitiveString.new("git1"), CaseInsensitiveString.new("p1"), material_revision)
    vsm.addUpstreamMaterialNode(SCMDependencyNode.new("git", "git", "Git"), CaseInsensitiveString.new("git2"), CaseInsensitiveString.new("current"), material_revision)
    noop_proc = proc {}

    graph_model = ValueStreamMapModel.new(vsm.presentationModel(), nil, noop_proc, noop_proc, noop_proc, proc {|pipeline_name| "/edit/#{pipeline_name}"})
    materialNames = Array.new
    materialNames << "git1"
    materialNames << "git2"

    expect(graph_model.current_pipeline).to eq("current")
    expect(graph_model.current_material).to eq(nil)

    expect(graph_model.levels.size).to eq(3)
    expect(graph_model.levels[0].nodes.size).to eq(1)
    expect(graph_model.levels[1].nodes.size).to eq(2)
    expect(graph_model.levels[2].nodes.size).to eq(1)

    nodeInThirdLevel = graph_model.levels[0].nodes[0]
    expect(nodeInThirdLevel.id).to eq("git")
    expect(nodeInThirdLevel.node_type).to eq("GIT")
    expect(nodeInThirdLevel.material_names).to eq(materialNames)
    expect(nodeInThirdLevel.locator).to eq("")
    expect(nodeInThirdLevel.dependents[0]).to eq("p1")
    expect(nodeInThirdLevel.dependents.size).to eq(2)
    expect(nodeInThirdLevel.parents.size).to eq(0)
    expect(nodeInThirdLevel.depth).to eq(1)

    node1InSecondLevel = graph_model.levels[1].nodes[0]
    expect(node1InSecondLevel.id).to eq("p1")
    expect(node1InSecondLevel.node_type).to eq(DependencyNodeType::PIPELINE.to_s)
    expect(node1InSecondLevel.locator).to eq("/go/pipeline/activity/p1")
    expect(node1InSecondLevel.dependents[0]).to eq("current")
    expect(node1InSecondLevel.parents[0]).to eq("git")
    expect(node1InSecondLevel.depth).to eq(1)
    expect(node1InSecondLevel.edit_path).to eq('/edit/p1')
    expect(node1InSecondLevel.can_edit).to eq(true)

    node2InSecondLevel = graph_model.levels[1].nodes[1]
    expect(node2InSecondLevel.node_type).to eq(DependencyNodeType::DUMMY.to_s)
    expect(node2InSecondLevel.locator).to eq("")
    expect(node2InSecondLevel.name.start_with?("dummy")).to eq(true)
    expect(node2InSecondLevel.dependents[0]).to eq("current")
    expect(node2InSecondLevel.parents[0]).to eq("git")
    expect(node2InSecondLevel.depth).to eq(2)

    nodeInFirstLevel = graph_model.levels[2].nodes[0]
    expect(nodeInFirstLevel.id).to eq("current")
    expect(nodeInFirstLevel.node_type).to eq(DependencyNodeType::PIPELINE.to_s)
    expect(nodeInFirstLevel.locator).to eq("/go/pipeline/activity/current")
    expect(nodeInFirstLevel.dependents.size).to eq(0)
    expect(nodeInFirstLevel.parents[0]).to eq("p1")
    expect(nodeInFirstLevel.depth).to eq(1)
  end

  it "should set error on model if result was not successful" do
    graph_model = ValueStreamMapModel.new(nil, "error message")
    expect(graph_model.current_pipeline).to eq(nil)
    expect(graph_model.levels).to eq(nil)
    expect(graph_model.error).to eq("error message")
  end

  it "should initialize graph model with instances for pipelines" do
    # git -> p1 --->p2---> current ---> p3
    #        |            ^
    #        |            |
    #        -------X-----

    revision_p1_1 = PipelineRevision.new("p1", 1, "label-p1-1")
    revision_p1_1.addStages(Stages.new([StageMother.passedStageInstance("stage-1-for-p1-1", "j1", "p1"), StageMother.passedStageInstance("stage-2-for-p1-1", "j2", "p1")]))

    revision_p1_2 = PipelineRevision.new("p1", 2, "label-p1-2")
    revision_p1_2.addStages(Stages.new([StageMother.passedStageInstance("stage-1-for-p1-2", "j1", "p1")]))

    revision_p2_1 = PipelineRevision.new("p2", 1, "label-p2-1")
    revision_p2_1.addStages(Stages.new([StageMother.passedStageInstance("stage-1-for-p2-1", "j1", "p2"), StageMother.unrunStage("unrun_stage")]))

    revision_p3_1 = UnrunPipelineRevision.new("p3")
    revision_p3_1.addStages(Stages.new([StageMother.unrunStage("unrun_stage1"), StageMother.unrunStage("unrun_stage2")]))

    vsm = ValueStreamMap.new(CaseInsensitiveString.new("current"), nil)
    vsm.addUpstreamNode(PipelineDependencyNode.new(CaseInsensitiveString.new("p1"), "p1"), revision_p1_1, CaseInsensitiveString.new("current"))
    vsm.addUpstreamNode(PipelineDependencyNode.new(CaseInsensitiveString.new("p2"), "p2"), revision_p2_1, CaseInsensitiveString.new("current"))
    vsm.addUpstreamNode(PipelineDependencyNode.new(CaseInsensitiveString.new("p1"), "p1"), revision_p1_2, CaseInsensitiveString.new("p2"))
    modifications = modifications()
    vsm.addUpstreamMaterialNode(SCMDependencyNode.new("git", "git", "Git"), com.thoughtworks.go.config.CaseInsensitiveString.new("git-trunk"), CaseInsensitiveString.new("p1"), material_revision)
    p3_node = vsm.addDownstreamNode(PipelineDependencyNode.new(CaseInsensitiveString.new("p3"), "p3"), CaseInsensitiveString.new("current"));
    p3_node.addRevision(revision_p3_1);

    vsm_path_partial = proc do |pipeline_name, counter|
      "some/path/to/#{pipeline_name}/#{counter}"
    end
    vsm_material_path_partial = proc do |fingerprint, revision|
      "some/path/to/#{fingerprint}/#{revision}"
    end
    stage_detail_path_partial = proc do |pipeline_name, counter, stage_name, stage_counter|
      "path/to/stage/#{pipeline_name}/#{counter}/#{stage_name}/#{stage_counter}"
    end
    graph_model = ValueStreamMapModel.new(vsm.presentationModel(), nil, vsm_path_partial, vsm_material_path_partial, stage_detail_path_partial)

    nodeThatTheGraphIsBuiltFor = graph_model.levels[3].nodes[0]
    expect(nodeThatTheGraphIsBuiltFor.id).to eq("current")

    nodeP1 = graph_model.levels[1].nodes[0]
    expect(nodeP1.id).to eq("p1")
    expect(nodeP1.instances.size).to eq(2)
    expect(nodeP1.instances[0].label).to eq("label-p1-2")
    expect(nodeP1.instances[0].counter).to eq(2)
    expect(nodeP1.instances[0].locator).to eq("some/path/to/p1/2")
    expect(nodeP1.instances[0].stages.size).to eq(1)
    expect(nodeP1.instances[0].stages[0].name).to eq("stage-1-for-p1-2")
    expect(nodeP1.instances[0].stages[0].duration).to eq(0)
    expect(nodeP1.instances[0].stages[0].status).to eq("Passed")

    expect(nodeP1.instances[1].label).to eq("label-p1-1")
    expect(nodeP1.instances[1].counter).to eq(1)
    expect(nodeP1.instances[1].locator).to eq("some/path/to/p1/1")
    expect(nodeP1.instances[1].stages.size).to eq(2)
    expect(nodeP1.instances[1].stages[0].name).to eq("stage-1-for-p1-1")
    expect(nodeP1.instances[1].stages[0].status).to eq("Passed")
    expect(nodeP1.instances[1].stages[0].locator).to eq("path/to/stage/p1/1/stage-1-for-p1-1/1")
    expect(nodeP1.instances[1].stages[1].name).to eq("stage-2-for-p1-1")
    expect(nodeP1.instances[1].stages[1].status).to eq("Passed")
    expect(nodeP1.instances[1].stages[1].locator).to eq("path/to/stage/p1/1/stage-2-for-p1-1/1")

    nodeP2 = graph_model.levels[2].nodes[0]
    expect(nodeP2.id).to eq("p2")
    expect(nodeP2.instances.size).to eq(1)
    expect(nodeP2.instances[0].label).to eq("label-p2-1")
    expect(nodeP2.instances[0].counter).to eq(1)
    expect(nodeP2.instances[0].locator).to eq("some/path/to/p2/1")
    expect(nodeP2.instances[0].stages.size).to eq(2)
    expect(nodeP2.instances[0].stages[0].name).to eq("stage-1-for-p2-1")
    expect(nodeP2.instances[0].stages[0].status).to eq("Passed")
    expect(nodeP2.instances[0].stages[0].locator).to eq("path/to/stage/p2/1/stage-1-for-p2-1/1")
    expect(nodeP2.instances[0].stages[1].name).to eq("unrun_stage")
    expect(nodeP2.instances[0].stages[1].status).to eq("Unknown")
    expect(nodeP2.instances[0].stages[1].locator).to eq("")

    nodeP3 = graph_model.levels[4].nodes[0]
    expect(nodeP3.id).to eq("p3")
    expect(nodeP3.instances.size).to eq(1)
    expect(nodeP3.instances[0].label).to eq("")
    expect(nodeP3.instances[0].counter).to eq(0)
    expect(nodeP3.instances[0].locator).to eq("")
    expect(nodeP3.instances[0].stages[0].name).to eq("unrun_stage1")
    expect(nodeP3.instances[0].stages[0].status).to eq("Unknown")
    expect(nodeP3.instances[0].stages[0].locator).to eq("")
    expect(nodeP3.instances[0].stages[1].name).to eq("unrun_stage2")
    expect(nodeP3.instances[0].stages[1].status).to eq("Unknown")
    expect(nodeP3.instances[0].stages[1].locator).to eq("")


    nodeDummy = graph_model.levels[2].nodes[1]
    expect(nodeDummy.node_type).to eq(DependencyNodeType::DUMMY.to_s)
    expect(nodeDummy.instances.size).to eq(0)

    nodeGit = graph_model.levels[0].nodes[0]
    expect(nodeGit.id).to eq("git")
    expect(nodeGit.node_type).to eq("GIT")
    expect(nodeGit.instances.size).to eq(0)
    expect(nodeGit.material_revisions.size).to eq(1)
    expect(nodeGit.material_revisions[0].modifications.size).to eq(1)
    git_instance = nodeGit.material_revisions[0].modifications[0]
    modification = modifications.get(0)
    expect(git_instance.revision).to eq(modification.getRevision())
    expect(git_instance.locator).to eq("some/path/to/git/r1")
  end


  it "should populate message & locator correctly" do
    # git -> p1 --->p2---> current ---> p3
    #        |            ^
    #        |            |
    #        -------X-----

    revision_p1_1 = PipelineRevision.new("p1", 1, "label-p1-1")
    revision_p1_1.addStages(Stages.new([StageMother.passedStageInstance("stage-1-for-p1-1", "j1", "p1"), StageMother.passedStageInstance("stage-2-for-p1-1", "j2", "p1")]))

    revision_p2_1 = PipelineRevision.new("p2", 1, "label-p2-1")
    revision_p2_1.addStages(Stages.new([StageMother.passedStageInstance("stage-1-for-p2-1", "j1", "p2"), StageMother.unrunStage("unrun_stage")]))

    vsm = ValueStreamMap.new(CaseInsensitiveString.new("current"), nil)
    vsm.addUpstreamNode(PipelineDependencyNode.new(CaseInsensitiveString.new("p1"), "p1"), revision_p1_1, CaseInsensitiveString.new("current"))
    vsm.addUpstreamNode(PipelineDependencyNode.new(CaseInsensitiveString.new("p2"), "p2"), revision_p2_1, CaseInsensitiveString.new("current"))
    vsm.addUpstreamNode(PipelineDependencyNode.new(CaseInsensitiveString.new("p1"), "p1"), revision_p1_1, CaseInsensitiveString.new("p2"))
    vsm.addUpstreamMaterialNode(SCMDependencyNode.new("git", "git", "Git"), com.thoughtworks.go.config.CaseInsensitiveString.new("git-trunk"), CaseInsensitiveString.new("p1"), material_revision)

    p3_node = vsm.addDownstreamNode(PipelineDependencyNode.new(CaseInsensitiveString.new("p3"), "p3"), CaseInsensitiveString.new("current"));
    p3_node.setViewType(com.thoughtworks.go.domain.valuestreammap.VSMViewType::NO_PERMISSION)
    p3_node.setMessage("You are not authorized to view this pipeline")


    vsm_path_partial = proc do |pipeline_name, counter|
      "some/path/to/#{pipeline_name}/#{counter}"
    end
    vsm_material_path_partial = proc do |fingerprint, revision|
      "some/path/to/#{fingerprint}/#{revision}"
    end
    stage_detail_path_partial = proc do |pipeline_name, counter, stage_name, stage_counter|
      "path/to/stage/#{pipeline_name}/#{counter}/#{stage_name}/#{stage_counter}"
    end
    graph_model = ValueStreamMapModel.new(vsm.presentationModel(), nil, vsm_path_partial, vsm_material_path_partial, stage_detail_path_partial)

    nodeThatTheGraphIsBuiltFor = graph_model.levels[3].nodes[0]
    expect(nodeThatTheGraphIsBuiltFor.id).to eq("current")

    nodeP1 = graph_model.levels[1].nodes[0]
    expect(nodeP1.id).to eq("p1")
    expect(nodeP1.instances.size).to eq(1)
    expect(nodeP1.instances[0].label).to eq("label-p1-1")
    expect(nodeP1.instances[0].counter).to eq(1)
    expect(nodeP1.instances[0].locator).to eq("some/path/to/p1/1")
    expect(nodeP1.instances[0].stages.size).to eq(2)
    expect(nodeP1.instances[0].stages[0].name).to eq("stage-1-for-p1-1")
    expect(nodeP1.instances[0].stages[0].duration).to eq(0)
    expect(nodeP1.instances[0].stages[0].status).to eq("Passed")

    nodeP2 = graph_model.levels[2].nodes[0]
    expect(nodeP2.id).to eq("p2")
    expect(nodeP2.instances.size).to eq(1)
    expect(nodeP2.instances[0].label).to eq("label-p2-1")
    expect(nodeP2.instances[0].counter).to eq(1)
    expect(nodeP2.instances[0].locator).to eq("some/path/to/p2/1")
    expect(nodeP2.instances[0].stages.size).to eq(2)
    expect(nodeP2.instances[0].stages[0].name).to eq("stage-1-for-p2-1")
    expect(nodeP2.instances[0].stages[0].status).to eq("Passed")
    expect(nodeP2.instances[0].stages[0].locator).to eq("path/to/stage/p2/1/stage-1-for-p2-1/1")
    expect(nodeP2.instances[0].stages[1].name).to eq("unrun_stage")
    expect(nodeP2.instances[0].stages[1].duration).to be_nil
    expect(nodeP2.instances[0].stages[1].status).to eq("Unknown")
    expect(nodeP2.instances[0].stages[1].locator).to eq("")

    nodeP3 = graph_model.levels[4].nodes[0]
    expect(nodeP3.id).to eq("p3")
    expect(nodeP3.instances.size).to eq(0)
    expect(nodeP3.view_type).to eq("NO_PERMISSION")
    expect(nodeP3.locator).to eq("")
    expect(nodeP3.message).to eq("You are not authorized to view this pipeline")

    nodeDummy = graph_model.levels[2].nodes[1]
    expect(nodeDummy.node_type).to eq(DependencyNodeType::DUMMY.to_s)
    expect(nodeDummy.instances.size).to eq(0)

    nodeGit = graph_model.levels[0].nodes[0]
    expect(nodeGit.id).to eq("git")
    expect(nodeGit.node_type).to eq("GIT")
    expect(nodeGit.material_revisions.size).to eq(1)
    expect(nodeGit.material_revisions[0].modifications.size).to eq(1)
    expect(nodeGit.material_revisions[0].modifications[0].revision).to eq("r1")
    expect(nodeGit.material_revisions[0].modifications[0].locator).to eq("some/path/to/git/r1")
  end

  it "should populate details of material_revisions with modifications" do
    # git -> current

    vsm = ValueStreamMap.new(CaseInsensitiveString.new("current"), PipelineRevision.new("current", 1, "current-1"))
    modifications = modifications()
    vsm.addUpstreamMaterialNode(SCMDependencyNode.new("git", "git", "Git"), CaseInsensitiveString.new("git1"), CaseInsensitiveString.new("current"), material_revision)
    graph_model = ValueStreamMapModel.new(vsm.presentationModel(), nil)
    git_node = graph_model.levels[0].nodes[0]

    expect(git_node.material_revisions.size).to eq(1)
    expect(git_node.material_revisions[0].modifications.size).to eq(1)

    git_instance = git_node.material_revisions[0].modifications[0]
    modification = modifications.get(0)
    expect(git_instance.revision).to eq(modification.getRevision())
    expect(git_instance.user).to eq(modification.getUserName())
    expect(git_instance.comment).to eq(modification.getComment())
    expect(git_instance.modified_time).to eq("less than a minute ago")
  end

  it "should create VSM json model for material correctly" do
    # git -> p1

    material = GitMaterial.new("url")
    modifications = modifications()
    vsm = ValueStreamMap.new(material, nil, modifications[0])
    vsm.addDownstreamNode(PipelineDependencyNode.new(CaseInsensitiveString.new("p1"), "p1"), vsm.current_material.getId())

    graph_model = ValueStreamMapModel.new(vsm.presentationModel(), nil)
    expect(graph_model.current_pipeline).to eq(nil)
    expect(graph_model.current_material).to eq(material.getFingerprint())

    git_node = graph_model.levels[0].nodes[0]
    expect(git_node.material_revisions.size).to eq(1)
    expect(git_node.material_revisions[0].modifications.size).to eq(1)

    git_instance = git_node.material_revisions[0].modifications[0]
    modification = modifications.get(0)
    expect(git_instance.revision).to eq(modification.getRevision())
    expect(git_instance.user).to eq(modification.getUserName())
    expect(git_instance.comment).to eq(modification.getComment())
    expect(git_instance.modified_time).to eq("less than a minute ago")
  end

  def modifications
    modification = com.thoughtworks.go.domain.materials.Modification.new("user", "comment", "", java.util.Date.new(), "r1")
    return com.thoughtworks.go.domain.materials.Modifications.new([modification].to_java(com.thoughtworks.go.domain.materials.Modification))
  end

  def material_revision
    return MaterialRevision.new(GitMaterial.new("url"), false, modifications)
  end
end
