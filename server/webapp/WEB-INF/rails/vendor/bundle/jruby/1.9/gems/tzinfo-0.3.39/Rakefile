# Available options:
#
# rake test - Runs all test cases.
# rake package - Runs test cases and builds packages for distribution.
# rake rdoc - Builds API documentation in doc dir.
# rake build_tz_modules - Builds Timezone modules and the Country index. 
#   Expects to find source data in ../data.
# rake build_tz_module zone=Zone/Name - Builds a single Timezone module. 
#   Expects to find source data in ../data.
# rake build_countries - Builds the Country index.
#   Expects to find source data in ../data.

require 'rake'
require 'rake/testtask'
require 'rake/rdoctask'
require 'rake/gempackagetask'
require 'fileutils'

Rake::TaskManager.class_eval do
  def remove_task(task_name)
    @tasks.delete(task_name.to_s)
  end
end

def remove_task(task_name)
  Rake.application.remove_task(task_name)
end

self.class.class_eval { alias_method :orig_sh, :sh }
private :orig_sh

def sh(*cmd, &block)
  if cmd.first =~ /\A__tar_with_owner__ -?([zjcvf]+)(.*)\z/
    opts = $1
    args = $2
    cmd[0] = "tar c --owner 0 --group 0 -#{opts.gsub('c', '')}#{args}"    
  end
  
  orig_sh(*cmd, &block)
end


BUILD_TZ_CLASSES_DIR = 'lib/tzinfo.build_tz_classes'

SPEC = eval(File.read('tzinfo.gemspec'))

package_task = Rake::GemPackageTask.new(SPEC) do |pkg|
  pkg.need_zip = true
  pkg.need_tar_gz = true
  pkg.tar_command = '__tar_with_owner__'
end

# Replace the Rake::PackageTask task that prepares the files to package with
# a version that ensures the permissions are correct for the package.
# Also just copy rather than link the files so that old versions are maintained.
remove_task package_task.package_dir_path
file package_task.package_dir_path => [package_task.package_dir] + package_task.package_files do
  mkdir_p package_task.package_dir_path rescue nil
  chmod(0755, package_task.package_dir_path)
  package_task.package_files.each do |fn|
    f = File.join(package_task.package_dir_path, fn)
    fdir = File.dirname(f)
    mkdir_p(fdir) if !File.exist?(fdir)
    if File.directory?(fn)
      mkdir_p(f)
      chmod(0755, f)
    else
      rm_f f
      cp(fn, f)
      chmod(0644, f)
    end
  end
end


# Replace the Rake::GemPackageTask task that builds the gem with a version that
# changes to the copied package directory first. This allows the gem builder
# to pick up the correct file permissions.
remove_task "#{package_task.package_dir}/#{package_task.gem_file}"
file "#{package_task.package_dir}/#{package_task.gem_file}" => [package_task.package_dir] + package_task.gem_spec.files do
  when_writing("Creating GEM") do
    chdir(package_task.package_dir_path) do
      Gem::Builder.new(package_task.gem_spec).build
    end
    
    verbose(true) do
      mv File.join(package_task.package_dir_path, package_task.gem_file), "#{package_task.package_dir}/#{package_task.gem_file}"
    end
  end
end


Rake::TestTask.new('test') do |t|
  # Force a particular timezone to be local (helps find issues when local
  # timezone isn't GMT). This won't work on Windows.
  ENV['TZ'] = 'America/Los_Angeles'

  t.libs << '.'
  t.pattern = 'test/tc_*.rb'
  t.verbose = true
end


Rake::RDocTask.new do |rdoc|
  rdoc.rdoc_dir = 'doc'
  rdoc.title = "TZInfo"
  rdoc.options << '--inline-source'
  rdoc.options.concat SPEC.rdoc_options
  rdoc.rdoc_files.include(*SPEC.extra_rdoc_files) 
  rdoc.rdoc_files.include('lib')  
end

