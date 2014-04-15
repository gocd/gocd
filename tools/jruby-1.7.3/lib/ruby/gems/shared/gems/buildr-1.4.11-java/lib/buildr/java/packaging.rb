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
  module Packaging #:nodoc:

    # Adds packaging for Java projects: JAR, WAR, AAR, EAR, Javadoc.
    module Java

      class Manifest

        STANDARD_HEADER = { 'Manifest-Version'=>'1.0', 'Created-By'=>'Buildr' }
        LINE_SEPARATOR = /\r\n|\n|\r[^\n]/ #:nodoc:
        SECTION_SEPARATOR = /(#{LINE_SEPARATOR}){2}/ #:nodoc:

        class << self

          # :call-seq:
          #   parse(str) => manifest
          #
          # Parse a string in MANIFEST.MF format and return a new Manifest.
          def parse(str)
            sections = str.split(SECTION_SEPARATOR).reject { |s| s.strip.empty? }
            new sections.map { |section|
              lines = section.split(LINE_SEPARATOR).inject([]) { |merged, line|
                if line[/^ /] == ' '
                  merged.last << line[1..-1]
                else
                  merged << line
                end
                merged
              }
              lines.map { |line| line.scan(/(.*?):\s*(.*)/).first }.
                inject({}) { |map, (key, value)| map.merge(key=>value) }
            }
          end

          # :call-seq:
          #   from_zip(file) => manifest
          #
          # Parse the MANIFEST.MF entry of a ZIP (or JAR) file and return a new Manifest.
          def from_zip(file)
            Zip::ZipInputStream::open(file.to_s) do |zip|
              while (entry = zip.get_next_entry)
                if entry.name == 'META-INF/MANIFEST.MF'
                  return Manifest.parse zip.read
                end
              end
            end
            Manifest.new
          end

          # :call-seq:
          #   update_manifest(file) { |manifest| ... }
          #
          # Updates the MANIFEST.MF entry of a ZIP (or JAR) file.  Reads the MANIFEST.MF,
          # yields to the block with the Manifest object, and writes the modified object
          # back to the file.
          def update_manifest(file)
            manifest = from_zip(file)
            result = yield manifest
            Zip::ZipFile.open(file.to_s) do |zip|
              zip.get_output_stream('META-INF/MANIFEST.MF') do |out|
                out.write manifest.to_s
                out.write "\n"
              end
            end
            result
          end

        end

        # Returns a new Manifest object based on the argument:
        # * nil         -- Empty Manifest.
        # * Hash        -- Manifest with main section using the hash name/value pairs.
        # * Array       -- Manifest with one section from each entry (must be hashes).
        # * String      -- Parse (see Manifest#parse).
        # * Proc/Method -- New Manifest from result of calling proc/method.
        def initialize(arg = nil)
          case arg
          when nil, Hash then @sections = [arg || {}]
          when Array then @sections = arg
          when String then @sections = Manifest.parse(arg).sections
          when Proc, Method then @sections = Manifest.new(arg.call).sections
          else
            fail 'Invalid manifest, expecting Hash, Array, file name/task or proc/method.'
          end
          # Add Manifest-Version and Created-By, if not specified.
          STANDARD_HEADER.each do |name, value|
            sections.first[name] ||= value
          end
        end

        # The sections of this manifest.
        attr_reader :sections

        # The main (first) section of this manifest.
        def main
          sections.first
        end

        include Enumerable

        # Iterate over each section and yield to block.
        def each(&block)
          @sections.each(&block)
        end

        # Convert to MANIFEST.MF format.
        def to_s
          @sections.map { |section|
            keys = section.keys
            keys.unshift('Name') if keys.delete('Name')
            lines = keys.map { |key| manifest_wrap_at_72("#{key}: #{section[key]}") }
            lines + ['']
          }.flatten.join("\n")
        end

      private

        def manifest_wrap_at_72(line)
          return [line] if line.size < 72
          [ line[0..70] ] + manifest_wrap_at_72(' ' + line[71..-1])
        end

      end


      # Adds support for MANIFEST.MF and other META-INF files.
      module WithManifest #:nodoc:

        class << self
          def included(base)
            base.class_eval do
              alias :initialize_without_manifest :initialize
              alias :initialize :initialize_with_manifest
            end
          end

        end

        # Specifies how to create the manifest file.
        attr_accessor :manifest

        # Specifies files to include in the META-INF directory.
        attr_accessor :meta_inf

        def initialize_with_manifest(*args) #:nodoc:
          initialize_without_manifest *args
          @manifest = false
          @meta_inf = []
          @dependencies = FileList[]

          prepare do
            @prerequisites << manifest if String === manifest || Rake::Task === manifest
            [meta_inf].flatten.map { |file| file.to_s }.uniq.each { |file| path('META-INF').include file }
          end

          enhance do
            if manifest
              # Tempfiles gets deleted on garbage collection, so we're going to hold on to it
              # through instance variable not closure variable.
              @manifest_tmp = Tempfile.new('MANIFEST.MF')
              File.chmod 0644, @manifest_tmp.path
              self.manifest = File.read(manifest.to_s) if String === manifest || Rake::Task === manifest
              self.manifest = Manifest.new(manifest) unless Manifest === manifest
              #@manifest_tmp.write Manifest::STANDARD_HEADER
              @manifest_tmp.write manifest.to_s
              @manifest_tmp.write "\n"
              @manifest_tmp.close
              path('META-INF').include @manifest_tmp.path, :as=>'MANIFEST.MF'
            end
          end
        end

      end

      class ::Buildr::ZipTask
        include WithManifest
      end


      # Extends the ZipTask to create a JAR file.
      #
      # This task supports two additional attributes: manifest and meta-inf.
      #
      # The manifest attribute specifies how to create the MANIFEST.MF file.
      # * A hash of manifest properties (name/value pairs).
      # * An array of hashes, one for each section of the manifest.
      # * A string providing the name of an existing manifest file.
      # * A file task can be used the same way.
      # * Proc or method called to return the contents of the manifest file.
      # * False to not generate a manifest file.
      #
      # The meta-inf attribute lists one or more files that should be copied into
      # the META-INF directory.
      #
      # For example:
      #   package(:jar).with(:manifest=>'src/MANIFEST.MF')
      #   package(:jar).meta_inf << file('README')
      class JarTask < ZipTask

        def initialize(*args) #:nodoc:
          super
        end

        # :call-seq:
        #   with(options) => self
        #
        # Additional
        # Pass options to the task. Returns self. ZipTask itself does not support any options,
        # but other tasks (e.g. JarTask, WarTask) do.
        #
        # For example:
        #   package(:jar).with(:manifest=>'MANIFEST_MF')
        def with(*args)
          super args.pop if Hash === args.last
          fail "package.with() should not contain nil values" if args.include? nil
          include :from=>args if args.size > 0
          self
        end

      end


      # Extends the JarTask to create a WAR file.
      #
      # Supports all the same options as JarTask, in additon to these two options:
      # * :libs -- An array of files, tasks, artifact specifications, etc that will be added
      #   to the WEB-INF/lib directory.
      # * :classes -- A directory containing class files for inclusion in the WEB-INF/classes
      #   directory.
      #
      # For example:
      #   package(:war).with(:libs=>'log4j:log4j:jar:1.1')
      class WarTask < JarTask

        # Directories with class files to include under WEB-INF/classes.
        attr_accessor :classes

        # Artifacts to include under WEB-INF/libs.
        attr_accessor :libs

        def initialize(*args) #:nodoc:
          super
          @classes = []
          @libs = []
          enhance do |war|
            @libs.each {|lib| lib.invoke if lib.respond_to?(:invoke) }
            @classes.to_a.flatten.each { |classes| include classes, :as => 'WEB-INF/classes' }
            path('WEB-INF/lib').include Buildr.artifacts(@libs) unless @libs.nil? || @libs.empty?
          end
        end

        def libs=(value) #:nodoc:
          @libs = Buildr.artifacts(value)
        end

        def classes=(value) #:nodoc:
          @classes = [value].flatten.map { |dir| file(dir.to_s) }
        end

      end


      # Extends the JarTask to create an AAR file (Axis2 service archive).
      #
      # Supports all the same options as JarTask, with the addition of :wsdls, :services_xml and :libs.
      #
      # * :wsdls -- WSDL files to include (under META-INF).  By default packaging will include all WSDL
      #   files found under src/main/axis2.
      # * :services_xml -- Location of services.xml file (included under META-INF).  By default packaging
      #   takes this from src/main/axis2/services.xml.  Use a different path if you genereate the services.xml
      #   file as part of the build.
      # * :libs -- Array of files, tasks, artifact specifications, etc that will be added to the /lib directory.
      #
      # For example:
      #   package(:aar).with(:libs=>'log4j:log4j:jar:1.1')
      #
      #   filter.from('src/main/axis2').into('target').include('services.xml', '*.wsdl').using('http_port'=>'8080')
      #   package(:aar).wsdls.clear
      #   package(:aar).with(:services_xml=>_('target/services.xml'), :wsdls=>_('target/*.wsdl'))
      class AarTask < JarTask
        # Artifacts to include under /lib.
        attr_accessor :libs
        # WSDLs to include under META-INF (defaults to all WSDLs under src/main/axis2).
        attr_accessor :wsdls
        # Location of services.xml file (defaults to src/main/axis2/services.xml).
        attr_accessor :services_xml

        def initialize(*args) #:nodoc:
          super
          @libs = []
          @wsdls = []
          prepare do
            path('META-INF').include @wsdls
            path('META-INF').include @services_xml, :as=>'services.xml' if @services_xml
            path('lib').include Buildr.artifacts(@libs) unless @libs.nil? || @libs.empty?
          end
        end

        def libs=(value) #:nodoc:
          @libs = Buildr.artifacts(value)
        end

        def wsdls=(value) #:nodoc:
          @wsdls |= Array(value)
        end
      end


      # Extend the JarTask to create an EAR file.
      #
      # The following component types are supported by the EARTask:
      #
      # * :war -- A J2EE Web Application
      # * :ejb -- An Enterprise Java Bean
      # * :jar -- A J2EE Application Client.[1]
      # * :lib -- An ear scoped shared library[2] (for things like logging,
      #           spring, etc) common to the ear components
      #
      # The EarTask uses the "Mechanism 2: Bundled Optional Classes" as described on [2].
      # All specified libraries are added to the EAR archive and the Class-Path manifiest entry is
      # modified for each EAR component. Special care is taken with WebApplications, as they can
      # contain libraries on their WEB-INF/lib directory, libraries already included in a war file
      # are not referenced by the Class-Path entry of the war in order to avoid class collisions
      #
      # EarTask supports all the same options as JarTask, in additon to these two options:
      #
      # * :display_name -- The displayname to for this ear on application.xml
      #
      # * :map -- A Hash used to map component type to paths within the EAR.
      #     By default each component type is mapped to a directory with the same name,
      #     for example, EJBs are stored in the /ejb path.  To customize:
      #                       package(:ear).map[:war] = 'web-applications'
      #                       package(:ear).map[:lib] = nil # store shared libraries on root of archive
      #
      # EAR components are added by means of the EarTask#add, EarTask#<<, EarTask#push methods
      # Component type is determined from the artifact's type.
      #
      #      package(:ear) << project('coolWebService').package(:war)
      #
      # The << method is just an alias for push, with the later you can add multiple components
      # at the same time. For example..
      #
      #      package(:ear).push 'org.springframework:spring:jar:2.6',
      #                                   projects('reflectUtils', 'springUtils'),
      #                                   project('coolerWebService').package(:war)
      #
      # The add method takes a single component with an optional hash. You can use it to override
      # some component attributes.
      #
      # You can override the component type for a particular artifact. The following example
      # shows how you can tell the EarTask to treat a JAR file as an EJB:
      #
      #      # will add an ejb entry for the-cool-ejb-2.5.jar in application.xml
      #      package(:ear).add 'org.coolguys:the-cool-ejb:jar:2.5', :type=>:ejb
      #      # A better syntax for this is:
      #      package(:ear).add :ejb=>'org.coolguys:the-cool-ejb:jar:2.5'
      #
      # By default, every JAR package is assumed to be a library component, so you need to specify
      # the type when including an EJB (:ejb) or Application Client JAR (:jar).
      #
      # For WebApplications (:war)s, you can customize the context-root that appears in application.xml.
      # The following example also specifies a different directory inside the EAR where to store the webapp.
      #
      #      package(:ear).add project(:remoteService).package(:war),
      #                                 :path=>'web-services', :context_root=>'/Some/URL/Path'
      #
      # [1] http://java.sun.com/j2ee/sdk_1.2.1/techdocs/guides/ejb/html/Overview5.html#10106
      # [2] http://java.sun.com/j2ee/verified/packaging.html
      class EarTask < JarTask

        SUPPORTED_TYPES = [:war, :ejb, :jar, :rar, :lib]

        # The display-name entry for application.xml
        attr_accessor :display_name
        # The description entry for application.xml
        attr_accessor :description
        # Map from component type to path inside the EAR.
        attr_accessor :dirs
        # Security roles entry for application.xml
        attr_accessor :security_roles

        def initialize(*args)
          super
          @dirs = Hash.new { |h, k| k.to_s }
          @libs, @components, @security_roles = [], [], []
          prepare do
            @components.each do |component|
              path(component[:path]).include(component[:clone] || component[:artifact])
            end
            path('META-INF').include(descriptor)
          end
        end

        # Add an artifact to this EAR.
        def add(*args)
          options = Hash === args.last ? args.pop.clone : {}
          args.flatten!
          args.map! do |pkg|
            case pkg
            when Project
              pkg.packages.select { |pp| JarTask === pp && SUPPORTED_TYPES.include?(pp.type) }
            when Rake::FileTask
              pkg # add the explicitly provided file
            when Hash
              Buildr.artifact(pkg)
            when String
              begin
                Buildr.artifact(pkg)
              rescue # not an artifact spec, it must me a filename
                file(pkg)
              end
            else
              raise "Invalid EAR component #{pkg.class}: #{pkg}"
            end
          end
          args.flatten!
          args.compact!
          if args.empty?
            raise ":type must not be specified for type=>component argument style" if options.key?(:type)
            raise ":as must not be specified for type=>component argument style" if options.key?(:as)
            comps = {}
            options.delete_if { |k, v| comps[k] = v if SUPPORTED_TYPES.include?(k) }
            raise "You must specify at least one valid component to add" if comps.empty?
            comps.each { |k, v| add(v, {:as => k}.merge(options)) }
          else
            args.each do |artifact|
              type = options[:as] || options[:type]
              unless type
                type = artifact.respond_to?(:type) ? artifact.type : artifact.to_s.pathmap('%x').to_sym
                type = :lib if type == :jar
              end
              raise "Unknown EAR component type: #{type}. Perhaps you may explicity tell what component type to use." unless
                SUPPORTED_TYPES.include?(type)
              component = options.merge(:artifact => artifact, :type => type,
                :id=>artifact.respond_to?(:to_spec) ? artifact.id : artifact.to_s.pathmap('%n'),
                :path=>options[:path] || dirs[type].to_s)
              component[:clone] = component_clone(component) unless :lib == type
              # update_classpath(component) unless :lib == type || Artifact === artifact
              @components << component
            end
          end
          self
        end

        alias_method :push, :add
        alias_method :<<, :push

      protected

        def component_clone(component)
          file(path_to(component[:path], component[:artifact].to_s.pathmap('%f')) => component[:artifact]) do |task|
            mkpath task.to_s.pathmap('%d')
            cp component[:artifact].to_s, task.to_s
            Manifest.update_manifest(task) do |manifest|
              class_path = manifest.main['Class-Path'].to_s.split
              included_libs = class_path.map { |fn| fn.pathmap('%f') }
              Zip::ZipFile.foreach(task.to_s) do |entry|
                included_libs << entry.name.pathmap('%f') if entry.file? && entry.name =~ /^WEB-INF\/lib\/[^\/]+$/
              end
              # Include all other libraries in the classpath.
              class_path += libs_classpath(component).reject { |path| included_libs.include?(File.basename(path)) }
              manifest.main['Class-Path'] = class_path.join(' ')
            end
          end
        end

        def associate(project)
          @project = project
        end

        def path_to(*args) #:nodoc:
          @project.path_to(:target, :ear, name.pathmap('%n'), *args)
        end
        alias_method :_, :path_to

        def update_classpath(component)
          package = file(component[:artifact].to_s)
          package.manifest = (package.manifest || {}).dup # avoid mofifying parent projects manifest
          package.prepare do
            header = case package.manifest
              when Hash then package.manifest
              when Array then package.manifest.first
            end
            if header
              # Determine which libraries are already included.
              class_path = header['Class-Path'].to_s.split
              included_libs = class_path.map { |fn| File.basename(fn) }
              included_libs += package.path('WEB-INF/lib').sources.map { |fn| File.basename(fn) }
              # Include all other libraries in the classpath.
              class_path += libs_classpath(component).reject { |path| included_libs.include?(File.basename(path)) }
              header['Class-Path'] = class_path.join(' ')
            end
          end
        end

      private

        # Classpath of all packages included as libraries (type :lib).
        def libs_classpath(component)
          from = component[:path]
          @classpath = @components.select { |comp| comp[:type] == :lib }.
            map do |lib|
            basename = lib[:artifact].to_s.pathmap('%f')
            full_path = lib[:path].empty? ? basename : File.join(lib[:path], basename)
            Util.relative_path(full_path, from)
          end
        end

        def descriptor_xml
          buffer = ""
          xml = Builder::XmlMarkup.new(:target=>buffer, :indent => 2)
          xml.declare! :DOCTYPE, :application, :PUBLIC,
          "-//Sun Microsystems, Inc.//DTD J2EE Application 1.2//EN",
          "http://java.sun.com/j2ee/dtds/application_1_2.dtd"
          xml.application do
            xml.tag! 'display-name', display_name
            desc = self.description || @project.comment
            xml.tag! 'description', desc if desc
            @components.each do |comp|
              basename = comp[:artifact].to_s.pathmap('%f')
              uri = comp[:path].empty? ? basename : File.join(comp[:path], basename)
              case comp[:type]
              when :war
                xml.module :id=>comp[:id] do
                  xml.web do
                    xml.tag! 'web-uri', uri
                    xml.tag! 'context-root', File.join('', (comp[:context_root] || comp[:id])) unless comp[:context_root] == false
                  end
                end
              when :ejb
                xml.module :id=>comp[:id] do
                  xml.ejb uri
                end
              when :jar
                xml.jar uri
              end
            end
            @security_roles.each do |role|
              xml.tag! 'security-role', :id=>role[:id] do
                xml.description role[:description]
                xml.tag! 'role-name', role[:name]
              end
            end
          end
          buffer
        end

        # return a FileTask to build the ear application.xml file
        def descriptor
          return @descriptor if @descriptor
          descriptor_path = path_to('META-INF/application.xml')
          @descriptor = file(descriptor_path) do |task|
            trace "Creating EAR Descriptor: #{task.to_s}"
            mkpath File.dirname(task.name)
            File.open(task.name, 'w') { |file| file.print task.xml }
          end
          class << @descriptor
            attr_accessor :ear

            def xml
              @xml ||= ear.send :descriptor_xml
            end

            def needed?
              super || xml != File.read(self.to_s) rescue true
            end
          end
          @descriptor.ear = self
          @descriptor
        end

      end


      include Extension

      before_define(:package => :build) do |project|
        if project.parent && project.parent.manifest
          project.manifest = project.parent.manifest.dup
        else
          project.manifest = {
            'Build-By'=>ENV['USER'], 'Build-Jdk'=>ENV_JAVA['java.version'],
            'Implementation-Title'=>project.comment || project.name,
            'Implementation-Version'=>project.version }
        end
        if project.parent && project.parent.meta_inf
          project.meta_inf = project.parent.meta_inf.dup
        else
          project.meta_inf = [project.file('LICENSE')].select { |file| File.exist?(file.to_s) }
        end
      end


      # Manifest used for packaging. Inherited from parent project. The default value is a hash that includes
      # the Build-By, Build-Jdk, Implementation-Title and Implementation-Version values.
      # The later are taken from the project's comment (or name) and version number.
      attr_accessor :manifest

      # Files to always include in the package META-INF directory. The default value include
      # the LICENSE file if one exists in the project's base directory.
      attr_accessor :meta_inf

      # :call-seq:
      #   package_with_sources(options?)
      #
      # Call this when you want the project (and all its sub-projects) to create a source distribution.
      # You can use the source distribution in an IDE when debugging.
      #
      # A source distribution is a jar package with the classifier 'sources', which includes all the
      # sources used by the compile task.
      #
      # Packages use the project's manifest and meta_inf properties, which you can override by passing
      # different values (e.g. false to exclude the manifest) in the options.
      #
      # To create source distributions only for specific projects, use the :only and :except options,
      # for example:
      #   package_with_sources :only=>['foo:bar', 'foo:baz']
      #
      # (Same as calling package :sources on each project/sub-project that has source directories.)
      def package_with_sources(options = nil)
        options ||= {}
        enhance do
          selected = options[:only] ? projects(options[:only]) :
            options[:except] ? ([self] + projects - projects(options[:except])) :
            [self] + projects
          selected.reject { |project| project.compile.sources.empty? && project.resources.target.nil? }.
            each { |project| project.package(:sources) }
        end
      end

      # :call-seq:
      #   package_with_javadoc(options?)
      #
      # Call this when you want the project (and all its sub-projects) to create a JavaDoc distribution.
      # You can use the JavaDoc distribution in an IDE when coding against the API.
      #
      # A JavaDoc distribution is a ZIP package with the classifier 'javadoc', which includes all the
      # sources used by the compile task.
      #
      # Packages use the project's manifest and meta_inf properties, which you can override by passing
      # different values (e.g. false to exclude the manifest) in the options.
      #
      # To create JavaDoc distributions only for specific projects, use the :only and :except options,
      # for example:
      #   package_with_javadoc :only=>['foo:bar', 'foo:baz']
      #
      # (Same as calling package :javadoc on each project/sub-project that has source directories.)
      def package_with_javadoc(options = nil)
        options ||= {}
        enhance do
          selected = options[:only] ? projects(options[:only]) :
            options[:except] ? ([self] + projects - projects(options[:except])) :
            [self] + projects
          selected.reject { |project| project.compile.sources.empty? }.
            each { |project| project.package(:javadoc) }
        end
      end

    protected

      def package_as_jar(file_name) #:nodoc:
        Java::JarTask.define_task(file_name).tap do |jar|
          jar.with :manifest=>manifest, :meta_inf=>meta_inf
          jar.with [compile.target, resources.target].compact
        end
      end

      def package_as_war(file_name) #:nodoc:
        Java::WarTask.define_task(file_name).tap do |war|
          war.with :manifest=>manifest, :meta_inf=>meta_inf
          # Add libraries in WEB-INF lib, and classes in WEB-INF classes
          war.with :classes=>[compile.target, resources.target].compact
          war.with :libs=>compile.dependencies
          # Add included files, or the webapp directory.
          webapp = path_to(:source, :main, :webapp)
          war.with webapp if File.exist?(webapp)
        end
      end

      def package_as_aar(file_name) #:nodoc:
        Java::AarTask.define_task(file_name).tap do |aar|
          aar.with :manifest=>manifest, :meta_inf=>meta_inf
          aar.with :wsdls=>path_to(:source, :main, :axis2, '*.wsdl')
          aar.with :services_xml=>path_to(:source, :main, :axis2, 'services.xml')
          aar.with [compile.target, resources.target].compact
          aar.with :libs=>compile.dependencies
        end
      end

      def package_as_ear(file_name) #:nodoc:
        Java::EarTask.define_task(file_name).tap do |ear|
          ear.send :associate, self
          ear.with :display_name=>id, :manifest=>manifest, :meta_inf=>meta_inf
        end
      end

      def package_as_javadoc_spec(spec) #:nodoc:
        spec.merge(:type=>:jar, :classifier=>'javadoc')
      end

      def package_as_javadoc(file_name) #:nodoc:
        ZipTask.define_task(file_name).tap do |zip|
          zip.include :from=>doc.target
        end
      end

    end

  end
end


class Buildr::Project
  include Buildr::Packaging::Java
end
