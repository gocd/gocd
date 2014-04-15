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

OneJarConfig = Struct.new(:main, :include_libs, :go_agent_bootstrap_class, :ignore_libs)

ONE_JAR_MAINS = {
        'agent-bootstrapper.jar' => OneJarConfig.new('com.thoughtworks.go.agent.bootstrapper.AgentBootstrapper', true),
        'agent.jar' => OneJarConfig.new('com.thoughtworks.go.agent.AgentMain', true, "com.thoughtworks.go.agent.AgentProcessParentImpl", ['agent-launcher.jar', 'tfs-impl-classes.jar']),
        'go.jar' => OneJarConfig.new('com.thoughtworks.go.server.util.GoLauncher', false),
        'test-agent.jar' => OneJarConfig.new('com.thoughtworks.go.ArgPrintingMain', true, "com.thoughtworks.go.HelloWorldStreamWriter", ['agent-launcher.jar'])
}

def in_classpath_form deps
  deps.map { |f| File.join(".", "lib", File.basename(f.to_s)) }.join(" ")
end

def onejar jarname
  package(:jar, :file => _(:target, 'main.jar')).with(:manifest => manifest.merge("Class-Path" => in_classpath_form(compile.dependencies)))

  one_jar = package(:onejar, :file => jarname)
  self.main_jars << one_jar
  one_jar
end

def package_as_onejar file_name
  Buildr::ZipTask.define_task(file_name).clean.enhance do |zip|
    tmp_file_name = file_name + '.tmp'

    config = ONE_JAR_MAINS[File.basename(file_name)]

    File.open(_(:target, 'MANIFEST.MF'), 'w') do |f|
      f << Buildr::Packaging::Java::Manifest.new(manifest).to_s
      f << "One-Jar-Main-Class: " + config.main
      if (config.go_agent_bootstrap_class)
        f << "\nGo-Agent-Bootstrap-Class: " + config.go_agent_bootstrap_class
      end
    end

    ant('onejar') do |proj|
      proj.taskdef :name => 'one_jar',
                   :classname => 'com.simontuffs.onejar.ant.OneJarTask',
                   :classpath => jars_at('one-jar-ant-task').join(File::PATH_SEPARATOR)

      proj.one_jar(:destfile => tmp_file_name, :manifest => _(:target, 'MANIFEST.MF')) do
        proj.main :jar => _(:target, "main.jar")
        if config.go_agent_bootstrap_class
          proj.boot do
            proj.zipfileset(:file => _(:target, 'bootstrap_classes.jar'))
          end
        end
        config[:include_libs] && proj.lib do
          ignore_libs = config[:ignore_libs]
          compile.dependencies.each do |dep|
            unless ignore_libs && ignore_libs.include?(File.basename(dep.to_s))
              proj.fileset :file => dep
            end
          end
        end
      end
    end

    zip.merge(tmp_file_name)
    zip.enhance do
      rm tmp_file_name
    end
  end
end

def include_fileset_from_target(jar, module_name, pattern, target_dir = :classes)
  search_under = module_name + "/target/#{target_dir}"
  do_with_fileset_under(:include, jar, pattern, search_under)
end

def exclude_fileset_from_target(jar, module_name, pattern)
  search_under = module_name + '/target/classes'
  do_with_fileset_under(:exclude, jar, pattern, search_under)
end

def include_fileset(jar, project, pattern, target_dir = :classes)
  do_with_fileset(:include, jar, project, pattern, target_dir)
end

def exclude_fileset(jar, project, pattern)
  do_with_fileset(:exclude, jar, project, pattern)
end

def do_with_fileset(action, jar, project, pattern, target_dir = :classes)
  search_under = project._(:target, :main, target_dir)
  do_with_fileset_under(action, jar, pattern, search_under)
end

def do_with_fileset_under(action, jar, pattern, search_under)
  base_for_search = File.join($PROJECT_BASE, search_under)
  pattern_to_match = File.join(base_for_search, pattern)
  available_files = Dir.glob(pattern_to_match)
  raise "Did not find even one file with pattern: #{pattern_to_match}" if available_files.length <= 0

  available_files.each do |abs_path|
    jar.path(File.dirname(abs_path.gsub(/^#{base_for_search}\//, ''))).send(action, abs_path)
  end
end