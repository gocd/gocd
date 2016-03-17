def stdout_of(command)
  stdout = `#{command}`
  $?.success? || fail("`#{command}` failed")
  stdout
end

VERSION = '16.3.0'
RELEASE = stdout_of('git log --pretty=format:""').lines.count.to_s

require 'shellwords'
require 'pathname'
require 'erb'

class OsxPackage
  include Rake::DSL
  attr_reader :task_name
  attr_accessor :package_name, :app_name, :src_jar, :version, :release

  def initialize(task_name, &blk)
    yield self
    @task_name = task_name
    define_tasks
  end

  def define_tasks
    namespace task_name do
      task :clean do
        rm_rf app_dir
      end

      task :init do
        mkdir_p app_dir
        mkdir_p File.join(app_dir, 'Contents')
        mkdir_p File.join(app_dir, 'Contents', 'MacOS')
        mkdir_p File.join(app_dir, 'Contents', 'Resources')
      end

      task :layout do
        cp src_jar, File.join(app_dir, 'Contents', 'Resources', File.basename(src_jar))
        cp 'installers/osx/JavaApplicationStub64', File.join(app_dir, 'Contents', 'MacOS', package_name)
        cp 'installers/osx/gocd.icns', File.join(app_dir, 'Contents', 'Resources', "#{package_name}.icns")
        open(File.join(app_dir, 'Contents', 'Info.plist'), 'w') do |f|
          f.puts(ERB.new(File.read("installers/osx/#{package_name}/Info.plist.erb")).result(binding))
        end
      end

      task :package do
        cd build_root do
          sh("zip -r9 #{package_name}-#{version}-#{release}-osx.zip '#{app_name}'")
        end
      end

      task all: %w(clean init layout package)
    end

    desc "Build the #{app_name} app"
    task task_name => "#{task_name}:all"
  end

  def app_dir
    "#{build_root}/#{app_name}"
  end

  def build_root
    "target/pkgs/#{package_name}/osx/BUILD_ROOT"
  end
end

OsxPackage.new('server') do |t|
  t.package_name = 'go-server'
  t.app_name     = 'Go Server.app'
  t.src_jar      = 'server/target/go.jar'
  t.version      = VERSION
  t.release      = RELEASE
end

OsxPackage.new('agent') do |t|
  t.package_name = 'go-agent'
  t.app_name     = 'Go Agent.app'
  t.src_jar      = 'agent-bootstrapper/target/agent-bootstrapper.jar'
  t.version      = VERSION
  t.release      = RELEASE
end

task default: %w(server agent)
