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

#
module Buildr
  class POM

    POM_TO_SPEC_MAP = { :group=>"groupId", :id=>"artifactId", :type=>"type",
      :version=>"version", :classifier=>"classifier", :scope=>"scope" }
    SCOPES_TRANSITIVE = [nil, "compile", "runtime"]
    SCOPES_WE_USE = SCOPES_TRANSITIVE + ["provided"]

    # POM project as Hash (using XmlSimple).
    attr_reader :project
    # Parent POM if referenced by this POM.
    attr_reader :parent

    class << self

      # :call-seq:
      #   POM.load(arg)
      #
      # Load new POM object form various kind of sources such as artifact, hash representing spec, filename, XML.
      def load(source)
        case source
        when Hash
          load(Buildr.artifact(source).pom)
        when Artifact
          pom = source.pom
          pom.invoke
          load(pom.to_s)
        when Rake::FileTask
          source.invoke
          load(source.to_s)
        when String
          filename = File.expand_path(source)
          unless pom = cache[filename]
            trace "Loading m2 pom file from #{filename}"
            begin
              pom = POM.new(IO.read(filename))
            rescue REXML::ParseException => e
              fail "Could not parse #{filename}, #{e.continued_exception}"
            end
            cache[filename] = pom
          end
          pom
        else
          raise ArgumentError, "Expecting Hash spec, Artifact, file name or file task"
        end
      end

    private

      def cache()
        @cache ||= {}
      end

    end

    def initialize(xml) #:nodoc:
      @project = XmlSimple.xml_in(xml)
      @parent = POM.load(pom_to_hash(project["parent"].first).merge(:type=>'pom')) if project['parent']
    end

    # :call-seq:
    #   dependencies(scopes?) => artifacts
    #   dependencies(:scopes = [:runtime, :test, ...], :optional = true) => artifacts
    #
    # Returns list of required dependencies as specified by the POM. You can specify which scopes
    # to use (e.g. "compile", "runtime"); use +nil+ for dependencies with unspecified scope.
    # The default scopes are +nil+, "compile" and "runtime" (aka SCOPES_WE_USE) and no optional dependencies.
    # Specifying optional = true will return all optional dependencies matching the given scopes.
    def dependencies(options = {})
      # backward compatibility
      options = { :scopes => options } if Array === options

      # support symbols, but don't fidget with nil
      options[:scopes] = (options[:scopes] || SCOPES_WE_USE).map { |s| s.to_s if s }

      # try to cache dependencies also
      @depends_for_scopes ||= {}
      unless depends = @depends_for_scopes[options]
        declared = project["dependencies"].first["dependency"] rescue nil
        depends = (declared || [])
        depends = depends.reject { |dep| value_of(dep["optional"]) =~ /true/ } unless options[:optional]
        depends = depends.map { |dep|
            spec = pom_to_hash(dep, properties)
            apply = managed(spec)
            spec = apply.merge(spec) if apply

            next if options[:exclusions] && options[:exclusions].any? { |ex| dep['groupId'] == ex['groupId'] && dep['artifactId'] == ex['artifactId'] }

            # calculate transitive dependencies
            if options[:scopes].include?(spec[:scope])
              spec.delete(:scope)

              exclusions = dep["exclusions"].first["exclusion"] rescue nil
              transitive_deps = POM.load(spec).dependencies(:exclusions => exclusions, :scopes => (options[:scopes_transitive] || SCOPES_TRANSITIVE) ) rescue []

              [Artifact.to_spec(spec)] + transitive_deps
            end
          }.flatten.compact #.uniq_by{|spec| art = spec.split(':'); "#{art[0]}:#{art[1]}"}
        @depends_for_scopes[options] = depends
      end
      depends
    end

    # :call-seq:
    #   properties() => hash
    #
    # Returns properties available to this POM as hash. Includes explicit properties and pom.xxx/project.xxx
    # properties for groupId, artifactId, version and packaging.
    def properties()
      @properties ||= begin
        pom = ["groupId", "artifactId", "version", "packaging"].inject({}) { |hash, key|
          value = project[key] || (parent ? parent.project[key] : nil)
          hash[key] = hash["pom.#{key}"] = hash["project.#{key}"] = value_of(value) if value
          hash
        }
        props = project["properties"].first rescue {}
        props = props.inject({}) { |mapped, pair| mapped[pair.first] = value_of(pair.last, props) ; mapped }
        (parent ? parent.properties.merge(props) : props).merge(pom)
      end
    end

    # :call-seq:
    #    managed() => hash
    #    managed(hash) => hash
    #
    # The first form returns all the managed dependencies specified by this POM in dependencyManagement.
    # The second form uses a single spec hash and expands it from the current/parent POM. Used to determine
    # the version number if specified in dependencyManagement instead of dependencies.
    def managed(spec = nil)
      if spec
        managed.detect { |dep| [:group, :id, :type, :classifier].all? { |key| spec[key] == dep[key] } } ||
          (parent ? parent.managed(spec) : nil)
      else
        @managed ||= begin
          managed = project["dependencyManagement"].first["dependencies"].first["dependency"] rescue nil
          managed ? managed.map { |dep| pom_to_hash(dep, properties) } : []
        end
      end
    end

  private

    # :call-seq:
    #    value_of(element) => string
    #    value_of(element, true) => string
    #
    # Returns the normalized text value of an element from its XmlSimple value. The second form performs
    # property substitution.
    def value_of(element, substitute = nil)
      value = element.to_a.join.strip
      value = value.gsub(/\$\{([^}]+)\}/) { |key| Array(substitute[$1]).join.strip } if substitute
      value
    end

    # :call-seq:
    #    pom_to_hash(element) => hash
    #    pom_to_hash(element, true) => hash
    #
    # Return the spec hash from an XmlSimple POM referencing element (e.g. project, parent, dependency).
    # The second form performs property substitution.
    def pom_to_hash(element, substitute = nil)
      hash = POM_TO_SPEC_MAP.inject({}) { |spec, pair|
        spec[pair.first] = value_of(element[pair.last], substitute) if element[pair.last]
        spec
      }
      {:scope => "compile", :type => "jar"}.merge(hash)
    end

  end
end
