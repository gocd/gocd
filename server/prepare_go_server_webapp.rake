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
$LOAD_PATH << File.join("webapp/WEB-INF/rails/vendor/rails/activesupport/lib")
require 'active_support'
require 'rubygems'

# Backport of missing SecureRandom methods from 1.9 - http://softover.com/UUID_in_Ruby_1.8
module SecureRandom
  class << self
    def method_missing(method_sym, *arguments, &block)
      case method_sym
        when :urlsafe_base64
          r19_urlsafe_base64(*arguments)
        when :uuid
          r19_uuid(*arguments)
        else
          super
      end
    end

    private
    def r19_urlsafe_base64(n=nil, padding=false)
      s = [random_bytes(n)].pack("m*")
      s.delete!("\n")
      s.tr!("+/", "-_")
      s.delete!("=") if !padding
      s
    end

    def r19_uuid
      ary = random_bytes(16).unpack("NnnnnN")
      ary[2] = (ary[2] & 0x0fff) | 0x4000
      ary[3] = (ary[3] & 0x3fff) | 0x8000
      "%08x-%04x-%04x-%04x-%04x%08x" % ary
    end
  end
end

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

  #prepare help docs
  task('generate_help_documents').invoke unless ENV['BUILD_DOC'] == 'no'

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

task :handle_assets_rails2 do
  # js
  task('create_all_js_rails2').invoke
  task('copy_compressed_js_to_webapp_rails2').invoke

  # css
  task('pull_latest_sass').invoke

  task('version-image-urls-in-css').invoke

  task('create_all_css_rails2').invoke
  task('copy_compressed_css_to_webapp_rails2').invoke
end

task :handle_assets_rails4 do
  rm_rf("target/webapp/WEB-INF/rails.new/tmp")
  task('precompile_assets').invoke
  assets_location_in_target = "target/webapp/WEB-INF/rails.new/public/assets"
  rm_rf assets_location_in_target if File.exist? assets_location_in_target
  cp_r "webapp/WEB-INF/rails.new/public/assets", "target/webapp/WEB-INF/rails.new/public/"

  #delete assets used by rails2
  rm_rf("target/webapp/javascripts")
  rm_rf("target/webapp/css")
  rm_rf("target/webapp/stylesheets")
  rm_rf("target/webapp/sass")
  rm_rf("target/webapp/images")
end

task :handle_assets do
  ENV['USE_NEW_RAILS'] == "Y" ? task('handle_assets_rails4').invoke : task('handle_assets_rails2').invoke
end

#prepare help docs
task :generate_help_documents do
  plugin_api_target_dir = "../plugin-infra/go-plugin-api/target/"
  api_doc_files=[]
  Dir.glob(plugin_api_target_dir+'/*current.jar') do |file_name|
    api_doc_files << file_name
  end
  api_doc_files << File.join(plugin_api_target_dir, "javadoc")
  api_doc_files << File.join(plugin_api_target_dir, "classes/plugin-descriptor.xsd")
  api_doc_files << File.join("..", "plugin-infra", "sample-plugins", "target", "go-sample-plugins.zip")

  ruby = File.expand_path('../../tools/bin', __FILE__) + (Gem.win_platform? ? '/go.jruby.bat' : '/go.jruby')
  sh "cd #{File.join("../helper/")} && #{ruby} -S rake site build_sitemap"

  require 'fileutils'
  FileUtils::mkdir_p(File.join("../helper/build/resources"))
  cp_r api_doc_files, File.join("../helper", "build/resources")
end

#copy
ADMIN_VERSION_FILE = "target/webapp/WEB-INF/vm/admin/admin_version.txt.vm"
CRUISE_VERSION_FILE = "target/webapp/WEB-INF/classes/ui/navigation/cruise_version.st"

task :copy_files do
  safe_cp "webapp", "target"

  if ENV['USE_NEW_RAILS'] == "Y"
    FileUtils.remove_dir("target/webapp/WEB-INF/rails", true)
  else
    FileUtils.remove_dir("target/webapp/WEB-INF/rails.new", true)

    FileUtils.remove_dir("target/webapp/WEB-INF/rails/spec", true)
    FileUtils.remove_dir("target/webapp/WEB-INF/rails/vendor/rspec-1.2.8", true)
    FileUtils.remove_dir("target/webapp/WEB-INF/rails/vendor/rspec-rails-1.2.7.1", true)
  end

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


task :precompile_assets do
  ruby = File.expand_path('../../tools/bin', __FILE__) + (Gem.win_platform? ? '/go.jruby.bat' : '/go.jruby')
  classpath = File.read("target/server-test-dependencies")
  sh "cd #{File.join("webapp/WEB-INF/rails.new")} && CLASSPATH=#{classpath} RAILS_ENV=production #{ruby} -S rake assets:clobber assets:precompile"
end

task :create_all_js_rails2 do
  yui_compress_all(YUI_JS_OUTPUT, JS_LIB_DIR, "*.js")
  yui_compress_all(YUI_JS_OUTPUT, JS_APP_DIR, "*.js")

  priority_libs = ["es5-shim.min.js", "jquery-1.7.2.js", "jquery.timeago-1.2.3.js", "jquery.url-1.0.js", "jquery_no_conflict.js", "prototype-1.6.0.js",
                   "scriptaculous-1.8.0.js", "bootstrap-2.3.2.min.js", "angular.1.0.8.min.js", "angular-resource.1.0.8.min.js", "effects-1.8.0.js"]
  libs_to_load_first = []
  priority_libs.each do |lib|
    libs_to_load_first << File.join(JS_LIB_DIR, lib)
  end
  libs_to_load_second = Dir.glob(File.join(JS_LIB_DIR, "*.js")) - libs_to_load_first

  app_paths = put_first(Dir.glob(File.join(JS_APP_DIR, "*.js")), JS_APP_PUT_FIRST)

  file_list = libs_to_load_first + libs_to_load_second + app_paths

  File.open(COMPRESSED_ALL_DOT_JS, "w") do |file_handle|
    file_list.each do |path|
      merge(file_handle, path, JS_FILE_TERMINATOR) unless JS_TO_BE_SKIPPED.include? path
    end
  end
