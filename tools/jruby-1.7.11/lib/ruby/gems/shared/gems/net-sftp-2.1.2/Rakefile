require "rubygems"
require "rake"
require "rake/clean"
require "rdoc/task"

task :default => ["build"]
CLEAN.include [ 'pkg', 'rdoc' ]
name = "net-sftp"

$:.unshift File.join(File.dirname(__FILE__), 'lib')
require "net/sftp/version"
version = Net::SFTP::Version::STRING.dup

begin
  require "jeweler"
  Jeweler::Tasks.new do |s|
    s.version = version
    s.name = name
    s.rubyforge_project = s.name
    s.summary = "A pure Ruby implementation of the SFTP client protocol"
    s.description = s.summary
    s.email = "net-ssh@solutious.com"
    s.homepage = "https://github.com/net-ssh/net-sftp"
    s.authors = ["Jamis Buck", "Delano Mandelbaum"]

    s.add_dependency 'net-ssh', ">=2.6.5"

    s.add_development_dependency 'test-unit'
    s.add_development_dependency 'mocha'

    s.license = "MIT"

    s.signing_key = File.join('/mnt/gem/', 'gem-private_key.pem')
    s.cert_chain  = ['gem-public_cert.pem']
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

