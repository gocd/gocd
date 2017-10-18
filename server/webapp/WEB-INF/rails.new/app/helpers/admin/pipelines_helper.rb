module Admin
  module PipelinesHelper
    include JavaImports

    def default_stage_config
      job_configs = JobConfigs.new([JobConfig.new(CaseInsensitiveString.new("defaultJob"), Resources.new, ArtifactPlans.new, com.thoughtworks.go.config.Tasks.new([AntTask.new].to_java(Task)))].to_java(JobConfig))
      StageConfig.new(CaseInsensitiveString.new("defaultStage"), job_configs)
    end
  end
end
