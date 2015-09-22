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

require 'securerandom'
require 'rubygems'

GO_ROOT = File.expand_path('../../', __FILE__)
SERVER_MODULE_ROOT = File.join(GO_ROOT, 'server')

# prepare webapp
VERSION_NUMBER = ENV["VERSION_NUMBER"]
RELEASE_COMMIT = ENV["RELEASE_COMMIT"]

task :prepare_webapp do
  if ENV['SKIP_WAR'] == 'Y'
    safe_cp "test-resources/web.xml", "target/webapp/WEB-INF"
    puts "INFO: skipping war packaging"
    next
  end

  #copy
  task('copy_files').invoke

  task('handle_assets').invoke

  # rails
  task('copy-code-to-be-interpolated').invoke
  task('keep-only-prod-gems').invoke
  task('inline-rails-partials').invoke
  task('copy_inlined_erbs_to_webapp').invoke

  #prepare for production mode
  task('write_revision_number').invoke
  task('change_rails_env_to_production').invoke
end

desc "erase any non-prod gems from the final distribution"
task 'keep-only-prod-gems' do
  require 'pathname'

  $stderr.puts "*** Cleaning up any gems not needed in production environment"
  gemfile = Pathname.new("target/webapp/WEB-INF/rails.new/Gemfile").expand_path
  root = gemfile.dirname
  puts "*** Using gemfile #{gemfile}"

  cd root do
    require 'bundler'
    definition = ::Bundler.definition
    all = definition.specs.to_a
    puts "*** All gems - #{all.collect(&:full_name)}"
    requested = definition.specs_for(definition.groups.collect(&:to_sym) - [:development, :test, :assets]).to_a
    puts "*** Requested gems - #{requested.collect(&:full_name)}"
    ignored_gems = all - requested
    puts "*** Ignored gems - #{ignored_gems.collect(&:full_name)}"

    gem_dirs_to_remove = Dir["vendor/bundle/jruby/1.9/gems/{#{ignored_gems.collect(&:full_name).join(',')}}"]
    spec_files_to_remove = Dir["vendor/bundle/jruby/1.9/specifications/{#{ignored_gems.collect(&:full_name).join(',')}}.gemspec"]

    (gem_dirs_to_remove + spec_files_to_remove).each do |file|
      rm_rf file
    end
  end
end

task :handle_assets_rails4 do
  rm_rf("target/webapp/WEB-INF/rails.new/tmp")
  task('precompile_assets').invoke
  assets_location_in_target = "target/webapp/WEB-INF/rails.new/public/assets"
  rm_rf assets_location_in_target if File.exist? assets_location_in_target
  cp_r "webapp/WEB-INF/rails.new/public/assets", "target/webapp/WEB-INF/rails.new/public/"
  rm_rf "target/webapp/WEB-INF/rails.new/app/assets"
end

task :handle_assets do
  task('handle_assets_rails4').invoke
end

#copy
ADMIN_VERSION_FILE = "target/webapp/WEB-INF/vm/admin/admin_version.txt.vm"
CRUISE_VERSION_FILE = "target/webapp/WEB-INF/classes/ui/navigation/cruise_version.st"

task :copy_files do
  safe_cp "webapp", "target"

  FileUtils.remove_dir("target/webapp/WEB-INF/rails.new/spec", true)

  cp "messages/message.properties", "target/webapp"
  cp "../config/config-server/resources/cruise-config.xsd", "target/webapp"

end

task :write_revision_number do
  mkdir_p "target/webapp/WEB-INF/classes/ui/navigation"

  {ADMIN_VERSION_FILE => "%s(%s)", CRUISE_VERSION_FILE => "%s (%s)"}.each_pair do |path, template|
    File.open(path, "w") { |h| h.write(template % [VERSION_NUMBER, RELEASE_COMMIT]) }
  end
end

def create_pathing_jar classpath_file
  pathing_jar = File.expand_path(File.join(File.dirname(classpath_file), "pathing.jar"))
  manifest_file = File.expand_path(File.join(File.dirname(classpath_file), "MANIFEST.MF"))
  rm manifest_file if File.exists? manifest_file
  rm pathing_jar if File.exist? pathing_jar
  classpath_contents = File.read(classpath_file).split(File::PATH_SEPARATOR)
  to_be_written = ''
  classpath_contents.each_with_index do |entry, i|
    to_be_written += "\r\n \\" + entry + '\\ ' if File.directory? entry
    to_be_written += "\r\n \\" + entry + ' ' unless File.directory? entry
  end
  File.open(manifest_file, 'w') do |f|
    f.write("Class-Path: #{to_be_written}")
    f.write("\r\n")
  end
  raise "File not found: #{manifest_file}" unless File.exists? manifest_file
  sh "jar cmf #{manifest_file} #{pathing_jar}"
  pathing_jar
end

