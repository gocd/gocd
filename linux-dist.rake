def stdout_of(command)
  stdout = `#{command}`
  $?.success? || fail("`#{command}` failed")
  stdout
end

VERSION = '16.3.0'
RELEASE = stdout_of("git log --pretty=format:''").lines.count.to_s

require 'shellwords'
require 'json'


class RpmPackage
  def fpm_opts(package)
    opts = []
    opts << '-t' << type
    opts << '--depends' << 'which'
    opts << '--rpm-defattrfile' << '0440' # make sure all files, by default are only readable by their owners and group
    opts << '--rpm-defattrdir' << '0440' # make sure all dirs, by default are only readable by their owners and group

    package.directories.each do |dir, permissions|
      if permissions[:owned_by_package]
        opts << '--rpm-attr' << "#{permissions[:mode].to_s(8)},#{permissions[:owner]},#{permissions[:group]}:#{dir}"
      end
    end

    package.files.each do |file, permissions|
      # set ownership and mode on the rpm manifest
      opts << '--rpm-attr' << "#{permissions[:mode].to_s(8)},#{permissions[:owner]},#{permissions[:group]}:#{file}"
    end

    opts
  end

  def type
    'rpm'
  end

end

class DebPackage
  include Rake::DSL

  def fpm_opts(package)
    opts = []
    opts << '-t' << type
    opts << '--depends' << 'java7-runtime-headless'

    # HACK: for debian packages :(, since manifests cannot contain fine grained ownership
    opts << '--template-value' << "dir_permissions=target/tmp/dir_permissions_#{package.package_name}.json"
    opts << '--template-value' << "file_permissions=target/tmp/file_permissions_#{package.package_name}.json"

    mkdir_p 'target/tmp'
    open("target/tmp/dir_permissions_#{package.package_name}.json", 'w') { |f| f.puts(JSON.pretty_generate(package.directories)) }
    open("target/tmp/file_permissions_#{package.package_name}.json", 'w') { |f| f.puts(JSON.pretty_generate(package.files)) }

    opts
  end

  def type
    'deb'
  end

end

