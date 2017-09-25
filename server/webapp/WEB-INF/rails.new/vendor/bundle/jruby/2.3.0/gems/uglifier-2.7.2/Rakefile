# encoding: utf-8

require 'rubygems'
require 'bundler'
require 'bundler/gem_tasks'

begin
  Bundler.setup(:default, :development)
rescue Bundler::BundlerError => e
  $stderr.puts e.message
  $stderr.puts "Run `bundle install` to install missing gems"
  exit e.status_code
end

require 'rake'

require 'rspec/core'
require 'rspec/core/rake_task'
RSpec::Core::RakeTask.new(:spec) do |spec|
  spec.pattern = FileList['spec/**/*_spec.rb']
end

require 'rdoc/task'
Rake::RDocTask.new do |rdoc|
  version = File.exist?('VERSION') ? File.read('VERSION') : ""

  rdoc.rdoc_dir = 'rdoc'
  rdoc.title = "uglifier #{version}"
  rdoc.rdoc_files.include('README*')
  rdoc.rdoc_files.include('lib/**/*.rb')
end

desc "Rebuild lib/uglify.js"
task :js do
  cd 'vendor/source-map/' do
    `npm install`
    `node Makefile.dryice.js`
  end

  cd 'vendor/uglifyjs/' do
    # required to run ./uglifyjs2 --self; not bundled.
    `npm install`
  end

  source = ""
  source << "window = this;"
  source << File.read("vendor/source-map/dist/source-map.js")
  source << "MOZ_SourceMap = sourceMap;"
  source << `./vendor/uglifyjs/bin/uglifyjs --self --comments /Copyright/`

  File.write("lib/uglify.js", source)
end

begin
  require 'rubocop/rake_task'
  RuboCop::RakeTask.new(:rubocop)
  task :default => [:spec]
rescue LoadError
  task :default => [:spec]
end
