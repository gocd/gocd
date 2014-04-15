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
  module IntellijIdea
    def self.new_document(value)
      REXML::Document.new(value, :attribute_quote => :quote)
    end

    # Abstract base class for IdeaModule and IdeaProject
    class IdeaFile
      DEFAULT_SUFFIX = ""
      DEFAULT_LOCAL_REPOSITORY_ENV_OVERRIDE = "MAVEN_REPOSITORY"

      attr_reader :buildr_project
      attr_writer :suffix
      attr_writer :id
      attr_accessor :template
      attr_accessor :local_repository_env_override

      def initialize
        @local_repository_env_override = DEFAULT_LOCAL_REPOSITORY_ENV_OVERRIDE
      end

      def suffix
        @suffix ||= DEFAULT_SUFFIX
      end

      def filename
        buildr_project.path_to("#{name}.#{extension}")
      end

      def id
        @id ||= buildr_project.name.split(':').last
      end

      def add_component(name, attrs = {}, &xml)
        self.components << create_component(name, attrs, &xml)
      end

      # IDEA can not handle text content with indents so need to removing indenting
      # Can not pass true as third argument as the ruby library seems broken
      def write(f)
        document.write(f, -1, false, true)
      end

      protected

      def name
        "#{self.id}#{suffix}"
      end

      def relative(path)
        ::Buildr::Util.relative_path(File.expand_path(path.to_s), self.base_directory)
      end

      def base_directory
        buildr_project.path_to
      end

      def resolve_path_from_base(path, base_variable)
        m2repo = Buildr::Repositories.instance.local
        if path.to_s.index(m2repo) == 0 && !self.local_repository_env_override.nil?
          return path.sub(m2repo, "$#{self.local_repository_env_override}$")
        else
          begin
            return "#{base_variable}/#{relative(path)}"
          rescue ArgumentError
            # ArgumentError happens on windows when self.base_directory and path are on different drives
            return path
          end
        end
      end

      def file_path(path)
        "file://#{resolve_path(path)}"
      end

      def create_component(name, attrs = {})
        target = StringIO.new
        Builder::XmlMarkup.new(:target => target, :indent => 2).component({:name => name}.merge(attrs)) do |xml|
          yield xml if block_given?
        end
        Buildr::IntellijIdea.new_document(target.string).root
      end

      def components
        @components ||= []
      end

      def create_composite_component(name, components)
        return nil if components.empty?
        component = self.create_component(name)
        components.each do |element|
          element = element.call if element.is_a?(Proc)
          component.add_element element
        end
        component
      end

      def add_to_composite_component(components)
        components << lambda do
          target = StringIO.new
          yield Builder::XmlMarkup.new(:target => target, :indent => 2)
          Buildr::IntellijIdea.new_document(target.string).root
        end
      end

      def load_document(filename)
        Buildr::IntellijIdea.new_document(File.read(filename))
      end

      def document
        if File.exist?(self.filename)
          doc = load_document(self.filename)
        else
          doc = base_document
          inject_components(doc, self.initial_components)
        end
        if self.template
          template_doc = load_document(self.template)
          REXML::XPath.each(template_doc, "//component") do |element|
            inject_component(doc, element)
          end
        end
        inject_components(doc, self.default_components.compact + self.components)

        # Sort the components in the same order the idea sorts them
        sorted = doc.root.get_elements('//component').sort { |s1, s2| s1.attribute('name').value <=> s2.attribute('name').value }
        doc = base_document
        sorted.each do |element|
          doc.root.add_element element
        end

        doc
      end

      def inject_components(doc, components)
        components.each do |component|
          # execute deferred components
          component = component.call if Proc === component
          inject_component(doc, component) if component
        end
      end

      # replace overridden component (if any) with specified component
      def inject_component(doc, component)
        doc.root.delete_element("//component[@name='#{component.attributes['name']}']")
        doc.root.add_element component
      end
    end

    # IdeaModule represents an .iml file
    class IdeaModule < IdeaFile
      DEFAULT_TYPE = "JAVA_MODULE"

      attr_accessor :type
      attr_accessor :group
      attr_reader :facets
      attr_writer :jdk_version

      def initialize
        super()
        @type = DEFAULT_TYPE
      end

      def buildr_project=(buildr_project)
        @id = nil
        @facets = []
        @skip_content = false
        @buildr_project = buildr_project
      end

      def jdk_version
        @jdk_version || buildr_project.compile.options.source || "1.6"
      end

      def extension
        "iml"
      end

      def main_source_directories
        @main_source_directories ||= [
          buildr_project.compile.sources,
          buildr_project.resources.sources
        ].flatten.compact
      end

      def test_source_directories
        @test_source_directories ||= [
          buildr_project.test.compile.sources,
          buildr_project.test.resources.sources
        ].flatten.compact
      end

      def excluded_directories
        @excluded_directories ||= [
          buildr_project.resources.target,
          buildr_project.test.resources.target,
          buildr_project.path_to(:target, :main),
          buildr_project.path_to(:target, :test),
          buildr_project.path_to(:reports)
        ].flatten.compact
      end

      attr_writer :main_output_dir

      def main_output_dir
        @main_output_dir ||= buildr_project._(:target, :main, :idea, :classes)
      end

      attr_writer :test_output_dir

      def test_output_dir
        @test_output_dir ||= buildr_project._(:target, :test, :idea, :classes)
      end

      def main_dependencies
        @main_dependencies ||=  buildr_project.compile.dependencies
      end

      def test_dependencies
        @test_dependencies ||=  buildr_project.test.compile.dependencies
      end

      def add_facet(name, type)
        add_to_composite_component(self.facets) do |xml|
          xml.facet(:name => name, :type => type) do |xml|
            yield xml if block_given?
          end
        end
      end

      def skip_content?
        !!@skip_content
      end

      def skip_content!
        @skip_content = true
      end

      def add_gwt_facet(modules = {}, options = {})
        name = options[:name] || "GWT"
        settings =
          {
            :webFacet => "Web",
            :compilerMaxHeapSize => "512",
            :compilerParameters => "-draftCompile -localWorkers 2 -strict",
            :gwtSdkUrl => "file://$GWT_TOOLS$",
            :gwtScriptOutputStyle => "PRETTY"
          }.merge(options[:settings] || {})

        add_facet(name, "gwt") do |f|
          f.configuration do |c|
            settings.each_pair do |k, v|
              c.setting :name => k.to_s, :value => v.to_s
            end
            c.packaging do |d|
              modules.each_pair do |k, v|
                d.module :name => k, :enabled => v
              end
            end
          end
        end
      end

      def add_web_facet(options = {})
        name = options[:name] || "Web"
        url_base = options[:url_base] || "/"
        default_webroots = [buildr_project._(:source, :main, :webapp)]
        webroots = options[:webroots] || default_webroots
        default_web_xml = "#{buildr_project._(:source, :main, :webapp)}/WEB-INF/web.xml"
        web_xml = options[:web_xml] || default_web_xml
        version = options[:version] || "3.0"

        add_facet(name, "web") do |f|
          f.configuration do |c|
            c.descriptors do |d|
              if File.exist?(web_xml) || default_web_xml != web_xml
                d.deploymentDescriptor :name => 'web.xml', :url => file_path(web_xml), :optional => "true", :version => version
              end
            end
            c.webroots do |w|
              webroots.each do |webroot|
                w.root :url => file_path(webroot), :relative => url_base
              end
            end
          end
        end
      end

      def add_jruby_facet(options = {})
        name = options[:name] || "JRuby"
        jruby_version = options[:jruby_version] || "jruby-1.5.2-p249"
        add_facet(name, "JRUBY") do |f|
          f.configuration(:number => 0) do |c|
            c.JRUBY_FACET_CONFIG_ID :NAME => "JRUBY_SDK_NAME", :VALUE => jruby_version
          end
        end
      end

      def add_jpa_facet(options = {})
        name = options[:name] || "JPA"
        factory_entry = options[:factory_entry] || buildr_project.name.to_s
        validation_enabled = options[:validation_enabled].nil? ? true : options[:validation_enabled]
        provider_enabled = options[:provider_enabled] || 'Hibernate'
        default_persistence_xml = buildr_project._(:source, :main, :resources, "META-INF/persistence.xml")
        persistence_xml = options[:persistence_xml] || default_persistence_xml
        default_orm_xml = buildr_project._(:source, :main, :resources, "META-INF/orm.xml")
        orm_xml = options[:orm_xml] || default_orm_xml
        add_facet(name, "jpa") do |f|
          f.configuration do |c|
            c.setting :name => "validation-enabled", :value => validation_enabled
            c.setting :name => "provider-name", :value => provider_enabled
            c.tag!('datasource-mapping') do |ds|
              ds.tag!('factory-entry', :name => factory_entry)
            end
            if File.exist?(persistence_xml) || default_persistence_xml != persistence_xml
              c.deploymentDescriptor :name => 'persistence.xml', :url => file_path(persistence_xml)
            end
            if File.exist?(orm_xml) || default_orm_xml != orm_xml
              c.deploymentDescriptor :name => 'orm.xml', :url => file_path(orm_xml)
            end
          end
        end
      end

      def add_ejb_facet(options = {})
        name = options[:name] || "EJB"
        default_ejb_xml = buildr_project._(:source, :main, :resources, "WEB-INF/ejb-jar.xml")
        ejb_xml = options[:ejb_xml] || default_ejb_xml
        ejb_roots = options[:ejb_roots] || [buildr_project.compile.sources, buildr_project.resources.sources].flatten

        add_facet(name, "ejb") do |facet|
          facet.configuration do |c|
            c.descriptors do |d|
              if File.exist?(ejb_xml) || default_ejb_xml != ejb_xml
                d.deploymentDescriptor :name => 'ejb-jar.xml', :url => file_path(ejb_xml)
              end
            end
            c.ejbRoots do |e|
              ejb_roots.each do |ejb_root|
                e.root :url => file_path(ejb_root)
              end
            end
          end
        end
      end

      protected

      def test_dependency_details
        main_dependencies_paths = main_dependencies.map(&:to_s)
        target_dir = buildr_project.compile.target.to_s
        test_dependencies.select { |d| d.to_s != target_dir }.collect do |d|
          dependency_path = d.to_s
          export = main_dependencies_paths.include?(dependency_path)
          source_path = nil
          if d.respond_to?(:to_spec_hash)
            source_spec = d.to_spec_hash.merge(:classifier => 'sources')
            source_path = Buildr.artifact(source_spec).to_s
            source_path = nil unless File.exist?(source_path)
          end
          [dependency_path, export, source_path]
        end
      end

      def base_document
        target = StringIO.new
        Builder::XmlMarkup.new(:target => target).module(:version => "4", :relativePaths => "true", :type => self.type)
        Buildr::IntellijIdea.new_document(target.string)
      end

      def initial_components
        []
      end

      def default_components
        [
          lambda { module_root_component },
          lambda { facet_component }
        ]
      end

      def facet_component
        create_composite_component("FacetManager", self.facets)
      end

      def module_root_component
        create_component("NewModuleRootManager", "inherit-compiler-output" => "false") do |xml|
          generate_compile_output(xml)
          generate_content(xml) unless skip_content?
          generate_initial_order_entries(xml)
          project_dependencies = []


          self.test_dependency_details.each do |dependency_path, export, source_path|
            next unless export
            generate_lib(xml, dependency_path, export, source_path, project_dependencies)
          end

          self.test_dependency_details.each do |dependency_path, export, source_path|
            next if export
            generate_lib(xml, dependency_path, export, source_path, project_dependencies)
          end

          xml.orderEntryProperties
        end
      end

      def generate_lib(xml, dependency_path, export, source_path, project_dependencies)
        project_for_dependency = Buildr.projects.detect do |project|
          [project.packages, project.compile.target, project.resources.target, project.test.compile.target, project.test.resources.target].flatten.
            detect { |artifact| artifact.to_s == dependency_path }
        end
        if project_for_dependency
          if project_for_dependency.iml? &&
            !project_dependencies.include?(project_for_dependency) &&
            project_for_dependency != self.buildr_project
            generate_project_dependency(xml, project_for_dependency.iml.name, export, !export)
          end
          project_dependencies << project_for_dependency
        else
          generate_module_lib(xml, url_for_path(dependency_path), export, (source_path ? url_for_path(source_path) : nil), !export)
        end
      end

      def jar_path(path)
        "jar://#{resolve_path(path)}!/"
      end

      def url_for_path(path)
        if path =~ /jar$/i
          jar_path(path)
        else
          file_path(path)
        end
      end

      def resolve_path(path)
        resolve_path_from_base(path, "$MODULE_DIR$")
      end

      def generate_compile_output(xml)
        xml.output(:url => file_path(self.main_output_dir.to_s))
        xml.tag!("output-test", :url => file_path(self.test_output_dir.to_s))
        xml.tag!("exclude-output")
      end

      def generate_content(xml)
        xml.content(:url => "file://$MODULE_DIR$") do
          # Source folders
          {
            :main => self.main_source_directories,
            :test => self.test_source_directories
          }.each do |kind, directories|
            directories.map { |dir| dir.to_s }.compact.sort.uniq.each do |dir|
              xml.sourceFolder :url => file_path(dir), :isTestSource => (kind == :test ? 'true' : 'false')
            end
          end

          # Exclude target directories
          self.net_excluded_directories.
            collect { |dir| file_path(dir) }.
            select { |dir| relative_dir_inside_dir?(dir) }.
            sort.each do |dir|
            xml.excludeFolder :url => dir
          end
        end
      end

      def relative_dir_inside_dir?(dir)
        !dir.include?("../")
      end

      def generate_initial_order_entries(xml)
        xml.orderEntry :type => "sourceFolder", :forTests => "false"
        xml.orderEntry :type => "jdk", :jdkName => jdk_version, :jdkType => "JavaSDK"
      end

      def generate_project_dependency(xml, other_project, export, test = false)
        attribs = {:type => 'module', "module-name" => other_project}
        attribs[:exported] = '' if export
        attribs[:scope] = 'TEST' if test
        xml.orderEntry attribs
      end

      def generate_module_lib(xml, path, export, source_path, test = false)
        attribs = {:type => 'module-library'}
        attribs[:exported] = '' if export
        attribs[:scope] = 'TEST' if test
        xml.orderEntry attribs do
          xml.library do
            xml.CLASSES do
              xml.root :url => path
            end
            xml.JAVADOC
            xml.SOURCES do
              if source_path
                xml.root :url => source_path
              end
            end
          end
        end
      end

      # Don't exclude things that are subdirectories of other excluded things
      def net_excluded_directories
        net = []
        all = self.excluded_directories.map { |dir| buildr_project._(dir.to_s) }.sort_by { |d| d.size }
        all.each_with_index do |dir, i|
          unless all[0 ... i].find { |other| dir =~ /^#{other}/ }
            net << dir
          end
        end
        net
      end
    end

    # IdeaModule represents an .ipr file
    class IdeaProject < IdeaFile
      attr_accessor :vcs
      attr_accessor :extra_modules
      attr_accessor :artifacts
      attr_accessor :configurations
      attr_writer :jdk_version

      def initialize(buildr_project)
        super()
        @buildr_project = buildr_project
        @vcs = detect_vcs
        @extra_modules = []
        @artifacts = []
        @configurations = []
      end

      def jdk_version
        @jdk_version ||= buildr_project.compile.options.source || "1.6"
      end

      def add_artifact(name, type, build_on_make = false)
        add_to_composite_component(self.artifacts) do |xml|
          xml.artifact(:name => name, :type => type, :"build-on-make" => build_on_make) do |xml|
            yield xml if block_given?
          end
        end
      end

      def add_configuration(name, type, factory_name, default = false)
        add_to_composite_component(self.configurations) do |xml|
          xml.configuration(:name => name, :type => type, :factoryName => factory_name, :default => default) do |xml|
            yield xml if block_given?
          end
        end
      end

      def add_exploded_war_artifact(project, options = {})
        artifact_name = options[:name] || project.iml.id
        build_on_make = options[:build_on_make].nil? ? false : options[:build_on_make]

        add_artifact(artifact_name, "exploded-war", build_on_make) do |xml|
          dependencies = (options[:dependencies] || ([project] + project.compile.dependencies)).flatten
          libraries, projects = partition_dependencies(dependencies)

          ## The content here can not be indented
          output_dir = options[:output_dir] || project._(:artifacts, artifact_name)
          xml.tag!('output-path', output_dir)

          xml.root :id => "root" do
            xml.element :id => "directory", :name => "WEB-INF" do
              xml.element :id => "directory", :name => "classes" do
                projects.each do |p|
                  xml.element :id => "module-output", :name => p.iml.id
                end
                if options[:enable_jpa]
                  module_names = options[:jpa_module_names] || [project.iml.id]
                  module_names.each do |module_name|
                    facet_name = options[:jpa_facet_name] || "JPA"
                    xml.element :id => "jpa-descriptors", :facet => "#{module_name}/jpa/#{facet_name}"
                  end
                end
                if options[:enable_ejb]
                  module_names = options[:ejb_module_names] || [project.iml.id]
                  module_names.each do |module_name|
                    facet_name = options[:ejb_facet_name] || "EJB"
                    xml.element :id => "javaee-facet-resources", :facet => "#{module_name}/ejb/#{facet_name}"
                  end
                end
              end
              xml.element :id => "directory", :name => "lib" do
                libraries.each(&:invoke).map(&:to_s).each do |dependency_path|
                  xml.element :id => "file-copy", :path => resolve_path(dependency_path)
                end
              end
            end

            if options[:enable_war].nil? || options[:enable_war]
              module_names = options[:war_module_names] || [project.iml.id]
              module_names.each do |module_name|
                facet_name = options[:war_facet_name] || "Web"
                xml.element :id => "javaee-facet-resources", :facet => "#{module_name}/web/#{facet_name}"
              end
            end

            if options[:enable_gwt]
              module_names = options[:gwt_module_names] || [project.iml.id]
              module_names.each do |module_name|
                facet_name = options[:gwt_facet_name] || "GWT"
                xml.element :id => "gwt-compiler-output", :facet => "#{module_name}/gwt/#{facet_name}"
              end
            end
          end
        end
      end

      def add_exploded_ear_artifact(project, options ={})

        artifact_name = options[:name] || project.iml.id
        build_on_make = options[:build_on_make].nil? ? true : options[:build_on_make]

        add_artifact(artifact_name, "exploded-ear", build_on_make) do |xml|
          dependencies = (options[:dependencies] || ([project] + project.compile.dependencies)).flatten
          libraries, projects = partition_dependencies(dependencies)

          ## The content here can not be indented
          output_dir = options[:output_dir] || project._(:artifacts, artifact_name)
          xml.tag!('output-path', output_dir)

          xml.root :id => "root" do

            xml.element :id => "module-output", :name => project.iml.id

            projects.each do |p|
              xml.element :id => "directory", :name => p.iml.id do
                xml.element :id => "module-output", :name => p.iml.id
              end
            end

            xml.element :id => "directory", :name => "lib" do
              libraries.each(&:invoke).map(&:to_s).each do |dependency_path|
                xml.element :id => "file-copy", :path => resolve_path(dependency_path)
              end
            end

          end
        end
      end

      def add_exploded_ejb_artifact(project, options = {})

        artifact_name = options[:name] || project.iml.id
        build_on_make = options[:build_on_make].nil? ? true : options[:build_on_make]

        add_artifact(artifact_name, "exploded-ejb", build_on_make) do |xml|
          dependencies = (options[:dependencies] || ([project] + project.compile.dependencies)).flatten
          libraries, projects = partition_dependencies(dependencies)

          ## The content here can not be indented
          output_dir = options[:output_dir] || project._(:artifacts, artifact_name)
          xml.tag!('output-path', output_dir)

          xml.root :id => "root" do

            xml.element :id => "module-output", :name => project.iml.id

            if options[:enable_jpa]
              module_names = options[:jpa_module_names] || [project.iml.id]
              module_names.each do |module_name|
                facet_name = options[:jpa_facet_name] || "JPA"
                xml.element :id => "jpa-descriptors", :facet => "#{module_name}/jpa/#{facet_name}"
              end
            end

            if options[:enable_ejb].nil? || options[:enable_ejb]
              module_names = options[:ejb_module_names] || [project.iml.id]
              module_names.each do |module_name|
                facet_name = options[:ejb_facet_name] || "EJB"
                xml.element :id => "javaee-facet-resources", :facet => "#{module_name}/ejb/#{facet_name}"
              end
            end

          end
        end
      end


      def add_gwt_configuration(launch_page, project, options = {})
        name = options[:name] || "Run #{launch_page}"
        shell_parameters = options[:shell_parameters] || ""
        vm_parameters = options[:vm_parameters] || "-Xmx512m"

        add_configuration(name, "GWT.ConfigurationType", "GWT Configuration") do |xml|
          xml.module(:name => project.iml.id)
          xml.option(:name => "RUN_PAGE", :value => launch_page)
          xml.option(:name => "SHELL_PARAMETERS", :value => shell_parameters)
          xml.option(:name => "VM_PARAMETERS", :value => vm_parameters)

          xml.RunnerSettings(:RunnerId => "Run")
          xml.ConfigurationWrapper(:RunnerId => "Run")
          xml.method()
        end
      end

      protected

      def extension
        "ipr"
      end

      def detect_vcs
        if File.directory?(buildr_project._('.svn'))
          "svn"
        elsif File.directory?(buildr_project._('.git'))
          "Git"
        end
      end

      def base_document
        target = StringIO.new
        Builder::XmlMarkup.new(:target => target).project(:version => "4")
        Buildr::IntellijIdea.new_document(target.string)
      end

      def default_components
        [
          lambda { modules_component },
          vcs_component,
          artifacts_component,
          configurations_component,
          lambda { framework_detection_exclusion_component }
        ]
      end

      def framework_detection_exclusion_component
        create_component('FrameworkDetectionExcludesConfiguration') do |xml|
          xml.file :url => file_path(buildr_project._(:artifacts))
        end
      end

      def initial_components
        [
          lambda { project_root_manager_component },
          lambda { project_details_component }
        ]
      end

      def project_root_manager_component
        attribs = {}
        attribs["version"] = "2"
        attribs["languageLevel"] = "JDK_#{self.jdk_version.gsub('.', '_')}"
        attribs["assert-keyword"] = "true"
        attribs["jdk-15"] = "true"
        attribs["project-jdk-name"] = self.jdk_version
        attribs["project-jdk-type"] = "JavaSDK"
        create_component("ProjectRootManager", attribs) do |xml|
          xml.output("url" => "file://$PROJECT_DIR$/out")
        end
      end

      def project_details_component
        create_component("ProjectDetails") do |xml|
          xml.option("name" => "projectName", "value" => self.name)
        end
      end

      def modules_component
        create_component("ProjectModuleManager") do |xml|
          xml.modules do
            buildr_project.projects.select { |subp| subp.iml? }.each do |subproject|
              module_path = subproject.base_dir.gsub(/^#{buildr_project.base_dir}\//, '')
              path = "#{module_path}/#{subproject.iml.name}.iml"
              attribs = {:fileurl => "file://$PROJECT_DIR$/#{path}", :filepath => "$PROJECT_DIR$/#{path}"}
              if subproject.iml.group == true
                attribs[:group] = subproject.parent.name.gsub(':', '/')
              elsif !subproject.iml.group.nil?
                attribs[:group] = subproject.iml.group.to_s
              end
              xml.module attribs
            end
            self.extra_modules.each do |iml_file|
              xml.module :fileurl => "file://$PROJECT_DIR$/#{iml_file}",
                         :filepath => "$PROJECT_DIR$/#{iml_file}"
            end
            if buildr_project.iml?
              xml.module :fileurl => "file://$PROJECT_DIR$/#{buildr_project.iml.name}.iml",
                         :filepath => "$PROJECT_DIR$/#{buildr_project.iml.name}.iml"
            end
          end
        end
      end

      def vcs_component
        if vcs
          create_component("VcsDirectoryMappings") do |xml|
            xml.mapping :directory => "", :vcs => vcs
          end
        end
      end

      def artifacts_component
        create_composite_component("ArtifactManager", self.artifacts)
      end

      def configurations_component
        create_composite_component("ProjectRunConfigurationManager", self.configurations)
      end

      def resolve_path(path)
        resolve_path_from_base(path, "$PROJECT_DIR$")
      end

    private

      def partition_dependencies(dependencies)
        libraries = []
        projects = []

        dependencies.each do |dependency|
          artifacts = Buildr.artifacts(dependency)
          artifacts_as_strings = artifacts.map(&:to_s)
          project = Buildr.projects.detect do |project|
            [project.packages, project.compile.target, project.resources.target, project.test.compile.target, project.test.resources.target].flatten.
              detect { |component| artifacts_as_strings.include?(component.to_s) }
          end
          if project
            projects << project
          else
            libraries += artifacts
          end
        end
        return libraries.uniq, projects.uniq
      end
    end

    module ProjectExtension
      include Extension

      first_time do
        desc "Generate Intellij IDEA artifacts for all projects"
        Project.local_task "idea" => "artifacts"

        desc "Delete the generated Intellij IDEA artifacts"
        Project.local_task "idea:clean"
      end

      before_define do |project|
        project.recursive_task("idea")
        project.recursive_task("idea:clean")
      end

      after_define do |project|
        idea = project.task("idea")

        files = [
          (project.iml if project.iml?),
          (project.ipr if project.ipr?)
        ].compact

        files.each do |ideafile|
          module_dir =  File.dirname(ideafile.filename)
          idea.enhance do |task|
            mkdir_p module_dir
            info "Writing #{ideafile.filename}"
            t = Tempfile.open("buildr-idea")
            temp_filename = t.path
            t.close!
            File.open(temp_filename, "w") do |f|
              ideafile.write f
            end
            mv temp_filename, ideafile.filename
          end
        end

        project.task("idea:clean") do
          files.each do |f|
            info "Removing #{f.filename}" if File.exist?(f.filename)
            rm_rf f.filename
          end
        end
      end

      def ipr
        if ipr?
          @ipr ||= IdeaProject.new(self)
        else
          raise "Only the root project has an IPR"
        end
      end

      def ipr?
        !@no_ipr && self.parent.nil?
      end

      def iml
        if iml?
          unless @iml
            inheritable_iml_source = self.parent
            while inheritable_iml_source && !inheritable_iml_source.iml?
              inheritable_iml_source = inheritable_iml_source.parent;
            end
            @iml = inheritable_iml_source ? inheritable_iml_source.iml.clone : IdeaModule.new
            @iml.buildr_project = self
          end
          return @iml
        else
          raise "IML generation is disabled for #{self.name}"
        end
      end

      def no_ipr
        @no_ipr = true
      end

      def no_iml
        @has_iml = false
      end

      def iml?
        @has_iml = @has_iml.nil? ? true : @has_iml
      end
    end
  end
end

class Buildr::Project
  include Buildr::IntellijIdea::ProjectExtension
end
