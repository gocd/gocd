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

begin

gem 'rdoc'
require 'rdoc/task'
desc 'Creates a symlink to rake' 's lib directory to support combined rdoc generation'
file 'rake/lib' do
  rake_path = $LOAD_PATH.find { |p| File.exist? File.join(p, 'rake.rb') }
  mkdir_p 'rake'
  File.symlink(rake_path, 'rake/lib')
end

desc 'Generate RDoc documentation in rdoc/'
RDoc::Task.new :rdoc do |rdoc|
  rdoc.rdoc_dir = 'rdoc'
  rdoc.title = spec.name
  rdoc.options = spec.rdoc_options.clone
  rdoc.rdoc_files.include('lib/**/*.rb')
  rdoc.rdoc_files.include spec.extra_rdoc_files

  # include rake source for better inheritance rdoc
  rdoc.rdoc_files.include('rake/lib/**.rb')
end
task 'rdoc' => %w(rake/lib)

require 'jekylltask'
module TocFilter
  def toc(input)
    output = "<ol class=\"toc\">"
    input.scan(/<(h2)(?:>|\s+(.*?)>)([^<]*)<\/\1\s*>/mi).each do |entry|
      id = (entry[1][/^id=(['"])(.*)\1$/, 2] rescue nil)
      title = entry[2].gsub(/<(\w*).*?>(.*?)<\/\1\s*>/m, '\2').strip
      if id
        output << %{<li><a href="##{id}">#{title}</a></li>}
      else
        output << %{<li>#{title}</li>}
      end
    end
    output << '</ol>'
    output
  end
end
Liquid::Template.register_filter(TocFilter)

desc 'Generate Buildr documentation in _site/'
JekyllTask.new 'jekyll' do |task|
  task.source = 'doc'
  task.target = '_site'
end

if 0 == system('pygmentize -V > /dev/null 2> /dev/null')
  puts 'Buildr uses the Pygments python library. You can install it by running ' 'sudo easy_install Pygments' ' or ' 'sudo apt-get install python-pygments' ''
end

desc 'Generate Buildr documentation as buildr.pdf'
file 'buildr.pdf' => '_site' do |task|
  pages = File.read('_site/preface.html').scan(/<li><a href=['"]([^'"]+)/).flatten.map { |f| "_site/#{f}" }
  sh 'prince', '--input=html', '--no-network', '--log=prince_errors.log', "--output=#{task.name}", '_site/preface.html', *pages
end

desc 'Build a copy of the Web site in the ./_site'
task 'site' => ['_site', :rdoc, 'buildr.pdf'] do
  cp_r 'rdoc', '_site'
  fail 'No RDocs in site directory' unless File.exist?('_site/rdoc/Buildr.html')
  cp 'CHANGELOG', '_site'
  open('_site/.htaccess', 'w') do |htaccess|
    htaccess << %Q{
<FilesMatch "CHANGELOG">
ForceType 'text/plain; charset=UTF-8'
</FilesMatch>
}
  end
  cp 'buildr.pdf', '_site'
  fail 'No PDF in site directory' unless File.exist?('_site/buildr.pdf')
  puts 'OK'
end

# Publish prerequisites to Web site.
desc "Publish complete web site"
task 'publish' => %w(site setup-local-site-svn) do
  puts "Uploading new site ..."
  sh 'rsync', '--progress', '--recursive', '--delete', '--exclude=.svn','_site/', 'site'
  task('publish-site-svn').invoke
  puts 'Done'
end

# Update HTML + PDF documentation (but not rdoc, changelog etc.)
desc "Publish non-release specific documentation to web site"
task 'publish-doc' => %w(buildr.pdf _site setup-local-site-svn) do
  cp 'buildr.pdf', '_site'
  puts 'Uploading new site ...'
  sh 'rsync', '--progress', '--recursive', '_site/', 'site' # Note: no --delete
  task('publish-site-svn').invoke
  puts 'Done'
end

task 'publish-site-svn' do
  cd 'site'
  sh 'svn', 'add', '--force', '.'
  cd '..'
  sh 'svn', 'commit', 'site', '-m', 'Publish latest site'
end

desc 'Checkout or update site to local directory'
task 'setup-local-site-svn' do
  if File.exist?('site')
    sh 'svn', 'up', 'site'
    sh 'svn', 'revert', '--recursive', 'site'
  else
    sh 'svn', 'co', 'https://svn.apache.org/repos/asf/buildr/site', 'site'
  end
end

rescue Exception => e
# The doc tasks do not work on our CI infrastructure with jruby as the native libraries
# are not compatible with version of the C++ library we are running there
end

task 'clobber' do
  rm_f 'rake/lib'
  rm_rf 'rake'
  rm_rf 'rdoc'
  rm_rf 'site'
  rm_rf '_site'
  rm_f 'buildr.pdf'
  rm_f 'prince_errors.log'
end