end

task :copy_compressed_js_to_webapp_rails2 do
  safe_cp COMPRESSED_ALL_DOT_JS, "target/webapp/compressed"
  safe_cp "target/webapp/javascripts/lib/d3-3.1.5.min.js", "target/webapp/compressed"
  FileUtils.remove_dir("target/webapp/javascripts", true)
end

# css optimization
CSS_DIRS = ["target/webapp/css", "target/webapp/stylesheets"]
COMPRESSED_ALL_DOT_CSS = ["target/plugins.css", "target/patterns.css", "target/views.css", "target/css_sass.css", "target/vm.css"]

def expand_css_wildcard wildcard
  Dir.glob("target/webapp/stylesheets/" + wildcard)
end

task :pull_latest_sass do
  # Clear css_sass folder before regenerating new css files
  FileUtils.remove_dir("target/webapp/stylesheets/css_sass", true)

  ruby = File.expand_path('../../tools/bin', __FILE__) + (Gem.win_platform? ? '/go.jruby.bat' : '/go.jruby')
  sh "cd target/webapp/sass && #{ruby} -S sass --update .:../stylesheets/css_sass"

  FileUtils.remove_dir("target/webapp/sass", true)
end

task :create_all_css_rails2 do
  main_dir = "target/webapp/stylesheets/"
  yui_compress_all(YUI_CSS_OUTPUT, main_dir, "*.css")
  File.open("target/all.css", "w") do |handle|
    ["main.css", "layout.css", "structure.css", "ie_hacks.css", "module.css"].each do |file|
      merge(handle, File.join(main_dir, file))
    end
  end

  parent_directory = "target/webapp/stylesheets"
  css_directories_to_be_compressed = [{:dir => "plugins", :perform => Proc.new do |d| yui_compress_all(YUI_CSS_OUTPUT, File.join(parent_directory, d), "*.css") end},
                                      {:dir => "patterns", :perform => Proc.new do |d| yui_compress_all(YUI_CSS_OUTPUT, File.join(parent_directory, d), "*.css") end},
                                      {:dir => "views", :perform => Proc.new do |d| yui_compress_all(YUI_CSS_OUTPUT, File.join(parent_directory, d), "*.css") end},
                                      {:dir => "css_sass", :perform => Proc.new do |d| yui_compress_all(YUI_CSS_OUTPUT, File.join(parent_directory, d), "**/*.css") end},
                                      {:dir => "vm", :perform => Proc.new do |d| yui_compress_all(YUI_CSS_OUTPUT, File.join(parent_directory, d), "*.css") end}]

  css_directories_to_be_compressed.each do |tuple|
    tuple[:perform].call(tuple[:dir])
  end

  css_to_be_merged = ["plugins/*.css", "patterns/*.css", "views/*.css", "css_sass/**/*.css", "vm/**/*.css"]
  css_to_be_merged.each_with_index do |wildcard, index|
    matched_paths = expand_css_wildcard(wildcard)
    File.open(COMPRESSED_ALL_DOT_CSS[index], "w") do |handle|
      matched_paths.each do |path|
        merge(handle, path)
      end
    end
  end

  # compress each file in css/ & stylesheets/structure/
  yui_compress_all(YUI_CSS_OUTPUT, "target/webapp/css", "*.css")
  yui_compress_all(YUI_CSS_OUTPUT, "target/webapp/stylesheets/structure", "*.css")
end

task :copy_compressed_css_to_webapp_rails2 do
  cp "target/all.css", "target/webapp/stylesheets"
  COMPRESSED_ALL_DOT_CSS.each do |file|
    name = File.basename(file).gsub(File.extname(file), '')
    cp file, "target/webapp/stylesheets/#{name}"
  end
end

#image optimization
task "version-image-urls-in-css" do
  CSS_DIRS.each do |css_dir|
    Dir.glob File.join(css_dir, '*', '*.css') do |file|
      content = File.read(file).gsub /url\(['"\s]*([^\)"'\s]+)['"\s]*\)/, "url(\\1?#{VERSION_NUMBER})"
      File.open(file, 'w') do |f|
        f.write(content)
      end
    end
  end
end

# inline partials
RAILS_DIR = ENV['USE_NEW_RAILS'] == "Y" ? "rails.new" : "rails"
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

def locals_hash call
  render_arguments = render_args(call)
  scope_map = render_arguments.scan(/:locals\s*=>\s*\{\s*:scope\s*=>\s*(.*?)\s*\}\s*\Z/m)
  scope_map.flatten[0]
end

def inline_partial_render_calls file, depth
  inline_render_calls(File.read(file), file, /<%=\s*render\s*\:partial.*?%>/m, depth)
end

def inline_render_calls buffer, file, scanned_by, depth
  render_partial_calls = buffer.scan(scanned_by)
  render_partial_calls.each do |call|
    ruby_code = erb_ruby(call)
    local_hash = locals_hash(call)
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
  buffer = inline_render_calls(buffer, file, /<%=\s*render_json\s*\:partial.*?%>/m, depth) do |sub_buffer|
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