task :build_tz_modules do
  require 'lib/tzinfo/tzdataparser'
  
  FileUtils.mkdir_p(BUILD_TZ_CLASSES_DIR)
  begin  
    p = TZInfo::TZDataParser.new('data', BUILD_TZ_CLASSES_DIR)
    p.execute
    
    scm = Scm.create(File.dirname(__FILE__))
    
    ['indexes', 'definitions'].each do |dir|
      scm.sync("#{BUILD_TZ_CLASSES_DIR}/#{dir}", "lib/tzinfo/#{dir}")
    end
  ensure
    FileUtils.rm_rf(BUILD_TZ_CLASSES_DIR)
  end
end

class Scm
  def self.create(dir)
    if File.directory?(File.join(dir, '.git'))
      GitScm.new(dir)
    elsif File.directory?(File.join(dir, '.svn'))
      SvnScm.new(dir)
    else
      NullScm.new(dir)
    end
  end

  def initialize(dir)
  end

  def sync(source_dir, target_dir)
    puts "Sync from #{source_dir} to #{target_dir}#{command ? " using #{command}" : ''}"
    sync_dirs(source_dir, target_dir)
  end

  protected

  def exec_scm(params)
    puts "#{command} #{params}"
    `#{command} #{params}`
    raise "#{command} exited with status #$?" if $? != 0  
  end

  private
  
  def sync_dirs(source_dir, target_dir)
    # Assumes a directory will never turn into a file and vice-versa
    # (files will all end in .rb, directories won't).

    source_entries, target_entries = [source_dir, target_dir].collect do |dir|
      Dir.entries(dir).delete_if {|entry| entry =~ /\A\./}.sort
    end
    
    until source_entries.empty? || target_entries.empty?          
      last_source = source_entries.last
      last_target = target_entries.last
    
      if last_source == last_target
        source_file = File.join(source_dir, last_source)
        target_file = File.join(target_dir, last_target)
      
        if File.directory?(source_file)
          sync_dirs(source_file, target_file)
        else
          FileUtils.cp(source_file, target_file)
        end     
      
        source_entries.pop
        target_entries.pop
      elsif source_entries.last < target_entries.last
        sync_only_in_target(target_dir, target_entries)
      else      
        sync_only_in_source(source_dir, target_dir, source_entries)
      end    
    end
    
    until target_entries.empty?
      sync_only_in_target(target_dir, target_entries)
    end
    
    until source_entries.empty?
      sync_only_in_source(source_dir, target_dir, source_entries)
    end
  end

  def sync_only_in_target(target_dir, target_entries)
    target_file = File.join(target_dir, target_entries.last)
    delete(target_file)
    target_entries.pop
  end

  def sync_only_in_source(source_dir, target_dir, source_entries)
    source_file = File.join(source_dir, source_entries.last)
    target_file = File.join(target_dir, source_entries.last)
        
    if File.directory?(source_file)
      Dir.mkdir(target_file)
      add(target_file)
      sync_dirs(source_file, target_file)
    else
      FileUtils.cp(source_file, target_file)
      add(target_file)
    end
    
    source_entries.pop
  end
end

class NullScm < Scm
  def command
    nil
  end
  
  def add(file)
  end
  
  def delete(file)
    puts "rm -rf \"#{file}\""
    FileUtils.rm_rf(file)
  end
end

class GitScm < Scm
  def command
    'git'
  end
  
  def add(file)
    unless File.directory?(file)
      exec_scm "add \"#{file}\""
    end
  end
  
  def delete(file)
    exec_scm "rm -rf \"#{file}\""
  end
end

class SvnScm < Scm
  def command
    'svn'
  end
  
  def add(file)
    exec_scm "add \"#{file}\""
  end
  
  def delete(file)
    exec_scm "delete --force \"#{file}\""
  end
end


task :build_tz_module do
  require 'lib/tzinfo/tzdataparser'
  p = TZInfo::TZDataParser.new('data', 'lib/tzinfo')
  p.generate_countries = false
  p.only_zones = [ENV['zone']]
  p.execute
end

task :build_countries do
  require 'lib/tzinfo/tzdataparser'
  p = TZInfo::TZDataParser.new('data', 'lib/tzinfo')
  p.generate_countries = true
  p.generate_zones = false
  p.execute
end
