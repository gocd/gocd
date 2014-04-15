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

module TaskMother

  def exec_task(command='ls')
    args = Arguments.new([Argument.new("-la")].to_java(Argument))
    exec = ExecTask.new(command, args, "hero/ka/directory")
    exec.setCancelTask(ExecTask.new("echo", "'failing'", "oncancel_working_dir"))
    exec
  end

  def fetch_task(pipeline='pipeline', stage='stage', job='job', src_file='src', dest='dest')
    fetch = FetchTask.new(CaseInsensitiveString.new(pipeline), CaseInsensitiveString.new(stage), CaseInsensitiveString.new(job), src_file, dest)
    fetch.setCancelTask(ExecTask.new("echo", "'failing'", "oncancel_working_dir"))
    fetch
  end

  def simple_exec_task
    ExecTask.new("ls", "-la", "hero/ka/directory")
  end

  def simple_exec_task_with_args_list
    args = Arguments.new([Argument.new("-l"), Argument.new("-a")].to_java(Argument))
    ExecTask.new("ls", args, "hero/ka/directory")
  end

  def with_run_if(type, task)
    task.getConditions().add(type)
    task
  end

  def ant_task file="build.xml", target="compile", working_dir="default/wd"
    ant = AntTask.new
    ant.setTarget(target)
    ant.setBuildFile(file)
    ant.setWorkingDirectory(working_dir)
    ant
    end

  def nant_task file="default.build", target="compile", working_dir="default/wd"
    nant = NantTask.new
    nant.setTarget(target)
    nant.setBuildFile(file)
    nant.setWorkingDirectory(working_dir)
    nant
  end

  def rake_task file="rakefile", target="default", working_dir="default/wd"
    rake = RakeTask.new
    rake.setTarget(target)
    rake.setBuildFile(file)
    rake.setWorkingDirectory(working_dir)
    rake
  end

  def plugin_task plugin_id = "curl.plugin", configurations = []
    configuration = Configuration.new(configurations.to_java(ConfigurationProperty))
    pluggable_task_new = PluggableTask.new("", PluginConfiguration.new(plugin_id, "1.0"), configuration)
    pluggable_task_new
  end

  def simple_task_plugin_with_on_cancel_config
    task_plugin = plugin_task
    task_plugin.setCancelTask(simple_exec_task)
    task_plugin
  end

  def vm_for task
    Spring.bean("taskViewService").getViewModel(task, 'new')
  end
end