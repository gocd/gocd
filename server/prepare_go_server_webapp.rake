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
  #prepare help docs
  task('generate_help_documents').invoke unless ENV['BUILD_DOC'] == 'no'

  #copy
  task('copy_files').invoke

  # js
  task('create_all_js').invoke
  task('copy_compressed_js_to_webapp').invoke

  # css
  task('pull_latest_sass').invoke

  task('version-image-urls-in-css').invoke

  task('create_all_css').invoke
  task('copy_compressed_css_to_webapp').invoke

  # rails
  task('copy-code-to-be-interpolated').invoke
  task('inline-rails-partials').invoke
  task('copy_inlined_erbs_to_webapp').invoke

  #prepare for production mode
  task('write_revision_number').invoke
  task('change_rails_env_to_production').invoke
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

  FileUtils.remove_dir("target/webapp/WEB-INF/rails/spec", true)
  FileUtils.remove_dir("target/webapp/WEB-INF/rails/vendor/rspec-1.2.8", true)
  FileUtils.remove_dir("target/webapp/WEB-INF/rails/vendor/rspec-rails-1.2.7.1", true)

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

# javascript optimization
JS_PATTERNS_TO_BE_COMPRESSED_IN_ORDER = ["es5-shim.js", "jquery.js", "jquery.timeago.js", "jquery.url.js", "jquery_no_conflict.js", "prototype.js", "scriptaculous.js", "bootstrap.min.js", "angular.1.0.8.min.js", "angular-resource.1.0.8.min.js", "build_base_observer.js", "effects.js", "json_to_css.js", "util.js", "micro_content_popup.js", "ajax_popup_handler.js", "stage_detail.js", "compare_pipelines.js", "*.js"]
JS_PATTERNS_NOT_TO_BE_COMPRESSED = ["d3.js", "Tooltip.js", "Tooltip_ext.js"]
JS_PATTERNS_NOT_TO_BE_FORCE_APPENDED = ["inplace-editor.js"]
COMPRESSED_ALL_DOT_JS = "target/all.js"

task :create_all_js do
  js_files_to_be_compressed = JS_PATTERNS_TO_BE_COMPRESSED_IN_ORDER.inject([]) do |to_be_compressed, wildcard|
    matched_paths = expand_js_wildcard(wildcard)
    to_be_compressed + matched_paths.sort { |first, second| File.basename(first) <=> File.basename(second) }
  end

  js_files_to_be_compressed = JS_PATTERNS_NOT_TO_BE_COMPRESSED.inject(js_files_to_be_compressed) do |to_be_compressed, wildcard|
    to_be_compressed - expand_js_wildcard(wildcard)
  end

  js_files_to_be_compressed = JS_PATTERNS_NOT_TO_BE_FORCE_APPENDED.inject(js_files_to_be_compressed) do |to_be_compressed, wildcard|
    matched_files = expand_js_wildcard(wildcard)
    (to_be_compressed - matched_files) + matched_files
  end

  File.open(COMPRESSED_ALL_DOT_JS, "w") do |h|
    js_files_to_be_compressed.uniq.each do |path|
      contents = File.read(path)
      name = File.basename(path)
      h.puts "// #{name} - start"
      h.write(contents)
      h.puts ";\n// #{name} - end"
    end
  end
end

task :copy_compressed_js_to_webapp do
  safe_cp COMPRESSED_ALL_DOT_JS, "target/webapp/compressed"
  cp "target/webapp/javascripts/d3.js", "target/webapp/compressed"
  FileUtils.remove_dir("target/webapp/javascripts", true)
end

def expand_js_wildcard wildcard
  Dir.glob("target/webapp/javascripts/" + wildcard)
end

# css optimization
CSS_DIRS = ["target/webapp/css", "target/webapp/stylesheets"]
COMPRESSED_ALL_DOT_CSS = ["target/plugins.css", "target/patterns.css", "target/views.css", "target/css_sass.css"]
CSS_TO_BE_COMPRESSED = ["plugins/*.css", "patterns/*.css", "views/*.css", "css_sass/**/*.css"]

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

task :create_all_css do
  CSS_TO_BE_COMPRESSED.each_with_index do |wildcard, index|
    matched_paths = expand_css_wildcard(wildcard)
    File.open(COMPRESSED_ALL_DOT_CSS[index], "w") do |h|
      matched_paths.each do |path|
        sh "java -jar ../tools/yui-compressor-2.4.8/yuicompressor-2.4.8.jar --type css --charset utf-8 -o #{path} #{path}"

        contents = File.read(path)
        name = File.basename(path)
        h.puts "/* #{name} - start */"
        h.write(contents)
        h.puts "\n/* #{name} - end */"
      end
    end
  end
end

task :copy_compressed_css_to_webapp do
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
RAILS_VIEWS_SRC = "target/webapp/WEB-INF/rails/app/views"
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
  safe_cp RAILS_INTERPOLATED_VIEWS, "target/webapp/WEB-INF/rails/app"
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

SANITIZE_FOR_PRODUCTION = NotInProduction.new('target/webapp', {'WEB-INF/rails/config' => 'routes.rb', 'WEB-INF/rails/app/controllers' => ['users_controller.rb'], 'WEB-INF/rails/app/controllers/admin' => 'backup_controller.rb'})

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