def classpath
  dependencies_for_test_file = File.join(SERVER_MODULE_ROOT, 'target/server-test-dependencies')

  cd SERVER_MODULE_ROOT do
    sh("mvn dependency:build-classpath -Dmdep.outputFile=#{dependencies_for_test_file}")
    File.read(dependencies_for_test_file).tap do |deps|
      new_deps = [
        File.join(SERVER_MODULE_ROOT, 'target/classes'),
        File.join(SERVER_MODULE_ROOT, 'target/test-classes'),
        deps.strip
      ].join(File::PATH_SEPARATOR)
      File.open(dependencies_for_test_file, 'w') {|f| f.puts new_deps}
    end
  end

  if Gem.win_platform?
    create_pathing_jar dependencies_for_test_file
  else
    File.read(dependencies_for_test_file)
  end
end

def ruby_executable
  File.expand_path(File.join(File.dirname(__FILE__), "..", "tools", "bin", (Gem.win_platform? ? 'jruby.bat' : 'jruby')))
end

def rails_root(*path)
  File.expand_path(File.join(File.dirname(__FILE__), "webapp", "WEB-INF", "rails.new", *path))
end

def sh_with_environment(cmd, env={})
  original_env = ENV.clone.to_hash
  begin
    export_or_set = Gem.win_platform? ? 'set' : 'export'
    env.each do |k, v|
      $stderr.puts "#{export_or_set} #{k}=#{v[0..100]}"
    end

    ENV.replace(ENV.clone.to_hash.merge(env))
    sh(cmd)
  ensure
    ENV.replace(original_env)
  end
end


task :precompile_assets do
  cd rails_root do
    sh_with_environment("#{ruby_executable} -S ./bin/rake --trace assets:clobber assets:precompile", {'RAILS_ENV' => 'production', 'CLASSPATH' => classpath})
  end
end

task :jasmine_tests do
  cd rails_root do
    environment = {
      'RAILS_ENV'           => 'test',
      'REPORTERS'           => 'console,junit',
      'CLASSPATH'           => classpath
    }
    sh_with_environment("#{ruby_executable} -S ./bin/rake jasmine:ci", environment.merge('JASMINE_CONFIG_PATH' => './spec/javascripts/support/jasmine-ci-old.yml'))
    sh_with_environment("#{ruby_executable} -S ./bin/rake jasmine:ci", environment.merge("JASMINE_CONFIG_PATH" => './spec/javascripts/support/jasmine-ci-new.yml', 'REQUIRE_JS' => 'true'))
  end
end

# inline partials
RAILS_DIR = "rails.new"
RAILS_ROOT = "target/webapp/WEB-INF/" + RAILS_DIR
RAILS_VIEWS_SRC = RAILS_ROOT + "/app/views"
RAILS_INTERPOLATED_VIEWS = "target/rails_views/views"

def inline_partials dir = RAILS_INTERPOLATED_VIEWS
  Dir[File.join(dir, "*")].each do |path|
    File.directory?(path) ? inline_partials(path) : inline_partial(path)
  end
end

def erb_ruby call
  return call.scan(/\A<%=\s*(.+?)\s*-?%>\Z/m).flatten[0]
end

def args_of_fn fn_name, call
  ruby_code = erb_ruby(call)
  call_matcher_with_paren = /\A\s*#{fn_name}\s*\(/m
  arg_match = (ruby_code =~ call_matcher_with_paren) ? ruby_code.scan(/#{call_matcher_with_paren}(.*?);?\s*\);?\s*\Z/m) : ruby_code.scan(/\A\s*#{fn_name}\s*(.*?)\s*;?\s*\Z/m)
  arg_match.flatten[0]
end

def render_args call
  args_of_fn('render', call)
end

def locals_hash file, call
  render_arguments = render_args(call)
  scope_map = render_arguments.scan(/:locals\s*=>\s*\{\s*:scope\s*=>\s*(.*?)\s*\}[\s)]*\Z/m)
  locals_hash_value = scope_map.flatten[0]

  parser_says_there_are_no_locals = locals_hash_value.nil?
  probably_has_locals = render_arguments =~ /locals.*scope/
  scope_is_not_empty = render_arguments !~ /:locals\s*=>\s*:scope\s*=>\s*\{\s*\}/
  if parser_says_there_are_no_locals && probably_has_locals && scope_is_not_empty
    raise "ERROR: Inlining partials of #{file}. Parser says there are no locals here: #{call}"
  end

  locals_hash_value
end

