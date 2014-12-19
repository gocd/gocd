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
require 'active_support'
require 'rubygems'

# active support
ActiveSupport.use_standard_json_time_format = true
ActiveSupport.escape_html_entities_in_json = false

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
  task('inline-rails-partials').invoke
  task('copy_inlined_erbs_to_webapp').invoke

  #prepare for production mode
  task('write_revision_number').invoke
  task('change_rails_env_to_production').invoke
end

task :handle_assets_rails4 do
  rm_rf("target/webapp/WEB-INF/rails.new/tmp")
  task('precompile_assets').invoke
  assets_location_in_target = "target/webapp/WEB-INF/rails.new/public/assets"
  rm_rf assets_location_in_target if File.exist? assets_location_in_target
  cp_r "webapp/WEB-INF/rails.new/public/assets", "target/webapp/WEB-INF/rails.new/public/"
  rm_rf "target/webapp/WEB-INF/rails.new/app/assets"

  #delete assets used by rails2
  rm_rf("target/webapp/javascripts")
  rm_rf("target/webapp/css")
  rm_rf("target/webapp/stylesheets")
  rm_rf("target/webapp/sass")
  rm_rf("target/webapp/images")
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

  if File.directory?("../helper/build")
    safe_cp "../helper/build/", "target/webapp/"
    FileUtils.mv "target/webapp/build", "target/webapp/help"
  end
end

task :write_revision_number do
  mkdir_p "target/webapp/WEB-INF/classes/ui/navigation"

  {ADMIN_VERSION_FILE => "%s(%s)", CRUISE_VERSION_FILE => "%s (%s)"}.each_pair do |path, template|
    File.open(path, "w") { |h| h.write(template % [VERSION_NUMBER, RELEASE_COMMIT]) }
  end
end

YUI_CSS_OUTPUT = ".css$:.css"
YUI_JS_OUTPUT = ".js$:.js"

def yui_compress_all(pattern, parent_directory, extension)
  sh "java -jar ../tools/yui-compressor-2.4.8/yuicompressor-2.4.8.jar --charset utf-8 -o '#{pattern}' #{File.join(parent_directory, extension)}" if ENV['YUI_COMPRESS_ASSETS'] == 'Y'
end

CSS_FILE_TERMINATOR=''
JS_FILE_TERMINATOR=';'

def merge(file_handle, path, terminator=CSS_FILE_TERMINATOR)
  contents = File.read(path)
  name = File.basename(path)
  file_handle.puts "/* #{name} - start */"
  file_handle.write(contents)
  file_handle.write(terminator) # Terminate file contents with *_FILE_TERMINATOR. For example, in case the JS script does not end with a ; as the author might be assuming it will be loaded standalone, we will introduce a ;
  file_handle.puts "\n/* #{name} - end */"
end

# javascript optimization
JS_LIB_DIR = "target/webapp/javascripts/lib"
JS_APP_DIR = "target/webapp/javascripts"
JS_APP_PUT_FIRST = [JS_APP_DIR + "/build_base_observer.js", JS_APP_DIR + "/json_to_css.js", JS_APP_DIR + "/util.js", JS_APP_DIR + "/micro_content_popup.js", JS_APP_DIR + "/ajax_popup_handler.js", JS_APP_DIR + "/compare_pipelines.js"]
COMPRESSED_ALL_DOT_JS = "target/all.js"
JS_TO_BE_SKIPPED = [JS_LIB_DIR + "/d3-3.1.5.min.js", JS_APP_DIR + "/test_helper.js"]

def put_first(lib_paths, files_to_put_first)
  files_to_put_first + (lib_paths - files_to_put_first)
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
def set_classpath
  server_test_dependency_file_path = File.expand_path(File.join(File.dirname(__FILE__), "target", "server-test-dependencies"))
  if Gem.win_platform?
    classpath = create_pathing_jar server_test_dependency_file_path
  else
    classpath = File.read(server_test_dependency_file_path)
  end
  ENV['CLASSPATH'] = classpath
end

task :precompile_assets do
  ruby = File.expand_path(File.join(File.dirname(__FILE__), "..", "tools", "bin", (Gem.win_platform? ? 'go.jruby.bat' : 'go.jruby')))
  set_classpath
  if Gem.win_platform?
    ENV['RAILS_ENV'] = "production"
    sh <<END
    cd #{File.expand_path(File.join(File.dirname(__FILE__), "webapp", "WEB-INF", "rails.new"))} && #{ruby} -S rake assets:clobber assets:precompile
END
  else
    sh "cd #{File.join("webapp/WEB-INF/rails.new")} && RAILS_ENV=production #{ruby} -S rake assets:clobber assets:precompile"
  end
end

task :jasmine_tests do
  ruby = File.expand_path(File.join(File.dirname(__FILE__), "..", "tools", "bin", (Gem.win_platform? ? 'go.jruby.bat' : 'go.jruby')))
  ENV['RAILS_ENV'] = "test"
  ENV['REPORTERS'] = "console,junit"
  set_classpath
  if Gem.win_platform?
    sh <<END
    cd #{File.expand_path(File.join(File.dirname(__FILE__), "webapp", "WEB-INF", "rails.new"))} && #{ruby} -S rake spec:javascript
END
  else
    sh "cd #{File.join("webapp/WEB-INF/rails.new")} && #{ruby} -S rake spec:javascript"
  end
end

# css optimization
CSS_DIRS = ["target/webapp/css", "target/webapp/stylesheets"]
COMPRESSED_ALL_DOT_CSS = ["target/plugins.css", "target/patterns.css", "target/views.css", "target/css_sass.css", "target/vm.css"]

def expand_css_wildcard wildcard
  Dir.glob("target/webapp/stylesheets/" + wildcard)
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

#:locals => ({:scope => expr})
#:locals => ({:scope => ({foo => bar}).merge(:baz => quux)})
#:object =>

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

  SANITIZE_FOR_PRODUCTION.each do |src_dir, dest_dir, file_name|
    replace_content_in_file("#{src_dir}/#{file_name}", /^.*#NOT_IN_PRODUCTION.*$/, '' )
  end
end

def replace_content_in_file file_name, pattern, replacement
  text = File.read(file_name)
  text.gsub!(pattern, replacement)
  File.open(file_name, 'w') {|file| file.puts text}
end
