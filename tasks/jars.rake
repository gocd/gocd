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

def jar_at(foldername)
  Dir.glob($PROJECT_BASE + '/localivy/' + foldername + '/*.jar').delete_if do |file_name|
    (file_name =~ /-sources\.jar$/) || (file_name =~ /-src\.jar$/)
  end
end

def jars_at(*args)
  args.map { |a| jar_at a }.flatten
end

def jars_with_abs_path(*args)
  jars_at(*args).map {|a| _(File.expand_path(a)) }
end

def tw_go_jar(module_name, jar_name = module_name)
  $PROJECT_BASE + '/' + module_name + '/target/' + jar_name + '.jar'
end

def server_launcher_dependencies
  jars = Dir.glob($PROJECT_BASE + '/server-launcher/target/libs/*.jar')
  jars
end
def jetty_jars
  Dir.glob($PROJECT_BASE + '/jetty9/target/lib/*.jar')
end

def maven_dependency(groupid, artifactid, version, maven_repository = File.expand_path('~') + '/.m2/repository')
  groupid.gsub!('.', '/')
  path = maven_repository + '/' + groupid + '/' + artifactid + '/' + version + '/*.jar'
  jars = Dir.glob(path)
  jars
end

def local_maven_dependency(groupid, artifactid, version)
  jars = maven_dependency(groupid, artifactid, version, $PROJECT_BASE + '/local-maven-repo')
  jars
end