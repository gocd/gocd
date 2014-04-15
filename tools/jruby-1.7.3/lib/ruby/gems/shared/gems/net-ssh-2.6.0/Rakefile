require 'rubygems'
require 'rubygems/package_task'
require 'rake/clean'
require 'fileutils'
include FileUtils

require 'rdoc/task'


task :default => :package
 
# CONFIG =============================================================

# Change the following according to your needs
README = "README.rdoc"
CHANGES = "CHANGELOG.rdoc"
THANKS = 'THANKS.rdoc'
LICENSE = "LICENSE.rdoc"

# Files and directories to be deleted when you run "rake clean"
CLEAN.include [ 'pkg', '*.gem', '.config', 'doc']

# Virginia assumes your project and gemspec have the same name
name = 'net-ssh'
load "#{name}.gemspec"
version = @spec.version

# That's it! The following defaults should allow you to get started
# on other things. 


# TESTS/SPECS =========================================================



# INSTALL =============================================================

Gem::PackageTask.new(@spec) do |p|
  p.need_tar = true if RUBY_PLATFORM !~ /mswin/
end

task :build => [ :package ]
task :release => [ :rdoc, :package ]
task :install => [ :rdoc, :package ] do
	sh %{sudo gem install pkg/#{name}-#{version}.gem}
end
task :uninstall => [ :clean ] do
	sh %{sudo gem uninstall #{name}}
end


# RUBYFORGE RELEASE / PUBLISH TASKS ==================================

if @spec.rubyforge_project
  desc 'Publish website to rubyforge'
  task 'publish:rdoc' => 'doc/index.html' do
    sh "scp -r doc/* rubyforge.org:/var/www/gforge-projects/#{name}/ssh/v2/api/"
  end

  desc 'Public release to rubyforge'
  task 'publish:gem' => [:package] do |t|
    sh <<-end
      rubyforge add_release -o Any -a #{CHANGES} -f -n #{README} #{name} #{name} #{@spec.version} pkg/#{name}-#{@spec.version}.gem &&
      rubyforge add_file -o Any -a #{CHANGES} -f -n #{README} #{name} #{name} #{@spec.version} pkg/#{name}-#{@spec.version}.tgz 
    end
  end
end



# RUBY DOCS TASK ==================================

RDoc::Task.new do |t|
  # this only works with RDoc 3.1 or greater
  t.generator = 'hanna'   # gem install hanna-nouveau
	t.rdoc_dir = 'doc'
	t.title    = @spec.summary
	t.main = README
	t.rdoc_files.include(README)
	t.rdoc_files.include(CHANGES)
	t.rdoc_files.include(THANKS)
 	t.rdoc_files.include(LICENSE)
	t.rdoc_files.include('lib/**/*.rb')
end
