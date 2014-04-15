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

  # An ArtifactNamespace is a hierarchical dictionary used to manage ArtifactRequirements.
  # It can be used to have different artifact versions per project
  # or to allow users to select a version for addons or modules.
  #
  # Namespaces are opened using the Buildr.artifact_ns method, most important methods are:
  # [need]      Used to create a requirement on the namespace.
  # [use]       Set the artifact version to use for a requirement.
  # [values_at] Reference requirements by name.
  # [each]      Return each ArtifactRequirement in the namespace.
  # The method_missing method for instances provides some syntactic sugar to these.
  # See the following examples, and the methods for ArtifactRequirement.
  #
  # = Avoiding constant pollution on buildfile
  #
  # Each project has its own ArtifactNamespace inheriting the one from the
  # parent project up to the root namespace.
  #
  # Consider the following snippet, as project grows, each subproject
  # may need different artifact combinations and/or versions. Assigning
  # artifact specifications to constants can make it painful to maintain
  # their references even if using structs/hashes.
  #
  #   -- buildfile --
  #   SPRING = 'org.springframework:spring:jar:2.5'
  #   SPRING_OLD = 'org.springframework:spring:jar:1.0'
  #   LOGGING = ['comons-logging:commons-logging:jar:1.1.1',
  #              'log4j:log4j:jar:1.2.15']
  #   WL_LOGGING = artifact('bea:wlcommons-logging:jar:8.1').from('path/to/wlcommons-logging.jar')
  #   LOGGING_WEBLOGIC = ['comons-logging:commons-logging:jar:1.1.1',
  #                       WL_LOGGING]
  #   COMMONS = struct :collections => 'commons-collection:commons-collection:jar:3.1',
  #                    :net => 'commons-net:commons-net:jar:1.4.0'
  #
  #   define 'example1' do
  #     define 'one' do
  #       compile.with SPRING, LOGGING_WEBLOGIC, COMMONS
  #     end
  #     define 'two' do
  #       compile.with SPRING_OLD, LOGGING, COMMONS
  #     end
  #     define 'three' do
  #       compile.with "commons-collections:commons-collections:jar:2.2"
  #     end
  #   end
  #
  #
  # With ArtifactNamespace you can do some more advanced stuff, the following
  # annotated snipped could still be reduced if default artifact definitions were
  # loaded from yaml file (see section below and ArtifactNamespace.load).
  #
  #   -- buildfile --
  #   artifact_ns do |ns| # the current namespace (root if called outside a project)
  #     # default artifacts
  #     ns.spring = 'org.springframework:spring:jar:2.5'
  #     # default logger is log4j
  #     ns.logger = 'log4j:log4j:jar:1.2.15'
  #
  #     # create a sub namespace by calling the #ns method,
  #     # artifacts defined on the sub-namespace can be referenced by
  #     # name :commons_net or by calling commons.net
  #     ns.ns :commons, :net => 'commons-net:commons-net:jar:1.4.0',
  #                     :logging => 'comons-logging:commons-logging:jar:1.1.1'
  #
  #
  #     # When a child namespace asks for the :log artifact,
  #     # these artifacts will be searched starting from the :current namespace.
  #     ns.virtual :log, :logger, :commons_logging
  #   end
  #
  #   artifact_ns('example2:one') do |ns| # namespace for the one subproject
  #     ns.logger = artifact('bea:wlcommons-logging:jar:8.1').from('path/to/wlcommons-logging.jar')
  #   end
  #   artifact_ns('example2:two') do |ns|
  #     ns.spring = '1.0' # for project two use an older spring version (just for an example)
  #   end
  #   artifact_ns('example2:three').commons_collections = 2.2'
  #   artifact_ns('example2:four') do |ns|
  #     ns.beanutils = 'commons-beanutils:commons-beanutils:jar:1.5'        # just for this project
  #     ns.ns(:compilation).use :commons_logging, :beanutils, :spring       # compile time dependencies
  #     ns.ns(:testing).use :log, :beanutils, 'cglib:cglib-nodep:jar:2.1.3' # run time dependencies
  #   end
  #
  #   define 'example2' do
  #     define 'one' do
  #       compile.with :spring, :log, :commons # uses weblogic logging
  #     end
  #     define 'two' do
  #       compile.with :spring, :log, :commons # will take old spring
  #     end
  #     define 'three' do
  #       compile.with :commons_collections
  #       test.with artifact_ns('example2:two').spring # use spring from project two
  #     end
  #     define 'four' do
  #       compile.with artifact_ns.compilation
  #       test.with artifact_ns.testing
  #     end
  #     task(:down_them_all) do # again, just to fill this space with something ;)
  #       parent.projects.map(&method(:artifact_ns)).map(&:artifacts).map(&:invoke)
  #     end
  #   end
  #
  # = Loading from a yaml file (e. profiles.yaml)
  #
  # If your projects use lots of jars (after all we are using java ;) you may prefer
  # to have constant artifact definitions on an external file.
  # Doing so would allow an external tool (or future Buildr feature) to maintain
  # an artifacts.yaml for you.
  # An example usage is documented on the ArtifactNamespace.load method.
  #
  # = For addon/plugin writers & Customizing artifact versions
  #
  # Sometimes users would need to change the default artifact versions used by some
  # module, for example, the XMLBeans compiler needs this, because of compatibility
  # issues. Another example would be to select the groovy version to use on all our
  # projects so that Buildr modules requiring groovy jars can use user prefered versions.
  #
  # To meet this goal, an ArtifactNamespace allows to specify ArtifactRequirement objects.
  # In fact the only difference with the examples you have already seen is that requirements
  # have an associated VersionRequirement, so that each time a user tries to select a version,
  # buildr checks if it satisfies the requirements.
  #
  # Requirements are declared using the ArtifactNamespace#need method, but again,
  # syntactic sugar is provided by ArtifactNamespace#method_missing.
  #
  # The following example is taken from the XMLBeans compiler module.
  # And illustrates how addon authors should specify their requirements,
  # provide default versions, and document the namespace for users to customize.
  #
  #    module Buildr::XMLBeans
  #
  #       # You need to document this constant, giving users some hints
  #       # about when are (maybe some of) these artifacts used. I mean,
  #       # some modules, add jars to the Buildr classpath when its file
  #       # is required, you would need to tell your users, so that they
  #       # can open the namespace and specify their defaults. Of course
  #       # when the requirements are defined, buildr checks if any compatible
  #       # version has been already defined, if so, uses it.
  #       #
  #       # Some things here have been changed to illustrate their meaning.
  #       REQUIRES = ArtifactNamespace.for(self).tap do |ns|
  #
  #         # This jar requires a >2.0 version, default being 2.3.0
  #         ns.xmlbeans! 'org.apache.xmlbeans:xmlbeans:jar:2.3.0', '>2'
  #
  #         # Users can customize with Buildr::XMLBeans::REQUIRES.stax_api = '1.2'
  #         # This is a non-flexible requirement, only satisfied by version 1.0.1
  #         ns.stax_api! 'stax:stax-api:jar:1.0.1'
  #
  #         # This one is not part of XMLBeans, but is just another example
  #         # illustrating an `artifact requirement spec`.
  #
  #         ns.need " some_name ->  ar:ti:fact:3.2.5 ->  ( >2 & <4)"
  #
  #         # As you can see it's just an artifact spec, prefixed with
  #         # ' some_name -> ', this means users can use that name to
  #         # reference the requirement, also this string has a VersionRequirement
  #         # just after another ->.
  #       end
  #
  #       # The REQUIRES constant is an ArtifactNamespace instance,
  #       # that means we can use it directly. Note that calling
  #       # Buildr.artifact_ns would lead to the currently executing context,
  #       # not the one for this module.
  #       def use
  #         # test if user specified his own version, if so, we could perform some
  #         # functionallity based on this.
  #         REQUIRES.some_name.selected? # => false
  #
  #         REQUIRES.some_name.satisfied_by?('1.5') # => false
  #         puts REQUIRES.some_name.requirement     # => ( >2 & <4 )
  #
  #         REQUIRES.artifacts # get the Artifact tasks
  #       end
  #
  #    end
  #
  # A more advanced example using ArtifactRequirement listeners is included
  # in the artifact_namespace_spec.rb description for 'Extension using ArtifactNamespace'
  # That's it for addon writers, now, users can select their prefered version with
  # something like:
  #
  #    require 'buildr/xmlbeans'
  #    Buildr::XMLBeans::REQUIRES.xmlbeans = '2.2.0'
  #
  # More advanced stuff, if users really need to select an xmlbeans version
  # per project, they can do so letting :current (that is, the currently running
  # namespace) be parent of the REQUIRES namespace:
  #
  #    Buildr::XMLBeans::REQUIRES.parent = :current
  #
  # Now, provided that the compiler does not caches its artifacts, it will
  # select the correct version. (See the first section for how to select per project
  # artifacts).
  #
  #
  class ArtifactNamespace
    class << self
      # Forget all namespaces, create a new ROOT
      def clear
        @instances = nil
        remove_const(:ROOT) rescue nil
        const_set(:ROOT, new('root'))
      end

      # Differs from Artifact.to_hash in that 1) it does not choke when version isn't present
      # and 2) it assumes that if an artifact spec ends with a colon, e.g. "org.example:library:jdk5:"
      # it indicates the last segment ("jdk5") is a classifier.
      def to_hash(spec)
        if spec.respond_to?(:to_spec)
          to_hash spec.to_spec
        elsif Hash === spec
          return spec
        elsif String === spec || Symbol === spec
          spec = spec.to_s
          if spec[-1,1] == ':'
            group, id, type, classifier, *rest = spec.split(':').map { |part| part.empty? ? nil : part }
          else
            group, id, type, version, *rest = spec.split(':').map { |part| part.empty? ? nil : part }
            unless rest.empty?
              # Optional classifier comes before version.
              classifier, version = version, rest.shift
            end
          end
          fail "Expecting <group:id:type:version> or <group:id:type:classifier:version>, found <#{spec}>" unless rest.empty?
          { :group => group, :id => id, :type => type, :version => version, :classifier => classifier }.reject { |k,v| v == nil }
        else
          fail "Unexpected artifact spec: #{spec.inspect}"
        end
      end

      # Populate namespaces from a hash of hashes.
      # The following example uses the profiles yaml to achieve this.
      #
      #   -- profiles.yaml --
      #   development:
      #     artifacts:
      #       root:        # root namespace
      #         spring:     org.springframework:spring:jar:2.5
      #         groovy:     org.codehaus.groovy:groovy:jar:1.5.4
      #         logging:    # define a named group
      #           - log4j:log4j:jar:1.2.15
      #           - commons-logging:commons-logging:jar:1.1.1
      #
      #       # open Buildr::XMLBeans namespace
      #       Buildr::XMLBeans:
      #         xmlbeans: 2.2
      #
      #       # for subproject one:oldie
      #       one:oldie:
      #         spring:  org.springframework:spring:jar:1.0
      #
      #   -- buildfile --
      #   ArtifactNamespace.load(Buildr.settings.profile['artifacts'])
      def load(namespaces = {})
        namespaces.each_pair { |name, uses| instance(name).use(uses) }
      end

      # :call-seq:
      #   ArtifactNamespace.instance { |current_ns| ... } -> current_ns
      #   ArtifactNamespace.instance(name) { |ns| ... } -> ns
      #   ArtifactNamespace.instance(:current) { |current_ns| ... } -> current_ns
      #   ArtifactNamespace.instance(:root) { |root_ns| ... } -> root_ns
      #
      # Obtain an instance for the given name
      def instance(name = nil)
        case name
        when :root, 'root' then return ROOT
        when ArtifactNamespace then return name
        when Array then name = name.join(':')
        when Module, Project then name = name.name
        when :current, 'current', nil then
          task = Thread.current[:rake_chain]
          task = task.instance_variable_get(:@value) if task
          name = case task
                 when Project then task.name
                 when Rake::Task then task.scope.join(':')
                 when nil then Buildr.application.current_scope.join(':')
                 end
        end
        name = name.to_s
        if name.size == 0
          instance = ROOT
        else
          name = name.to_s
          @instances ||= Hash.new { |h, k| h[k] = new(k) }
          instance = @instances[name]
        end
        yield(instance) if block_given?
        instance
      end

      alias_method :[], :instance
      alias_method :for, :instance

      # :call-seq:
      #   ArtifactNamespace.root { |ns| ... } -> ns
      #
      # Obtain the root namespace, returns the ROOT constant
      def root
        yield ROOT if block_given?
        ROOT
      end
    end

    module DClone #:nodoc:
      def dclone
        clone = self.clone
        clone.instance_variables.each do |i|
          value = clone.instance_variable_get(i)
          value = value.dclone rescue
          clone.instance_variable_set(i, value)
        end
        clone
      end
    end

    class Registry < Hash #:nodoc:
      include DClone

      attr_accessor :parent
      def alias(new_name, old_name)
        new_name = new_name.to_sym
        old_name = old_name.to_sym
        if obj = get(old_name, true)
          self[new_name] = obj
          @aliases ||= []
          group = @aliases.find { |a| a.include?(new_name) }
          group.delete(new_name) if group
          group = @aliases.find { |a| a.include?(old_name) }
          @aliases << (group = [old_name]) unless group
          group << new_name unless group.include?(new_name)
        end
        obj
      end

      def aliases(name)
        return [] unless name
        name = name.to_sym
        ((@aliases ||= []).find { |a| a.include?(name) } || [name]).dup
      end

      def []=(key, value)
        return unless key
        super(key.to_sym, value)
      end

      def get(key, include_parent = nil)
        [].tap { |a| aliases(key).select { |n| a[0] = self[n] } }.first ||
          (include_parent && parent && parent.get(key, include_parent))
      end

      def keys(include_parent = nil)
        (super() | (include_parent && parent && parent.keys(include_parent) || [])).uniq
      end

      def values(include_parent = nil)
        (super() | (include_parent && parent && parent.values(include_parent) || [])).uniq
      end

      def key?(key, include_parent = nil)
        return false unless key
        super(key.to_sym) || (include_parent && parent && parent.key?(key, include_parent))
      end

      def delete(key, include_parent = nil)
        aliases(key).map {|n| super(n) } && include_parent && parent && parent.delete(key, include_parent)
      end
    end

    # An artifact requirement is an object that ActsAsArtifact and has
    # an associated VersionRequirement. It also knows the name (some times equal to the
    # artifact id) that is used to store it in an ArtifactNamespace.
    class ArtifactRequirement
      attr_accessor :version
      attr_reader :name, :requirement

      include DClone

      # Create a requirement from an `artifact requirement spec`.
      # This spec has three parts, separated by  ->
      #
      #     some_name ->  ar:ti:fact:3.2.5 ->  ( >2 & <4)
      #
      # As you can see it's just an artifact spec, prefixed with
      #     some_name ->
      # the :some_name symbol becomes this object's name and
      # is used to store it on an ArtifactNamespace.
      #
      #                   ar:ti:fact:3.2.5
      #
      # The second part is an artifact spec by itself, and specifies
      # all remaining attributes, the version of this spec becomes
      # the default version of this requirement.
      #
      # The last part consist of a VersionRequirement.
      #                                     ->  ( >2 & <4)
      #
      # VersionRequirement supports RubyGem's comparision operators
      # in adition to parens, logical and, logical or and negation.
      # See the docs for VersionRequirement for more info on operators.
      def initialize(spec)
        self.class.send :include, ActsAsArtifact unless ActsAsArtifact === self
        if ArtifactRequirement === spec
          copy_attrs(spec)
        else
          spec = requirement_hash(spec)
          apply_spec_no_validation(spec[:spec])
          self.name = spec[:name]
          @requirement = spec[:requirement]
          @version = @requirement.default if VersionRequirement.requirement?(@version)
        end
      end

      def apply_spec_no_validation(spec)
        spec = ArtifactNamespace.to_hash(spec)
        ActsAsArtifact::ARTIFACT_ATTRIBUTES.each { |key| instance_variable_set("@#{key}", spec[key]) }
        self
      end

      # Copy attributes from other to this object
      def copy_attrs(other)
        (ActsAsArtifact::ARTIFACT_ATTRIBUTES + [:name, :requirement]).each do |attr|
          value = other.instance_variable_get("@#{attr}")
          value = value.dup if value && !value.kind_of?(Numeric) && !value.kind_of?(Symbol)
          instance_variable_set("@#{attr}", value)
        end
      end

      def name=(name)
        @name = name.to_s
      end

      # Set a the requirement, this must be an string formatted for
      # VersionRequirement#create to parse.
      def requirement=(version_requirement)
        @requirement = VersionRequirement.create(version_requirement.to_s)
      end

      # Return a hash consisting of :name, :spec, :requirement
      def requirement_hash(spec = self)
        result = {}
        if String === spec
          parts = spec.split(/\s*->\s*/, 3).map(&:strip)
          case parts.size
          when 1
            result[:spec] = ArtifactNamespace.to_hash(parts.first)
          when 2
            if /^\w+$/ === parts.first
              result[:name] = parts.first
              result[:spec] = ArtifactNamespace.to_hash(parts.last)
            else
              result[:spec] = ArtifactNamespace.to_hash(parts.first)
              result[:requirement] = VersionRequirement.create(parts.last)
            end
          when 3
            result[:name] = parts.first
            result[:spec] = ArtifactNamespace.to_hash(parts[1])
            result[:requirement] = VersionRequirement.create(parts.last)
          end
        else
          result[:spec] = ArtifactNamespace.to_hash(spec)
        end
        result[:name] ||= result[:spec][:id].to_s.to_sym
        result[:requirement] ||= VersionRequirement.create(result[:spec][:version])
        result
      end

      # Test if this requirement is satisfied by an artifact spec.
      def satisfied_by?(spec)
        return false unless requirement
        spec = ArtifactNamespace.to_hash(spec)
        hash = to_spec_hash
        hash.delete(:version)
        version = spec.delete(:version)
        hash == spec && requirement.satisfied_by?(version)
      end

      # Has user selected a version for this requirement?
      def selected?
        @selected
      end

      def selected! #:nodoc:
        @selected = true
        @listeners.each { |l| l.call(self) } if @listeners
        self
      end

      def add_listener(&callback)
        (@listeners ||= []) << callback
      end

      # Return the Artifact object for the currently selected version
      def artifact
        ::Buildr.artifact(self)
      end

      # Format this requirement as an `artifact requirement spec`
      def to_requirement_spec
        result = to_spec
        result = "#{name} -> #{result}" if name
        result = "#{result} -> #{requirement}" if requirement
        result
      end

      def to_s #:nodoc:
        id ? to_requirement_spec : version
      end

      # Return an artifact spec without the version part.
      def unversioned_spec
        hash = to_spec_hash
        return nil if hash.values.compact.length <= 1
        if hash[:classifier]
          "#{hash[:group]}:#{hash[:id]}:#{hash[:type]}:#{hash[:classifier]}:"
        else
          "#{hash[:group]}:#{hash[:id]}:#{hash[:type]}"
        end
      end

      class << self
        def unversioned_spec(spec)
          hash = ArtifactNamespace.to_hash(spec)
          return nil if hash.values.compact.length <= 1
          if hash[:classifier]
            "#{hash[:group]}:#{hash[:id]}:#{hash[:type]}:#{hash[:classifier]}:"
          else
            "#{hash[:group]}:#{hash[:id]}:#{hash[:type]}"
          end
        end
      end
    end

    include DClone
    include Enumerable
    attr_reader :name

    def initialize(name = nil) #:nodoc:
      @name = name.to_s if name
    end
    clear

    def root
      yield ROOT if block_given?
      ROOT
    end

    # ROOT namespace has no parent
    def parent
      if root?
        nil
      elsif @parent.kind_of?(ArtifactNamespace)
        @parent
      elsif @parent
        ArtifactNamespace.instance(@parent)
      elsif name
        parent_name = name.gsub(/::?[^:]+$/, '')
        parent_name == name ? root : ArtifactNamespace.instance(parent_name)
      else
        root
      end
    end

    # Set the parent for the current namespace, except if it is ROOT
    def parent=(other)
      raise 'Cannot set parent of root namespace' if root?
      @parent = other
      @registry = nil
    end

    # Is this the ROOT namespace?
    def root?
      ROOT == self
    end

    # Create a named sub-namespace, sub-namespaces are themselves
    # ArtifactNamespace instances but cannot be referenced by
    # the Buildr.artifact_ns, ArtifactNamespace.instance methods.
    # Reference needs to be through this object using the given +name+
    #
    #   artifact_ns('foo').ns(:bar).need :thing => 'some:thing:jar:1.0'
    #   artifact_ns('foo').bar # => the sub-namespace 'foo.bar'
    #   artifact_ns('foo').bar.thing # => the some thing artifact
    #
    # See the top level ArtifactNamespace documentation for examples
    def ns(name, *uses, &block)
      name = name.to_sym
      sub = registry[name]
      if sub
        raise TypeError.new("#{name} is not a sub namespace of #{self}") unless sub.kind_of?(ArtifactNamespace)
      else
        sub = ArtifactNamespace.new("#{self.name}.#{name}")
        sub.parent = self
        registry[name] = sub
      end
      sub.use(*uses)
      yield sub if block_given?
      sub
    end

    # Test if a sub-namespace by the given name exists
    def ns?(name)
      sub = registry[name.to_sym]
      ArtifactNamespace === sub
    end

    # :call-seq:
    #   artifact_ns.need 'name -> org:foo:bar:jar:~>1.2.3 -> 1.2.5'
    #   artifact_ns.need :name => 'org.foo:bar:jar:1.0'
    #
    # Create a new ArtifactRequirement on this namespace.
    # ArtifactNamespace#method_missing provides syntactic sugar for this.
    def need(*specs)
      named = specs.flatten.inject({}) do |seen, spec|
        if Hash === spec && (spec.keys & ActsAsArtifact::ARTIFACT_ATTRIBUTES).empty?
          spec.each_pair do |name, spec|
            if Array === spec # a group
              seen[name] ||= spec.map { |s| ArtifactRequirement.new(s) }
            else
              artifact = ArtifactRequirement.new(spec)
              artifact.name = name
              seen[artifact.name] ||= artifact
            end
          end
        else
          artifact = ArtifactRequirement.new(spec)
          seen[artifact.name] ||= artifact
        end
        seen
      end
      named.each_pair do |name, artifact|
        if Array === artifact # a group
          artifact.each do |a|
            unvers = a.unversioned_spec
            previous = registry[unvers]
            if previous && previous.selected? && a.satisfied_by?(previous)
              a.version = previous.version
            end
            registry[unvers] = a
          end
          group(name, *(artifact.map { |a| a.unversioned_spec } + [{:namespace => self}]))
        else
          unvers = artifact.unversioned_spec
          previous = registry.get(unvers, true)
          if previous && previous.selected? && artifact.satisfied_by?(previous)
            artifact.version = previous.version
            artifact.selected!
          end
          registry[unvers] = artifact
          registry.alias name, unvers unless name.to_s[/^\s*$/]
        end
      end
      self
    end

    # :call-seq:
    #   artifact_ns.use 'name -> org:foo:bar:jar:1.2.3'
    #   artifact_ns.use :name => 'org:foo:bar:jar:1.2.3'
    #   artifact_ns.use :name => '2.5.6'
    #
    # First and second form are equivalent, the third is used when an
    # ArtifactRequirement has been previously defined with :name, so it
    # just selects the version.
    #
    # ArtifactNamespace#method_missing provides syntactic sugar for this.
    def use(*specs)
      named = specs.flatten.inject({}) do |seen, spec|
        if Hash === spec && (spec.keys & ActsAsArtifact::ARTIFACT_ATTRIBUTES).empty?
          spec.each_pair do |name, spec|
            if ArtifactNamespace === spec # create as subnamespace
              raise ArgumentError.new("Circular reference") if self == spec
              registry[name.to_sym] = spec
            elsif Numeric === spec || (String === spec && VersionRequirement.version?(spec))
              artifact = ArtifactRequirement.allocate
              artifact.name = name
              artifact.version = spec.to_s
              seen[artifact.name] ||= artifact
            elsif Symbol === spec
              self.alias name, spec
            elsif Array === spec # a group
              seen[name] ||= spec.map { |s| ArtifactRequirement.new(s) }
            else
              artifact = ArtifactRequirement.new(spec)
              artifact.name = name
              seen[artifact.name] ||= artifact
            end
          end
        else
          if Symbol === spec
            artifact = get(spec).dclone
          else
            artifact = ArtifactRequirement.new(spec)
          end
          seen[artifact.name] ||= artifact
        end
        seen
      end
      named.each_pair do |name, artifact|
        is_group = Array === artifact
        artifact = [artifact].flatten.map do |artifact|
          unvers = artifact.unversioned_spec
          previous = get(unvers, false) || get(name, false)
          if previous # have previous on current namespace
            if previous.requirement # we must satisfy the requirement
              unless unvers # we only have the version
                satisfied = previous.requirement.satisfied_by?(artifact.version)
              else
                satisfied = previous.satisfied_by?(artifact)
              end
              raise "Unsatisfied dependency #{previous} " +
                "not satisfied by #{artifact}" unless satisfied
              previous.version = artifact.version # OK, set new version
              artifact = previous # use the same object for aliases
            else # not a requirement, set the new values
              unless artifact.id == previous.id && name != previous.name
                previous.copy_attrs(artifact)
                artifact = previous
              end
            end
          else
            if unvers.nil? && # we only have the version
                (previous = get(unvers, true, false, false))
              version = artifact.version
              artifact.copy_attrs(previous)
              artifact.version = version
            end
            artifact.requirement = nil
          end
          artifact.selected!
        end
        artifact = artifact.first unless is_group
        if is_group
          names = artifact.map do |art|
            unv = art.unversioned_spec
            registry[unv] = art
            unv
          end
          group(name, *(names + [{:namespace => self}]))
        elsif artifact.id
          unvers = artifact.unversioned_spec
          registry[name] = artifact
          registry.alias unvers, name
        else
          registry[name] = artifact
        end
      end
      self
    end

    # Like Hash#fetch
    def fetch(name, default = nil, &block)
      block ||= proc { raise IndexError.new("No artifact found by name #{name.inspect} in namespace #{self}") }
      real_name = name.to_s[/^[\w\-\.]+$/] ? name : ArtifactRequirement.unversioned_spec(name)
      get(real_name.to_sym) || default || block.call(name)
    end

    # :call-seq:
    #   artifact_ns[:name] -> ArtifactRequirement
    #   artifact_ns[:many, :names] -> [ArtifactRequirement]
    def [](*names)
      ary = values_at(*names)
      names.size == 1 ? ary.first : ary
    end

    # :call-seq:
    #   artifact_ns[:name] = 'some:cool:jar:1.0.2'
    #   artifact_ns[:name] = '1.0.2'
    #
    # Just like the use method
    def []=(*names)
      values = names.pop
      values = [values] unless Array === values
      names.each_with_index do |name, i|
        use name => (values[i] || values.last)
      end
    end

    # yield each ArtifactRequirement
    def each(&block)
      values.each(&block)
    end

    # return Artifact objects for each requirement
    def artifacts(*names)
      (names.empty? && values || values_at(*names)).map(&:artifact)
    end

    # Return all requirements for this namespace
    def values(include_parents = false, include_groups = true)
      seen, dict = {}, registry
      while dict
        dict.each do |k, v|
          v = v.call if v.respond_to?(:call)
          v = v.values if v.kind_of?(ArtifactNamespace)
          if Array === v && include_groups
            v.compact.each { |v| seen[v.name] = v unless seen.key?(v.name) }
          else
            seen[v.name] = v unless seen.key?(v.name)
          end
        end
        dict = include_parents ? dict.parent : nil
      end
      seen.values
    end

    # Return only the named requirements
    def values_at(*names)
      names.map do |name|
        catch :artifact do
          unless name.to_s[/^[\w\-\.]+$/]
            unvers = ArtifactRequirement.unversioned_spec(name)
            unless unvers.to_s == name.to_s
              req = ArtifactRequirement.new(name)
              reg = self
              while reg
                candidate = reg.send(:get, unvers, false, false, true)
                throw :artifact, candidate if req.satisfied_by?(candidate)
                reg = reg.parent
              end
            end
          end
          get(name.to_sym)
        end
      end
    end

    def key?(name, include_parents = false)
      name = ArtifactRequirement.unversioned_spec(name) unless name.to_s[/^[\w\-\.]+$/]
      registry.key?(name, include_parents)
    end

    def keys
      values.map(&:name)
    end

    def delete(name, include_parents = false)
      registry.delete(name, include_parents)
      self
    end

    def clear
      keys.each { |k| delete(k) }
    end

    # :call-seq:
    #   group :who, :me, :you
    #   group :them, :me, :you, :namespace => ns
    #
    # Create a virtual group on this namespace. When the namespace
    # is asked for the +who+ artifact, it's value is an array made from
    # the remaining names. These names are searched by default from the current
    # namespace.
    # Second form specified the starting namespace to search from.
    def group(group_name, *members)
      namespace = (Hash === members.last && members.pop[:namespace]) || :current
      registry[group_name] = lambda do
        artifacts = self.class[namespace].values_at(*members)
        artifacts = artifacts.first if members.size == 1
        artifacts
      end
      self
    end

    alias_method :virtual, :group

    # Create an alias for a named requirement.
    def alias(new_name, old_name)
      registry.alias(new_name, old_name) or
        raise NameError.new("Undefined artifact name: #{old_name}")
    end

    def to_s #:nodoc:
      name.to_s
    end

    # :call-seq:
    #   artifact_ns.cool_aid!('cool:aid:jar:2.3.4', '~>2.3') -> artifact_requirement
    #   artifact_ns.cool_aid = '2.3.5'
    #   artifact_ns.cool_aid  -> artifact_requirement
    #   artifact_ns.cool_aid? -> true | false
    #
    # First form creates an ArtifactRequirement on the namespace.
    # It is equivalent to providing a requirement_spec to the #need method:
    #   artifact_ns.need "cool_aid -> cool:aid:jar:2.3.4 -> ~>2.3"
    # the second argument is optional.
    #
    # Second form can be used to select an artifact version
    # and is equivalent to:
    #   artifact_ns.use :cool_aid => '1.0'
    #
    # Third form obtains the named ArtifactRequirement, can be
    # used to test if a named requirement has been defined.
    # It is equivalent to:
    #   artifact_ns.fetch(:cool_aid) { nil }
    #
    # Last form tests if the ArtifactRequirement has been defined
    # and a version has been selected for use.
    # It is equivalent to:
    #
    #   artifact_ns.has_cool_aid?
    #   artifact_ns.values_at(:cool_aid).flatten.all? { |a| a && a.selected? }
    #
    def method_missing(name, *args, &block)
      case name.to_s
      when /!$/ then
        name = $`.intern
        if args.size < 1 || args.size > 2
          raise ArgumentError.new("wrong number of arguments for #{name}!(spec, version_requirement?)")
        end
        need name => args.first
        get(name).tap { |r| r.requirement = args.last if args.size == 2 }
      when /=$/ then use $` => args.first
      when /\?$/ then
        name = $`.gsub(/^(has|have)_/, '').intern
        [get(name)].flatten.all? { |a| a && a.selected? }
      else
        if block || args.size > 0
          raise ArgumentError.new("wrong number of arguments #{args.size} for 0 or block given")
        end
        get(name)
      end
    end

    # Return an anonymous module
    #   # first create a requirement
    #   artifact_ns.cool_aid! 'cool:aid:jar:>=1.0'
    #
    #   # extend an object as a cool_aid delegator
    #   jars = Object.new.extend(artifact_ns.accessor(:cool_aid))
    #   jars.cool_aid = '2.0'
    #
    #   artifact_ns.cool_aid.version # -> '2.0'
    def accessor(*names)
      ns = self
      Module.new do
        names.each do |name|
          define_method("#{name}") { ns.send("#{name}") }
          define_method("#{name}?") { ns.send("#{name}?") }
          define_method("#{name}=") { |vers| ns.send("#{name}=", vers) }
        end
      end
    end

   private
    def get(name, include_parents = true, include_subs = true, include_self = true) #:nodoc:
      if include_subs && name.to_s[/_/] # try sub namespaces first
        sub, parts = self, name.to_s.split('_')
        sub_name = parts.shift.to_sym
        until sub != self || parts.empty?
          if registry[sub_name].kind_of?(ArtifactNamespace)
            sub = registry[sub_name]
            artifact = sub[parts.join('_')]
          else
            sub_name = [sub_name, parts.shift].join('_').to_sym
          end
        end
      end
      unless artifact
        if include_self
          artifact = registry.get(name, include_parents)
        elsif include_parents && registry.parent
          artifact = registry.parent.get(name, true)
        end
      end
      artifact = artifact.call if artifact.respond_to?(:call)
      artifact
    end

    def registry
      @registry ||= Registry.new.tap do |m|
        m.parent = parent.send(:registry) unless root?
      end
    end

  end # ArtifactNamespace

  # :call-seq:
  #   project.artifact_ns -> ArtifactNamespace
  #   Buildr.artifact_ns(name) -> ArtifactNamespace
  #   Buildr.artifact_ns -> ArtifactNamespace for the currently running Project
  #
  # Open an ArtifactNamespace.
  # If a block is provided, the namespace is yielded to it.
  #
  # See also ArtifactNamespace.instance
  def artifact_ns(name = nil, &block)
    name = self if name.nil? && self.kind_of?(Project)
    ArtifactNamespace.instance(name, &block)
  end

end


