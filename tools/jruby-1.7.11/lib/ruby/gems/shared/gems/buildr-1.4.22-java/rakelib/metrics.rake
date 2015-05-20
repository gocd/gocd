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

namespace 'metrics' do
  desc 'run Saikuro reports'
  task 'saikuro' do
    gem 'atoulme-Saikuro'
    require 'saikuro'
    require 'pathname'
    output_dir = File.expand_path(File.join(File.dirname(__FILE__), '..', '_reports', 'saikuro'))
    base_dir = Pathname.new(File.expand_path(File.join(File.dirname(__FILE__), '..')))
    rb_files = %w(lib addon).collect { |folder|
      FileList[File.expand_path(File.join(File.dirname(__FILE__), '..', folder, '**', '*.rb'))]
    }.flatten.collect {|path|
      Pathname.new(path).relative_path_from(base_dir).to_s
    }
    SaikuroRunner.new.run(rb_files, output_dir)
  end

  desc 'generate ccn treemap'
  task 'ccn_treemap' do
    require 'saikuro_treemap'
    SaikuroTreemap.generate_treemap :code_dirs => %w(lib addon), :output_file => '_reports/saikuro_treemap.html'
  end
end

desc 'Run all metrics tools'
task 'metrics' => %w(metrics:saikuro metrics:ccn_treemap)
