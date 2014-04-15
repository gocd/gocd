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


require 'rake/tasklib'
require 'jekyll'


class JekyllTask < Rake::TaskLib
  def initialize(name=:jekyll)  # :yield: self
    @name = name
    @source = name
    @target = name
    yield self if block_given?
    task name, :auto, :needs=>[@source] do |task, args|
      generate args.auto
    end
    if @source != @target
      file @target=>FileList["#{@source}/**/*"] do
        generate
      end
      task 'clobber' do
        rm_rf @target
      end
    end
  end

  attr_accessor :source
  attr_accessor :target
  attr_accessor :pygments

  def generate(auto = false)
    process = lambda do
      Jekyll.pygments = @pygments
      Jekyll.process source, target
      touch target
    end

    if auto
      require 'directory_watcher'
      puts "Auto generating: just edit a page and save, watch the console to see when we're done regenerating pages"
      dw = DirectoryWatcher.new(source)
      dw.interval = 1
      dw.glob = Dir.chdir(source) do
        dirs = Dir['*'].select { |x| File.directory?(x) }
        dirs -= [target]
        dirs = dirs.map { |x| "#{x}/**/*" }
        dirs += ['*']
      end
      dw.start
      dw.add_observer do |*args|
        t = Time.now.strftime("%Y-%m-%d %H:%M:%S")
        puts "[#{t}] regeneration: #{args.size} files changed"
        process.call
        puts "Done"
      end
      loop { sleep 1 }
    else
      puts "Generating documentation in #{target}"
      process.call
    end
  end
end


# TODO: Worked around bug in Jekyll 0.4.1. Removed when 0.4.2 is out.
# http://github.com/mojombo/jekyll/commit/c180bc47bf2f63db1bff9f6600cccbe5ad69077e#diff-0
class Albino
  def execute(command)
    output = ''
    Open4.popen4(command) do |pid, stdin, stdout, stderr|
      stdin.puts @target
      stdin.close
      output = stdout.read.strip
      [stdout, stderr].each { |io| io.close }
    end
    output
  end
end

class Jekyll::Page
  def render(layouts, site_payload)
    puts "... #{@name}"
    payload = {"page" => self.data}.deep_merge(site_payload)
    do_layout(payload, layouts)
  end
end

module TocFilter
  def toc(input)
    input.scan(/<(h2)(?:>|\s+(.*?)>)(.*?)<\/\1\s*>/mi).inject(%{<ol class="toc">}) { |toc, entry|
      id = entry[1][/^id=(['"])(.*)\1$/, 2]
      title = entry[2].gsub(/<(\w*).*?>(.*?)<\/\1\s*>/m, '\2').strip
      toc << %{<li><a href="##{id}">#{title}</a></li>}
    } << "</ol>"
  end
end
Liquid::Template.register_filter(TocFilter)
