##########################GO-LICENSE-START################################
# Copyright 2014 ThoughtWorks, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##########################GO-LICENSE-END##################################

require 'socket'

Buildr::Project.class_eval do
  attr_reader :main_jars, :test_jars, :twist_jars

  alias_method :actual_initialize, :initialize

  def initialize *args
    actual_initialize *args
    @main_jars = []
    @test_jars = []
    @twist_jars = []
  end

  def revision
    `hg parent --template "{rev}-{node|short}"`
  end
end

Buildr::Filter::Mapper.class_eval do
  def web_xml_line_transform(content, path = nil)
    content.gsub(/\<(.*?)\>(.*?)\<\/\1\>/) { |match| yield(match) || match }
  end

  def line_remover_transform(content, path = nil)
    content.gsub(/^.*#NOT_IN_PRODUCTION.*$/) { |match| '' }
  end
end

def agent_bootstrapper_layout(_module)
  layout = Layout.new
  layout[:target] = "../target/#{_module}"
  layout[:root] = ".."
  layout[:module_name] = _module
  layout
end

def server_layout(_module)
  layout = Layout::Default.new
  layout[:root] = ".."
  layout[:reports, :specs] = "../target/specs/#{_module}"
  layout[:reports, :twist] = "../target/reports/twist/#{_module}"
  layout[:target, :temp] = '../target/temp'
  layout[:module_name] = _module
  layout
end

def submodule_layout(_module)
  layout = Layout.new
  layout[:source, :main, :java] = 'src'
  layout[:source, :test, :java] = 'test'
  layout[:target] = "../target/#{_module}"
  layout[:root] = ".."
  layout[:reports, :junit] = "../target/reports/#{_module}"
  layout[:reports, :specs] = "../target/specs/#{_module}"
  layout[:reports, :twist] = "../target/reports/twist/#{_module}"
  layout[:target, :temp] = '../target/temp'
  layout[:module_name] = _module
  layout
end

def child_submodule_layout(_parent, _module)
  new_layout = _parent.layout.dup
  new_layout[:target] = _parent.path_to(:target, _module)
  new_layout
end

def package_submodule_layout(_parent, _module, _package_basename)
  new_layout = child_submodule_layout(_parent, _module)
  new_layout[:debian_package] = "#{new_layout[:target]}/#{_package_basename}-#{VERSION_NUMBER}-#{RELEASE_REVISION}.deb"
  new_layout[:redhat_package] = "#{new_layout[:target]}/#{_package_basename}-#{VERSION_NUMBER}-#{RELEASE_REVISION}.noarch.rpm"
  new_layout[:windows_package] = "#{new_layout[:target]}/windows_package/#{_package_basename}-#{VERSION_NUMBER}-#{RELEASE_REVISION}-setup.exe"
  new_layout[:solaris_package] = "#{new_layout[:target]}/sol/#{_package_basename}-#{VERSION_NUMBER}.#{RELEASE_REVISION}-solaris.gz"
  new_layout[:zip_package] = "#{new_layout[:target]}/#{_package_basename}-#{VERSION_NUMBER}-#{RELEASE_REVISION}.zip"
  new_layout[:osx_package] = "#{new_layout[:target]}/osx_package/#{_package_basename}-#{VERSION_NUMBER}-#{RELEASE_REVISION}-osx.zip"
  new_layout
end

def submodule_layout_for_different_src(_module)
  layout = submodule_layout(_module)
  layout[:source] = "../#{_module}"
  layout
end

def port_open?(ip, port)
  begin
    TCPSocket.new(ip, port)
    return true
  rescue Errno::ECONNREFUSED
    return false
  end
end

def filter_files(from, to)
  mkpath to
  filter.from(from).into(to).run
end

def safe_cp from, to
  mkdir_p to
  cp_r from, to
end

module CopyHelper
  def copy_all_with_mode from_to_map
    from_to_map.each do |src, dest|
      (dest.is_a?(Array) ? dest : [dest]).each do |single_dest|
        copy_with_mode(src, single_dest)
      end
    end
  end

  def copy_with_mode src, dest
    dest.is_a?(String) && dest = {:to => dest}
    mkdir_p(File.dirname(dest[:to]), :mode => 0755)
    cp(src, dest[:to])
    dest.has_key?(:mode) && chmod(dest[:mode], dest[:to])
  end

  def dest_with_mode dest, mode = 0755
    {:to => dest, :mode => mode}
  end


end

module DistPackagingHelper
  def metadata_src_project= metadata_src
    @metadata_src = metadata_src
  end
end

module LinuxPackageHelper
  include DistPackagingHelper

  def linux_dir
    _(:target, "linux_package")
  end

  def linux_packaging
    File.join($PROJECT_BASE, "installers", "shared")
  end

  def shared_pre
    File.read(File.join(linux_packaging, "shared_pre.sh"))
  end

  def shared_post
    File.read(File.join(linux_packaging, "shared_post.sh"))
  end

  def gzip_changelog doc_dir
    rm_f "#{doc_dir}/changelog.gz"
    sh "/bin/gzip -v9 #{doc_dir}/changelog"
  end

  def sub_and_copy_with_mode from_dir, from_name, to_dir, to_name, substitutions = {}, mode = 0755
    filter.from(from_dir).include(from_name).into(to_dir).using(:ant, substitutions).run
    (from_name != to_name) && mv(_(to_dir, from_name), _(to_dir, to_name))
    chmod mode, _(to_dir, to_name)
  end
end

module DebPackageHelper
  include LinuxPackageHelper

  def shared_deb_pre
    File.read(File.join(linux_packaging, "shared_deb_pre.sh"))
  end

  def shared_deb_post
    File.read(File.join(linux_packaging, "shared_deb_post.sh"))
  end

  def shared_deb_pre_post
    File.read(File.join(linux_packaging, "shared_deb_pre_post.sh"))
  end

  def deb_ctrl_dir
    _(linux_dir, "DEBIAN")
  end

  def debian_metadata package, file_name = nil
    project(@metadata_src).path_to("../installers/#{package}/deb", file_name)
  end

  def dpkg_deb depend_on, package, package_name
    task :debian => depend_on do
      mkdir_p deb_ctrl_dir, :mode => 0755
      sub_and_copy_with_mode debian_metadata(package), "control.#{package_name}", deb_ctrl_dir, "control", {:VERSION => VERSION_NUMBER, :RELEASE => RELEASE_REVISION}, 0644
      sub_and_copy_with_mode debian_metadata(package), "preinst.#{package_name}", deb_ctrl_dir, "preinst", :shared => shared_pre, :shared_deb => shared_deb_pre, :pre_post => shared_deb_pre_post
      sub_and_copy_with_mode debian_metadata(package), "postinst.#{package_name}", deb_ctrl_dir, "postinst", :shared => shared_post, :pre_post => shared_deb_pre_post, :shared_deb => shared_deb_post
      copy_all_with_mode(_(debian_metadata(package), "postrm.#{package_name}") => dest_with_mode(_(deb_ctrl_dir, "postrm")),
                         _(debian_metadata(package), "prerm.#{package_name}") => dest_with_mode(_(deb_ctrl_dir, "prerm")),
                         _(debian_metadata(package), "conffiles.#{package_name}") => dest_with_mode(_(deb_ctrl_dir, "conffiles"), 0644))

      sh "fakeroot dpkg-deb --build #{linux_dir} #{_(:debian_package)}"
    end
  end
end

module SolarisPackageHelper
  include DistPackagingHelper

  def sol_prototype_file name_with_version
    _(sol_pkg_dir(name_with_version), "Prototype")
  end

  def sol_pkg_dir name_with_version
    _(sol_dir, name_with_version)
  end

  def sol_pkg_file name_with_version, file_name
    _(sol_pkg_dir(name_with_version), file_name)
  end

  def sol_dir
    _(:target, "sol")
  end

  def sol_new_line
    java.lang.System.getProperty("line.separator")
  end

  def sol_pkg_src package, file_name
    _(sol_pkg_src_dir(package), file_name)
  end

  def sol_pkg_src_dir package
    project(@metadata_src).path_to("../installers/#{package}/sol")
  end

  def sol_dist_path name
    "#{File.expand_path(sol_dir)}/go-#{name}-#{VERSION_NUMBER}.#{RELEASE_REVISION}-solaris"
  end

  def sol_build depend_on, package, name_with_version

    task :sol_prepare => :sol_clean do
      mkdir_p sol_dir
      mkdir_p sol_pkg_file(name_with_version, 'install')
      cp_r _(:target, "explode", name_with_version), sol_dir
    end

    task :sol_clean do
      rm_rf sol_dir
    end

    task :solaris => [depend_on, :sol_prepare] do
      File.open(sol_prototype_file(name_with_version), 'w') do |h|
        h.write ['pkginfo', 'copyright', 'depend', 'checkinstall',
                 'preinstall', 'postinstall', 'preremove', 'postremove'].inject('') { |inj, evt| "#{inj}i #{evt}#{sol_new_line}" }
      end

      ['svc.xml', "go-#{package}"].each do |file_name|
        cp sol_pkg_src(package, file_name), sol_pkg_dir(name_with_version)
      end

      ["#{package}.sh", "go-#{package}"].each do |file_name|
        sol_pkg_file = sol_pkg_file(name_with_version, file_name)
        puts "CHMOD 0755 for file #{sol_pkg_file}"
        FileUtils.chmod 0755, sol_pkg_file
      end

      cd(sol_pkg_dir(name_with_version)) do
        command = "/usr/bin/pkgproto .=go-#{package} >> #{sol_prototype_file(name_with_version)}"
        puts "Executing command #{command} from #{`pwd`}"
        sh command
      end

      user_name = `/usr/xpg4/bin/id -u -n`.strip
      user_group = `/usr/xpg4/bin/id -g -n`.strip

      content = File.read(sol_prototype_file(name_with_version))

      File.open(sol_prototype_file(name_with_version), 'w') do |h|
        h.write(content.gsub("f none go-#{package}/Prototype=Prototype 0666 #{user_name} #{user_group}\n", ''))
      end

      filter.from(sol_pkg_dir(name_with_version)).include("Prototype").into(sol_pkg_dir(name_with_version)).
              using(/\s#{user_name}\s#{user_group}$/, " #{user_name} #{user_group}" => ' root other').run

      filter.from(sol_pkg_src_dir(package)).into(sol_pkg_dir(name_with_version)).
              using(:ant, "VERSION" => "#{VERSION_NUMBER}.#{RELEASE_REVISION}").run

      cp project(@metadata_src).path_to('..', 'LICENSE'), sol_pkg_file(name_with_version, 'copyright')

      sh "cd #{sol_pkg_dir(name_with_version)} && /usr/bin/pkgmk -o -r . -d #{File.expand_path(sol_dir)} -f Prototype"

      sh "/usr/bin/pkgtrans -s #{File.expand_path(sol_dir)} #{sol_dist_path(package)} TWSgo-#{package}"

      sh "cd #{sol_dir} && gzip #{sol_dist_path(package)}"

      rm_rf _(sol_dir, "TWSgo-#{package}")
    end
  end
end

module RpmPackageHelper
  include LinuxPackageHelper

  def rpm_ctrl_dir
    _(linux_dir, "../RPMS")
  end

  def shared_rpm_pre
    File.read(File.join(linux_packaging, "shared_rpm_pre.sh"))
  end

  def shared_rpm_post
    File.read(File.join(linux_packaging, "shared_rpm_post.sh"))
  end

  def rpm_metadata package, file_name = nil
    project(@metadata_src).path_to("../installers/#{package}/rpm", file_name)
  end

  def rpm_build depend_on, package, package_name
    task :rpm => depend_on do
      mkdir_p rpm_ctrl_dir, :mode => 0755
      sub_and_copy_with_mode rpm_metadata(package), "#{package_name}.spec", rpm_ctrl_dir, "#{package_name}.spec", {:VERSION => VERSION_NUMBER, :RELEASE => RELEASE_REVISION, :ROOT => linux_dir, :pre => shared_pre, :post => shared_post, :rpm_pre => shared_rpm_pre, :rpm_post => shared_rpm_post}, 0644
      sh "fakeroot rpmbuild --buildroot #{linux_dir} --define '_rpmdir #{rpm_ctrl_dir}' --define '__spec_install_pre %{___build_pre}' -bb --target noarch #{rpm_ctrl_dir}/#{package_name}.spec"
      cp_r File.join(rpm_ctrl_dir, "noarch", "#{package_name}-#{VERSION_NUMBER}-#{RELEASE_REVISION}.noarch.rpm"), _(:redhat_package)
    end
  end
end

module WinPackageHelper
  include DistPackagingHelper

  def windows_dir
    _(:target, "windows_package")
  end

  def windows_dir_file file_name
    File.join(windows_dir, file_name)
  end

  def windows_dir_file_matching file_name
    Dir[windows_dir_file(file_name)].first
  end

  def win_pkg_src package, file_name
    project(@metadata_src).path_to("../installers/#{package}/win", file_name)
  end

  def win_pkg_dest file_name
    File.join(windows_dir, file_name)
  end

  def win_pkg_content_dir package_name
    File.join(windows_dir, "#{package_name}-#{VERSION_NUMBER}")
  end

  def win_pkg_content_dir_child package_name, file
    File.join(win_pkg_content_dir(package_name), file)
  end

  def copy_dir win, package, package_name, dir
    win.copy :todir => win_pkg_content_dir_child(package_name, dir) do |cp|
      cp.fileset :dir => win_pkg_src(package, dir), :includes => "**/*"
    end
  end

  def from_root file_name
    File.join(_('../..'), file_name)
  end

  def win_build depend_on, package, package_name, windows_name, windows_java
    task :exe => depend_on do
      cp win_pkg_src(package, 'cruisewrapper.exe'), win_pkg_content_dir(package_name)
      disable_logging_value = ENV['DISABLE_WIN_INSTALLER_LOGGING'] || "false"
      Buildr.ant('win') do |win|
        jre_tar_gz = ENV['WINDOWS_JRE_LOCATION'].match("jre-.*$").to_s
        jre_tar = jre_tar_gz.gsub(".gz", "")

        win.get :src => ENV['WINDOWS_JRE_LOCATION'], :dest => "#{_(:target)}/#{jre_tar_gz}"
        win.gunzip :src => "#{_(:target)}/#{jre_tar_gz}"
        win.untar :src => "#{_(:target)}/#{jre_tar}", :dest => windows_dir
        win.copy :file => from_root('LICENSE'), :tofile => win_pkg_dest("LICENSE.dos")
        win.fixcrlf :file => win_pkg_dest("LICENSE.dos"), :eol => "dos"
        copy_dir win, package, package_name, 'lib'
        copy_dir win, package, package_name, 'config'
        win.copy :file => from_root("build/icons/#{package_name}.ico"), :tofile => win_pkg_dest("#{package_name}.ico")
        win.exec(:executable => '/usr/bin/makensis', :failonerror => true, :dir => windows_dir) do |exec|
          exec.env :key => "BINARY_SOURCE_DIR", :value => win_pkg_content_dir(package_name)
          exec.env :key => "LIC_FILE", :value => win_pkg_dest("LICENSE.dos")
          exec.env :key => "NAME", :value => windows_name
          exec.env :key => "MODULE", :value => windows_name.downcase
          exec.env :key => "GO_ICON", :value => win_pkg_dest("#{package_name}.ico")
          exec.env :key => "VERSION", :value => "#{VERSION_NUMBER}-#{RELEASE_REVISION}"
          padded_release_revision = "#{RELEASE_REVISION}".rjust(5,'0')
          exec.env :key => "REGVER", :value => "#{VERSION_NUMBER}#{padded_release_revision}".gsub(/\./, '')
          exec.env :key => "JAVA", :value => windows_java
          exec.env :key => "JAVASRC", :value => windows_dir_file_matching("jre*")
          exec.env :key => "DISABLE_LOGGING", :value => disable_logging_value
          exec.arg :line => "-NOCD #{win_pkg_src(package, package_name + '.nsi')}"
        end
      end
    end
  end
end

module OSXPackageHelper
  def osx_dir
    _(:target, "osx_package")
  end

  def build_osx_installer(pkg, pkg_dir)
    go_pkg_name_with_release_revision = "#{"go-#{pkg}-#{VERSION_NUMBER}"}-#{RELEASE_REVISION}"

    cd(osx_dir) do
      contents_dir = create_dir_if_not_present(pkg_dir, "Contents")
      cp File.join("..", "..", "..", "..", "installers", pkg, "osx", "Info.plist"), contents_dir
      replace_content_in_file(File.join(contents_dir, "Info.plist"), "@VERSION@", "#{VERSION_NUMBER}.#{RELEASE_REVISION}")
      replace_content_in_file(File.join(contents_dir, "Info.plist"), "@REGVER@", "#{RELEASE_REVISION}")

      resources_dir = create_dir_if_not_present(contents_dir, "Resources")
      cp File.join("..", "..", "..", "..", "build", "icons", "go-#{pkg}.icns"), resources_dir

      mac_os_dir = create_dir_if_not_present(contents_dir, "MacOS")
      java_application_stub_64_file = File.join(mac_os_dir, "go-#{pkg}")
      cp File.join("..", "..", "..", "..", "build", "osx", "JavaApplicationStub64"), java_application_stub_64_file
      chmod 0755, java_application_stub_64_file

      system("zip -q -r -9 #{go_pkg_name_with_release_revision}-osx.zip \"#{pkg_dir}\"") || \
        (STDERR.puts "Failed to zip the OSX installer from #{pkg_dir}"; exit 1)
      rm_rf pkg_dir
    end
  end

  def replace_content_in_file file_name, pattern, replacement
    text = File.read(file_name)
    text.gsub!(pattern, replacement)
    File.open(file_name, 'w') {|file| file.puts text}
  end

  def create_dir_if_not_present path, dir
    dir_to_create = File.join(path, dir)
    mkdir_p dir_to_create unless File.directory? dir_to_create
    dir_to_create
  end
end


def in_root filename
  File.join($PROJECT_BASE, filename)
end

module WireupTestJVMArguments
  include Extension

  after_define do |project|
    jvm_args = (project.test.options[:java_args] ||= [])
    jvm_args << "-Xmx1024m"
    if ENV['DEBUG_TEST'] && ENV['DEBUG_TEST'].downcase.strip == 'y'
      puts "*****Enabling debug for real, get ready for some serious action!!!*****"
      jvm_args << "-Xdebug"
      jvm_args << "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"
    end
    jvm_args.uniq!
  end
end

class Buildr::Project
  include WireupTestJVMArguments
end