class LinuxPackage
  include Rake::DSL
  attr_reader :task_name
  attr_accessor :package_name, :description, :packaging_type, :version, :release, :directories, :files, :prepare

  def initialize(task_name)
    yield self
    @task_name = task_name
    define_tasks
  end

  def define_tasks
    desc 'clean build root'
    task :clean do
      rm_rf build_root
    end

    desc 'initialize build root'
    task :init do
      mkdir_p build_root
    end

    desc 'prepare filesystem layout for package manager'
    task :prepare do
      directories.keys.each do |dir|
        mkdir_p File.join(build_root, dir)
      end

      files.each do |destination, permissions|
        cp_r permissions[:source], File.join(build_root, destination)
      end
    end

    desc 'package the distribution'
    task :package do
      sh(Shellwords.join(fpm_command))
      mkdir_p 'target/pkg'
      mv Dir["#{package_name}*.#{packaging_type.type}"].first, 'target/pkg', force: true
    end

    desc 'clean init, prep, and package'
    task task_name => %w(clean init prepare package)
  end

  def fpm_command
    cmd = %w(fpm)
    # cmd << '--debug'
    # cmd << '-e'
    # cmd << '--debug-workspace'
    cmd << '--force'
    cmd << '-s' << 'dir'
    cmd << '-C' << File.expand_path(build_root)
    cmd << '--name' << package_name
    cmd << '--version' << VERSION
    cmd << '--iteration' << RELEASE
    cmd << '--license' << 'Apache-2.0'
    cmd << '--vendor' << 'ThoughtWorks, Inc.'
    cmd << '--category' << 'Development/Build Tools'
    cmd << '--architecture' << 'all'
    cmd << '--maintainer' << 'ThoughtWorks, Inc.'
    cmd << '--url' << 'https://go.cd'
    cmd << '--before-upgrade' << 'installers/fpm/shared/before-upgrade.sh.erb'
    cmd << '--before-install' << 'installers/fpm/shared/before-install.sh.erb'
    cmd << '--after-install' << 'installers/fpm/shared/after-install.sh.erb'
    cmd << '--before-remove' << 'installers/fpm/shared/before-remove.sh.erb'
    cmd << '--after-remove' << 'installers/fpm/shared/after-remove.sh.erb'
    cmd << '--template-scripts'

    cmd += packaging_type.fpm_opts(self)

    cmd << '--description' << description

    directories.each do |dir, permissions|
      cmd << '--directories' << dir if permissions[:owned_by_package]
    end #directories.each

    files.each do |file, permissions|
      cmd << '--config-files' << file.sub(/^\//, '') if permissions[:conf_file]
    end #files.each

    cmd
  end

  def build_root
    "target/pkgs/#{package_name}/#{packaging_type.type}/BUILD_ROOT"
  end
end


namespace :server do
  { rpm: RpmPackage, deb: DebPackage }.each do |name, packaging_type|
    LinuxPackage.new(name) do |t|
      t.package_name = 'go-server'
      t.description  = <<-EOF
Go Server Component
Next generation continuous integration and release management server from ThoughtWorks.
      EOF
      t.prepare do
        sh("sed -i -e s@go-server.log@/var/log/go-server/go-server.log@ -e s@go-shine.log@/var/log/go-server/go-shine.log@ #{File.join(t.build_root, '/etc/go/log4j.properties')}")
      end

      t.directories = {
        '/etc/default'             => { owned_by_package: false },
        '/etc/go'                  => { mode: 0770, owner: 'go', group: 'go', owned_by_package: true },
        '/etc/init.d'              => { owned_by_package: false },
        '/usr/share/doc/go-server' => { mode: 0755, owner: 'root', group: 'root', owned_by_package: true },
        '/usr/share/go-server'     => { mode: 0755, owner: 'root', group: 'root', owned_by_package: true },
        '/var/lib/go-server'       => { mode: 0750, owner: 'go', group: 'go', owned_by_package: true },
        '/var/log/go-server'       => { mode: 0770, owner: 'go', group: 'go', owned_by_package: true },
        '/var/run/go-server'       => { mode: 0770, owner: 'go', group: 'go', owned_by_package: true }
      }

      t.files          = {
        '/etc/default/go-server'              => { mode: 0640, owner: 'root', group: 'go', source: 'installers/fpm/server/go-server.default', conf_file: true },
        '/etc/go/log4j.properties'            => { mode: 0640, owner: 'root', group: 'go', source: 'server/properties/src/log4j.properties', conf_file: true },
        '/etc/init.d/go-server'               => { mode: 0755, owner: 'root', group: 'root', source: 'installers/fpm/server/go-server.init' },
        '/usr/share/doc/go-server/LICENSE'    => { mode: 0644, owner: 'root', group: 'root', source: 'LICENSE' },
        '/usr/share/go-server/go.jar'         => { mode: 0644, owner: 'root', group: 'root', source: 'server/target/go.jar' },
        '/usr/share/go-server/server.sh'      => { mode: 0755, owner: 'root', group: 'root', source: 'installers/fpm/server/server.sh' },
        '/usr/share/go-server/stop-server.sh' => { mode: 0755, owner: 'root', group: 'root', source: 'installers/fpm/server/stop-server.sh' }
      }
      t.packaging_type = packaging_type.new
      t.version        = VERSION
      t.release        = RELEASE
    end

    task all: name
  end

end

namespace :agent do
  { rpm: RpmPackage, deb: DebPackage }.each do |name, packaging_type|
    LinuxPackage.new(name) do |t|
      t.package_name = 'go-agent'
      t.description  = <<-EOF
Go Agent Component
Next generation continuous integration and release management server from ThoughtWorks.
      EOF
      t.prepare do
        sh("sed -i -e s@go-agent.log@/var/log/go-agent/go-agent.log@ #{File.join(t.build_root, '/var/lib/go-agent/log4j.properties')}")
      end

      t.directories = {
        '/etc/default'            => {},
        '/etc/init.d'             => {},
        '/usr/share/doc/go-agent' => { mode: 0755, owner: 'root', group: 'root', owned_by_package: true },
        '/usr/share/go-agent'     => { mode: 0755, owner: 'root', group: 'root', owned_by_package: true },
        '/var/lib/go-agent'       => { mode: 0750, owner: 'go', group: 'go', owned_by_package: true },
        '/var/log/go-agent'       => { mode: 0770, owner: 'go', group: 'go', owned_by_package: true },
        '/var/run/go-agent'       => { mode: 0770, owner: 'go', group: 'go', owned_by_package: true }
      }

      t.files          = {
        '/etc/default/go-agent'                      => { mode: 0640, owner: 'root', group: 'go', source: 'installers/fpm/agent/go-agent.default', conf_file: true },
        '/etc/init.d/go-agent'                       => { mode: 0755, owner: 'root', group: 'root', source: 'installers/fpm/agent/go-agent.init' },
        '/usr/share/doc/go-agent/LICENSE'            => { mode: 0644, owner: 'root', group: 'root', source: 'LICENSE' },
        '/usr/share/go-agent/agent-bootstrapper.jar' => { mode: 0644, owner: 'root', group: 'root', source: 'agent-bootstrapper/target/agent-bootstrapper.jar' },
        '/usr/share/go-agent/agent.sh'               => { mode: 0755, owner: 'root', group: 'root', source: 'installers/fpm/agent/agent.sh' },
        '/usr/share/go-agent/stop-agent.sh'          => { mode: 0755, owner: 'root', group: 'root', source: 'installers/fpm/agent/stop-agent.sh' },
        '/var/lib/go-agent/log4j.properties'         => { mode: 0644, owner: 'root', group: 'root', source: 'agent/properties/log4j.properties', conf_file: true }
      }
      t.packaging_type = packaging_type.new
      t.version        = VERSION
      t.release        = RELEASE
    end

    task all: name
  end
end

task default: %w(server:all agent:all)
