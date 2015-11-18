# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.

def workspace_dir
  "#{File.expand_path(File.join(File.dirname(__FILE__), ".."))}"
end

desc 'Create JRuby all-in-one distribution'
task 'all-in-one' => 'all-in-one:all-in-one'

namespace 'all-in-one' do

  version = '1.7.0'
  jruby_distro = "jruby-bin-#{version}.tar.gz"
  url = "http://jruby.org.s3.amazonaws.com/downloads/#{version}/#{jruby_distro}"
  dir = "jruby-#{version}"

  task 'all-in-one' => %w(gem prepare download_and_extract install_dependencies clean_dist package)

  desc 'Prepare to run'
  task 'prepare' do
    mkpath '_all-in-one'
    cd '_all-in-one'
  end

  desc 'Download and extract JRuby'
  task 'download_and_extract' do
    unless File.exist?(jruby_distro)
      puts "Downloading JRuby from #{url} ..."
      require 'open-uri'
      File.open(jruby_distro, "wb") do |saved_file|
        # the following "open" is provided by open-uri
        open(url) do |read_file|
          saved_file.write(read_file.read)
        end
      end
      puts '[X] Downloaded JRuby'
    end

    rm_rf dir if File.exist? dir

    puts "Extracting JRuby to #{dir} ..."
    sh 'tar', 'xzf', jruby_distro
    puts '[X] Extracted JRuby'
    cd dir
  end

  desc 'Cleanup JRuby distribution'
  task 'clean_dist' do
    puts 'Cleaning...'
    mv 'tool/nailgun/ng.exe', 'bin'
    rm_rf 'tool'
    rm_rf 'docs'
    rm_rf 'lib/ruby/1.8'
    rm_rf 'lib/ruby/gems/1.9/doc'
    rm_rf 'lib/ruby/gems/shared/doc'
    rm_rf 'samples'
  end

  desc 'Install Buildr gem and dependencies'
  task 'install_dependencies' do
    puts 'Install Buildr gem ...'
    java_gem = FileList["../../pkg/buildr-#{spec.version}-java.gem"].first
    command = ['bin/jruby', '-S', 'gem', 'install', java_gem, '--no-rdoc', '--no-ri', '--env-shebang']
    system({'GEM_HOME' => nil, 'GEM_PATH' => nil, 'MY_RUBY_HOME' => nil, 'RUBYOPT' => nil}, *command)
    puts '[X] Install Buildr gem'
  end

  desc 'Package distribution'
  task 'package' do
    pkg_dir = "#{workspace_dir}/pkg"
    mkpath pkg_dir
    puts 'Zipping distribution ...'
    cd '..'
    new_dir  = "#{spec.name}-all-in-one-#{spec.version}"
    rm_rf new_dir
    mkdir new_dir
    mv dir, "#{new_dir}/embedded"
    mkdir "#{new_dir}/bin"
    cp "#{workspace_dir}/all-in-one/buildr", "#{new_dir}/bin/buildr"
    cp "#{workspace_dir}/all-in-one/buildr.cmd", "#{new_dir}/bin/buildr.cmd"
    File.chmod(0500, "#{new_dir}/bin/buildr", "#{new_dir}/bin/buildr.cmd")
    zip = "#{pkg_dir}/#{new_dir}.zip"
    rm zip if File.exist? zip
    sh 'zip', '-q', '-r', zip, new_dir
    puts '[X] Zipped distribution'

    puts 'Tarring distribution ...'
    tar = "#{pkg_dir}/#{new_dir}.tar.gz"
    rm tar if File.exist? tar
    sh 'tar', 'czf', tar, new_dir
    puts '[X] Tarred distribution'

    rm_rf new_dir
  end

end

task('clobber') { rm_rf '_all-in-one' }
