# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.

module Buildr
  # Methods added to Project to support packaging and tasks for packaging,
  # installing and uploading packages.
  module Package

    include Extension

    first_time do
      desc 'Create packages'
      Project.local_task('package'=>'build') { |name| "Packaging #{name}" }
      desc 'Install packages created by the project'
      Project.local_task('install'=>'package') { |name| "Installing packages from #{name}" }
      desc 'Remove previously installed packages'
      Project.local_task('uninstall') { |name| "Uninstalling packages from #{name}" }
      desc 'Upload packages created by the project'
      Project.local_task('upload'=>'package') { |name| "Deploying packages from #{name}" }
      # Anything that comes after local packaging (install, upload) executes the integration tests,
      # which do not conflict with integration invoking the project's own packaging (package=>
      # integration=>foo:package is not circular, just confusing to debug.)
      task 'package' do
        task('integration').invoke if Buildr.options.test && Buildr.application.original_dir == Dir.pwd
      end
    end

    before_define(:package => :build) do |project|
      [ :package, :install, :uninstall, :upload ].each { |name| project.recursive_task name }
      # Need to run build before package, since package is often used as a dependency by tasks that
      # expect build to happen.
      project.task('package'=>project.task('build'))
      project.group ||= project.parent && project.parent.group || project.name
      project.version ||= project.parent && project.parent.version
    end

    after_define(:package)

    # The project's identifier. Same as the project name, with colons replaced by dashes.
    # The ID for project foo:bar is foo-bar.
    def id
      name.gsub(':', '-')
    end

    # Group used for packaging. Inherited from parent project. Defaults to the top-level project name.
    attr_accessor :group

    # Version used for packaging. Inherited from parent project.
    attr_accessor :version

    # :call-seq:
    #   package(type, spec?) => task
    #
    # Defines and returns a package created by this project.
    #
    # The first argument declares the package type. For example, :jar to create a JAR file.
    # The package is an artifact that takes its artifact specification from the project.
    # You can override the artifact specification by passing various options in the second
    # argument, for example:
    #   package(:zip, :classifier=>'sources')
    #
    # Packages that are ZIP files provides various ways to include additional files, directories,
    # and even merge ZIPs together. Have a look at ZipTask for more information. In case you're
    # wondering, JAR and WAR packages are ZIP files.
    #
    # You can also enhance a JAR package using the ZipTask#with method that accepts the following options:
    # * :manifest -- Specifies how to create the MANIFEST.MF. By default, uses the project's
    #   #manifest property.
    # * :meta_inf -- Specifies files to be included in the META-INF directory. By default,
    #   uses the project's #meta-inf property.
    #
    # The WAR package supports the same options and adds a few more:
    # * :classes -- Directories of class files to include in WEB-INF/classes. Includes the compile
    #   target directory by default.
    # * :libs -- Artifacts and files to include in WEB-INF/libs. Includes the compile classpath
    #   dependencies by default.
    #
    # For example:
    #   define 'project' do
    #     define 'beans' do
    #       package :jar
    #     end
    #     define 'webapp' do
    #       compile.with project('beans')
    #       package(:war).with :libs=>MYSQL_JDBC
    #     end
    #     package(:zip, :classifier=>'sources').include path_to('.')
    #  end
    #
    # Two other packaging types are:
    # * package :sources -- Creates a JAR file with the source code and classifier 'sources', for use by IDEs.
    # * package :javadoc -- Creates a ZIP file with the Javadocs and classifier 'javadoc'. You can use the
    #   javadoc method to further customize it.
    #
    # A package is also an artifact. The following tasks operate on packages created by the project:
    #   buildr upload     # Upload packages created by the project
    #   buildr install    # Install packages created by the project
    #   buildr package    # Create packages
    #   buildr uninstall  # Remove previously installed packages
    #
    # If you want to add additional packaging types, implement a method with the name package_as_[type]
    # that accepts a file name and returns an appropriate Rake task.  For example:
    #   def package_as_zip(file_name) #:nodoc:
    #     ZipTask.define_task(file_name)
    #   end
    #
    # The file name is determined from the specification passed to the package method, however, some
    # packagers need to override this.  For example, package(:sources) produces a file with the extension
    # 'jar' and the classifier 'sources'.  If you need to overwrite the default implementation, you should
    # also include a method named package_as_[type]_spec.  For example:
    #   def package_as_sources_spec(spec) #:nodoc:
    #     # Change the source distribution to .zip extension
    #     spec.merge({ :type=>:zip, :classifier=>'sources' })
    #   end
    def package(*args)
      spec = Hash === args.last ? args.pop.dup : {}
      no_options = spec.empty? # since spec is mutated
      if spec[:file]
        rake_check_options spec, :file, :type
        spec[:type] = args.shift || spec[:type] || spec[:file].split('.').last.to_sym
        file_name = spec[:file]
      else
        rake_check_options spec, *ActsAsArtifact::ARTIFACT_ATTRIBUTES
        spec[:id] ||= self.id
        spec[:group] ||= self.group
        spec[:version] ||= self.version
        spec[:type] = args.shift || spec[:type] || compile.packaging || :zip
      end

      packager = method("package_as_#{spec[:type]}") rescue fail("Don't know how to create a package of type #{spec[:type]}")
      if packager.arity == 1
        unless file_name
          spec = send("package_as_#{spec[:type]}_spec", spec) if respond_to?("package_as_#{spec[:type]}_spec")
          file_name = path_to(:target, Artifact.hash_to_file_name(spec))
        end
        package = (no_options && packages.detect { |pkg| pkg.type == spec[:type] && (pkg.id.nil? || pkg.id == spec[:id]) &&
          (pkg.respond_to?(:classifier) ? pkg.classifier : nil) == spec[:classifier]}) ||
          packages.find { |pkg| pkg.name == file_name } ||
          packager.call(file_name)
      else
        Buildr.application.deprecated "We changed the way package_as methods are implemented.  See the package method documentation for more details."
        file_name ||= path_to(:target, Artifact.hash_to_file_name(spec))
        package = packager.call(file_name, spec)
      end

      # First time: prepare package for install, uninstall and upload tasks.
      unless packages.include?(package)
        # We already run build before package, but we also need to do so if the package itself is
        # used as a dependency, before we get to run the package task.
        task 'package'=>package
        package.enhance [task('build')]
        package.enhance { info "Packaging #{File.basename(file_name)}" }
        if spec[:file]
          class << package ; self ; end.send(:define_method, :type) { spec[:type] }
          class << package ; self ; end.send(:define_method, :id) { nil }
        else
          # Make it an artifact using the specifications, and tell it how to create a POM.
          package.extend ActsAsArtifact
          package.send :apply_spec, spec.only(*Artifact::ARTIFACT_ATTRIBUTES)

          # Create pom associated with package
          class << package
            def pom
              unless @pom
                pom_filename = Util.replace_extension(self.name, 'pom')
                spec = {:group=>group, :id=>id, :version=>version, :type=>:pom}
                @pom = Buildr.artifact(spec, pom_filename)
                @pom.content @pom.pom_xml
              end
              @pom
            end
          end

          file(Buildr.repositories.locate(package)=>package) { package.install }

          # Add the package to the list of packages created by this project, and
          # register it as an artifact. The later is required so if we look up the spec
          # we find the package in the project's target directory, instead of finding it
          # in the local repository and attempting to install it.
          Artifact.register package, package.pom
        end

        task('install')   { package.install if package.respond_to?(:install) }
        task('uninstall') { package.uninstall if package.respond_to?(:uninstall) }
        task('upload')    { package.upload if package.respond_to?(:upload) }

        packages << package
      end
      package
    end

    # :call-seq:
    #   packages => tasks
    #
    # Returns all packages created by this project. A project may create any number of packages.
    #
    # This method is used whenever you pass a project to Buildr#artifact or any other method
    # that accepts artifact specifications and projects. You can use it to list all packages
    # created by the project. If you want to return a specific package, it is often more
    # convenient to call #package with the type.
    def packages
      @packages ||= []
    end

  protected

    def package_as_zip(file_name) #:nodoc:
      ZipTask.define_task(file_name)
    end

    def package_as_tar(file_name) #:nodoc:
      TarTask.define_task(file_name)
    end
    alias :package_as_tgz :package_as_tar

    def package_as_sources_spec(spec) #:nodoc:
      spec.merge(:type=>:jar, :classifier=>'sources')
    end

    def package_as_sources(file_name) #:nodoc:
      ZipTask.define_task(file_name).tap do |zip|
        zip.include :from=>[compile.sources, resources.target].compact
      end
    end

  end
end

class Buildr::Project
  include Buildr::Package
end
