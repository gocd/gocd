require "rubygems"
require "rake"
require "rake/clean"
require "rdoc/task"

task :default => ["build"]
CLEAN.include [ 'pkg', 'rdoc' ]
name = "net-ssh"

$:.unshift File.join(File.dirname(__FILE__), 'lib')
require "net/ssh"
version = Net::SSH::Version::CURRENT

begin
  require "jeweler"
  Jeweler::Tasks.new do |s|
    s.version = version
    s.name = name
    s.rubyforge_project = s.name
    s.summary = "Net::SSH: a pure-Ruby implementation of the SSH2 client protocol."
    s.description = s.summary + " It allows you to write programs that invoke and interact with processes on remote servers, via SSH2."
    s.email = "net-ssh@solutious.com"
    s.homepage = "https://github.com/net-ssh/net-ssh"
    s.authors = ["Jamis Buck", "Delano Mandelbaum"]

    # Note: this is run at package time not install time so if you are
    # running on jruby, you need to install jruby-pageant manually.
    if RUBY_PLATFORM == "java"
      s.add_dependency 'jruby-pageant', ">=1.1.1"
    end

    s.add_development_dependency 'test-unit'
    s.add_development_dependency 'mocha'

    s.license = "MIT"

    #s.signing_key = File.join('/mnt/gem/', 'gem-private_key.pem')
    #s.cert_chain  = ['gem-public_cert.pem']
  end
  Jeweler::GemcutterTasks.new
rescue LoadError
  puts "Jeweler (or a dependency) not available. Install it with: sudo gem install jeweler"
end

require 'rake/testtask'
Rake::TestTask.new do |t|
  t.libs = ["lib", "test"]
end

extra_files = %w[LICENSE.txt THANKS.txt CHANGES.txt ]
RDoc::Task.new do |rdoc|
  rdoc.rdoc_dir = "rdoc"
  rdoc.title = "#{name} #{version}"
  rdoc.generator = 'hanna' # gem install hanna-nouveau
  rdoc.main = 'README.rdoc'
  rdoc.rdoc_files.include("README*")
  rdoc.rdoc_files.include("bin/*.rb")
  rdoc.rdoc_files.include("lib/**/*.rb")
  extra_files.each { |file|
    rdoc.rdoc_files.include(file) if File.exists?(file)
  }
end
