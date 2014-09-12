require "rubygems"
require "rubygems/package_task"
require "rdoc/task"
require "rake/testtask"

task :default => :test

Rake::TestTask.new do |t|
  t.libs += ["lib", "test"]
  t.test_files = FileList["test/*_test.rb"]
  t.verbose = true
end

RDoc::Task.new do |t|
  t.rdoc_files.include("README.rdoc", "lib/**/*.rb")
end

Gem::PackageTask.new(eval(IO.read(File.join(File.dirname(__FILE__), "yui-compressor.gemspec")))) do |pkg|
  pkg.need_zip = true
  pkg.need_tar = true
end
