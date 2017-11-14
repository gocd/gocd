# encoding: utf-8

require 'fileutils'
require 'bundler/gem_tasks'
require 'rspec/core/rake_task'
RSpec::Core::RakeTask.new(:spec) do |spec|
  spec.pattern = FileList['spec/**/*_spec.rb']
end

desc "Rebuild lib/uglify.js"
task :js do
  cd 'vendor/source-map/' do
    `npm install`
  end

  cd 'vendor/uglifyjs/' do
    # required to run ./uglifyjs2 --self; not bundled.
    `npm install`
  end

  cd 'vendor/uglifyjs-harmony' do
    # required to run ./uglifyjs2 --self; not bundled.
    `npm install`
  end

  FileUtils.cp("vendor/source-map/dist/source-map.js", "lib/source-map.js")

  source = `./vendor/uglifyjs/bin/uglifyjs --self --comments /Copyright/`
  File.write("lib/uglify.js", source)

  harmony_source = `./vendor/uglifyjs-harmony/bin/uglifyjs --self --comments /Copyright/`
  File.write("lib/uglify-harmony.js", harmony_source)

  FileUtils.cp("vendor/split/split.js", "lib/split.js")
  `patch -p1 -i patches/es5-string-split.patch`
end

begin
  require 'rubocop/rake_task'
  RuboCop::RakeTask.new(:rubocop)
  task :default => [:rubocop, :spec]
rescue LoadError
  task :default => [:spec]
end
