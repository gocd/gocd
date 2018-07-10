##########################################################################
# Copyright 2016 ThoughtWorks, Inc.
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
##########################################################################

RAILS_INTERPOLATED_VIEWS = (ENV['INPUT_DIR'] or raise 'INPUT_DIR not defined')

def inline_partials(dir = RAILS_INTERPOLATED_VIEWS)
  Dir[File.join(dir, '*')].each do |path|
    File.directory?(path) ? inline_partials(path) : inline_partial(path)
  end
end

def erb_ruby(call)
  call.scan(/\A<%=\s*(.+?)\s*-?%>\Z/m).flatten[0]
end

def args_of_fn(fn_name, call)
  ruby_code               = erb_ruby(call)
  call_matcher_with_paren = /\A\s*#{fn_name}\s*\(/m
  arg_match               = (ruby_code =~ call_matcher_with_paren) ? ruby_code.scan(/#{call_matcher_with_paren}(.*?);?\s*\);?\s*\Z/m) : ruby_code.scan(/\A\s*#{fn_name}\s*(.*?)\s*;?\s*\Z/m)
  arg_match.flatten[0]
end

def render_args(call)
  args_of_fn('render', call)
end

def locals_hash(file, call)
  render_arguments  = render_args(call)
  scope_map         = render_arguments.scan(/:locals\s*=>\s*\{\s*:scope\s*=>\s*(.*?)\s*\}[\s)]*\Z/m)
  locals_hash_value = scope_map.flatten[0]

  parser_says_there_are_no_locals = locals_hash_value.nil?
  probably_has_locals             = render_arguments =~ /locals.*scope/
  scope_is_not_empty              = render_arguments !~ /:locals\s*=>\s*:scope\s*=>\s*\{\s*\}/
  if parser_says_there_are_no_locals && probably_has_locals && scope_is_not_empty
    raise "ERROR: Inlining partials of #{file}. Parser says there are no locals here: #{call}"
  end

  locals_hash_value
end

def inline_partial_render_calls(file, depth)
  inline_render_calls(File.read(file), file, /<%=\s*render[\s(]*:partial.*?%>/m, depth)
end

def inline_render_calls(buffer, file, scanned_by, depth)
  render_partial_calls = buffer.scan(scanned_by)
  render_partial_calls.each do |call|
    ruby_code           = erb_ruby(call)
    local_hash          = locals_hash(file, call)
    partial_path        = ruby_code.scan(/:partial\s*=>\s*("|')(.+?)\1/m).flatten[1] #make me locals style
    partial_path        = (partial_path =~ /\//m ? File.join(RAILS_INTERPOLATED_VIEWS, File.dirname(partial_path), '_' + File.basename(partial_path)) : File.join(File.dirname(file), '_' + File.basename(partial_path)))
    actual_partial_path = actual_partial_path(partial_path)
    content             = inline_partial(actual_partial_path, depth + 1)
    if local_hash
      prefix  = '_'*depth
      content = "<% #{prefix}scope = #{local_hash}; -%>" + fix_scope_calls(content, prefix)
    end
    buffer.sub!(call, block_given? ? yield(content) : content)
  end
  buffer
end

def actual_partial_path(partial_path)
  glob = Dir.glob(partial_path)
  if glob.empty?
    glob = Dir.glob(partial_path + '.*')
    raise "cannot find partial: #{partial_path}" if glob.empty?
  end
  glob[0]
end

def inline_partial(file, depth = 0)
  return unless File.extname(file) == '.erb'
  buffer = inline_partial_render_calls(file, depth)
  unless File.basename(file) =~ /^_/
    File.open(file, 'w') do |fd|
      fd.write(buffer)
    end
  end
  buffer
end

def fix_scope_calls(content, prefix)
  content.gsub(/([^_])scope\[:/, "\\1#{prefix}scope[:")
end

task 'default' do
  inline_partials
end
