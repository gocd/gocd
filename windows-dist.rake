def stdout_of(command)
  stdout = `#{command}`
  $?.success? || fail("`#{command}` failed")
  stdout
end

VERSION = '16.3.0'
RELEASE = stdout_of('git log --pretty=format:""').lines.count.to_s

require 'shellwords'
require 'pathname'

def makensis(build_root, package, versioned_dir)
  cd build_root do
    env = {
      'BINARY_SOURCE_DIR' => Pathname(versioned_dir).relative_path_from(Pathname(build_root)).to_s,
      'LIC_FILE'          => 'LICENSE.dos',
      'NAME'              => package.gsub(/^go-/, '').capitalize,
      'MODULE'            => package.gsub(/^go-/, ''),
      'GO_ICON'           => "#{package}.ico",
      'VERSION'           => "#{VERSION}-#{RELEASE}",
      'REGVER'            => "#{VERSION}#{RELEASE.rjust(5, '0')}".gsub(/\./, ''),
      'JAVA'              => 'jre',
      'JAVASRC'           => (Dir['jre*'].first),
      'DISABLE_LOGGING'   => ENV['DISABLE_WIN_INSTALLER_LOGGING'] || 'false'
    }
    sh(env, "makensis -NOCD ../../../../../installers/windows/#{package}/#{package}.nsi")
  end
end

namespace :server do
  package       = 'go-server'
  build_root    = "target/pkgs/#{package}/windows/BUILD_ROOT"
  versioned_dir = "#{build_root}/#{package}-#{VERSION}"

  task :clean do
    rm_rf build_root
  end

  task :init do
    mkdir_p build_root
    mkdir_p versioned_dir
  end

  task :layout do
    sh("set -o pipefail; curl --silent --fail #{ENV['WINDOWS_JRE_URL']} | tar -zxf - -C #{build_root}")
    sh("unix2dos < LICENSE > target/pkgs/#{package}/windows/BUILD_ROOT/LICENSE.dos")
    cp 'LICENSE', "#{versioned_dir}/LICENSE"

    # cp 'installers/windows/JavaHome.ini', "#{build_root}/JavaHome.ini"
    # cp 'installers/windows/gocd.ico', "#{build_root}/#{package}.ico"

    cp 'server/target/go.jar', "#{versioned_dir}/go.jar"
    cp 'installers/windows/go-server/server.cmd', "#{versioned_dir}/server.cmd"
    cp 'installers/windows/go-server/start-server.bat', "#{versioned_dir}/start-server.bat"
    cp 'installers/windows/go-server/stop-server.bat', "#{versioned_dir}/stop-server.bat"
    cp 'installers/windows/go-server/cruisewrapper.exe', "#{versioned_dir}/cruisewrapper.exe"
    cp 'installers/windows/go-server/config.properties', "#{versioned_dir}/config.properties"
    cp_r 'installers/windows/go-server/config', "#{versioned_dir}/config"
    cp_r 'installers/windows/go-server/lib', "#{versioned_dir}/lib"
  end

  task :package do
    makensis(build_root, package, versioned_dir)
  end

  task build: %w(clean init layout package)
end

namespace :agent do
  package       = 'go-agent'
  build_root    = "target/pkgs/#{package}/windows/BUILD_ROOT"
  versioned_dir = "#{build_root}/#{package}-#{VERSION}"

  task :clean do
    rm_rf build_root
  end

  task :init do
    mkdir_p build_root
    mkdir_p versioned_dir
  end

  task :layout do
    sh("set -o pipefail; curl --silent --fail #{ENV['WINDOWS_JRE_URL']} | tar -zxf - -C #{build_root}")
    sh("unix2dos < LICENSE > target/pkgs/#{package}/windows/BUILD_ROOT/LICENSE.dos")
    cp 'LICENSE', "#{versioned_dir}/LICENSE"

    # cp 'installers/windows/JavaHome.ini', "#{build_root}/JavaHome.ini"
    # cp 'installers/windows/go-agent/ServerIP.ini', "#{build_root}/ServerIP.ini"
    # cp 'installers/windows/gocd.ico', "#{build_root}/#{package}.ico"

    cp 'agent-bootstrapper/target/libs/agent-bootstrapper-16.3.0.jar', "#{versioned_dir}/agent-bootstrapper.jar"
    cp 'installers/windows/go-agent/agent.cmd', "#{versioned_dir}/agent.cmd"
    cp 'installers/windows/go-agent/start-agent.bat', "#{versioned_dir}/start-agent.bat"
    cp 'installers/windows/go-agent/stop-agent.bat', "#{versioned_dir}/stop-agent.bat"
    cp 'installers/windows/go-agent/README.md', "#{versioned_dir}/README.md"
    cp 'installers/windows/go-agent/cruisewrapper.exe', "#{versioned_dir}/cruisewrapper.exe"
    cp_r 'installers/windows/go-agent/config', "#{versioned_dir}/config"
    cp 'agent/properties/log4j.properties', "#{versioned_dir}/config/log4j.properties"
    cp_r 'installers/windows/go-agent/lib', "#{versioned_dir}/lib"
  end

  task :package do
    makensis(build_root, package, versioned_dir)
  end

  task build: %w(clean init layout package)
end

task agent: %w(agent:build)
task server: %w(server:build)

task default: %w(agent server)
