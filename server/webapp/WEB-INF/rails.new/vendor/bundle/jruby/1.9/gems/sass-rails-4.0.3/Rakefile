require 'bundler'
Bundler::GemHelper.install_tasks

require 'rake/testtask'

Rake::TestTask.new(:test) do |t|
  t.libs << 'lib'
  t.libs << 'test'
  t.pattern = 'test/**/*_test.rb'
  t.verbose = false
end

desc 'Default: run unit tests.'
task default: :test

specname = "sass-rails.gemspec"
deps = `git ls-files`.split("\n") - [specname]

file specname => deps do
  files       = `git ls-files`.split("\n")
  test_files  = `git ls-files -- {test,spec,features}/*`.split("\n")
  executables = `git ls-files -- bin/*`.split("\n").map{ |f| File.basename(f) }

  require 'erb'

  File.open specname, 'w:utf-8' do |f|
    f.write ERB.new(File.read("#{specname}.erb")).result(binding)
  end
end