def inline_partial_render_calls file, depth
  inline_render_calls(File.read(file), file, /<%=\s*render[\s(]*\:partial.*?%>/m, depth)
end

def inline_render_calls buffer, file, scanned_by, depth
  render_partial_calls = buffer.scan(scanned_by)
  render_partial_calls.each do |call|
    ruby_code = erb_ruby(call)
    local_hash = locals_hash(file, call)
    partial_path = ruby_code.scan(/\:partial\s*=>\s*("|')(.+?)\1/m).flatten[1] #make me locals style
    partial_path = (partial_path =~ /\//m ? File.join(RAILS_INTERPOLATED_VIEWS, File.dirname(partial_path), "_" + File.basename(partial_path)) : File.join(File.dirname(file), "_" + File.basename(partial_path)))
    actual_partial_path = actual_partial_path(partial_path)
    content = inline_partial(actual_partial_path, depth + 1)
    if local_hash
      prefix = "_"*depth
      content = "<% #{prefix}scope = #{local_hash}; -%>" + fix_scope_calls(content, prefix)
    end
    if ENV['MARK_PARTIAL_BOUNDARIES'] == 'true'
      content = "<!-- Start #{ruby_code} -->\n#{content}\n<!-- end #{ruby_code} -->"
    end
    buffer.sub!(call, block_given? ? yield(content) : content)
  end
  buffer
end

def actual_partial_path partial_path
  glob = Dir.glob(partial_path)
  if glob.empty?
    glob = Dir.glob(partial_path + ".*")
    raise "cannot find partial: #{partial_path}" if glob.empty?
  end
  glob[0]
end

def inline_json_render_calls buffer, file, depth
  buffer = inline_render_calls(buffer, file, /<%=\s*render_json[\s(]*\:partial.*?%>/m, depth) do |sub_buffer|
    placeholder_map = {}
    sub_buffer.scan(/<%.*?%>/m).flatten.each do |match_ruby|
      key = SecureRandom.uuid
      placeholder_map[key] = match_ruby
      sub_buffer.gsub!(match_ruby, key)
    end
    json_escaped_buffer = ActiveSupport::JSON::Encoding.escape(sub_buffer)
    placeholder_map.each_pair do |id, matched_ruby_exp|
      needs_escaping = (matched_ruby_exp =~ /\A<%=/) && args_of_fn('yield', matched_ruby_exp).nil?
      json_escaped_buffer.gsub!(id, (needs_escaping ? "<%= json_escape((#{erb_ruby(matched_ruby_exp)})) -%>" : matched_ruby_exp)) # f_call("value" if predicate) is not valid ruby grammer, f_call(("value" if predicate)) is.
    end
    json_escaped_buffer
  end
end

def inline_partial file, depth = 0
  return unless File.extname(file) == ".erb"
  buffer = inline_partial_render_calls(file, depth)
  buffer = inline_json_render_calls(buffer, file, depth)
  unless File.basename(file) =~ /^_/
    File.open(file, 'w') do |fd|
      fd.write(buffer)
    end
  end
  buffer
end

def fix_scope_calls content, prefix
  content.gsub(/([^_])scope\[:/, "\\1#{prefix}scope[:")
end

def safe_cp from, to
  mkdir_p to
  cp_r from, to
end

task "copy-code-to-be-interpolated" do
  safe_cp RAILS_VIEWS_SRC, File.dirname(RAILS_INTERPOLATED_VIEWS)
end

task "inline-rails-partials" do
  inline_partials
end

task :copy_inlined_erbs_to_webapp do
  FileUtils.remove_dir(RAILS_VIEWS_SRC, true)
  safe_cp RAILS_INTERPOLATED_VIEWS, RAILS_ROOT + "/app"
end

# misc
class NotInProduction
  def initialize prefix, dir_file_mapping
    @prefix = prefix
    @dir_file_mapping = dir_file_mapping
  end

  def each
    (ENV['ALLOW_NON_PRODUCTION_CODE'] == 'yes') && return
    @dir_file_mapping.each do |dir, files|
      Array(files).each do |file|
        yield(File.join(@prefix, dir), dir, file)
      end
    end
  end
end

SANITIZE_FOR_PRODUCTION = NotInProduction.new('target/webapp', {"WEB-INF/#{RAILS_DIR}/config" => 'routes.rb', "WEB-INF/#{RAILS_DIR}/app/controllers/admin" => ['users_controller.rb'], "WEB-INF/#{RAILS_DIR}/app/controllers/admin" => 'backup_controller.rb'})

task :change_rails_env_to_production do
  replace_content_in_file("target/webapp/WEB-INF/web.xml", "<param-value>development</param-value>", "<param-value>production</param-value>")
  replace_content_in_file("target/webapp/WEB-INF/applicationContext-acegi-security.xml", /^.*_not_in_production.*$/, '' ) unless ENV['ALLOW_NON_PRODUCTION_CODE'] == 'yes'

  SANITIZE_FOR_PRODUCTION.each do |src_dir, dest_dir, file_name|
    replace_content_in_file("#{src_dir}/#{file_name}", /^.*#NOT_IN_PRODUCTION.*$/, '' )
  end
end

def replace_content_in_file file_name, pattern, replacement
  text = File.read(file_name)
  text.gsub!(pattern, replacement)
  File.open(file_name, 'w') {|file| file.puts text}
end
