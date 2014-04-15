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


require 'hpricot'


module Buildr

  # Search best artifact version from remote repositories
  module ArtifactSearch
    extend self
    
    def include(method = nil)
      (@includes ||= []).tap { push method if method }
    end

    def exclude(method = nil)
      (@excludes ||= []).tap { push method if method }
    end

    # TODO: return the url for best matching repo
    def best_version(spec, *methods)
      spec = Artifact.to_hash(spec)
      spec[:version] = requirement = VersionRequirement.create(spec[:version])
      select = lambda do |candidates|
        candidates.find { |candidate| requirement.satisfied_by?(candidate) }
      end
      result = nil
      methods = search_methods if methods.empty?
      if requirement.composed?
        until result || methods.empty?
          method = methods.shift
          type = method.keys.first
          from = method[type]
          if (include.empty? || !(include & [:all, type, from]).empty?) &&
              (exclude & [:all, type, from]).empty?
            if from.respond_to?(:call)
              versions = from.call(spec.dup)
            else
              versions = send("#{type}_versions", spec.dup, *from)
            end
            result = select[versions]
          end
        end
      end
      result ||= requirement.default
      raise "Could not find #{Artifact.to_spec(spec)}"  +
        "\n You may need to use an specific version instead of a requirement" unless result
      spec.merge :version => result
    end
    
    def requirement?(spec)
      VersionRequirement.requirement?(spec[:version])
    end
    
    private
    def search_methods
      [].tap do
        push :runtime => [Artifact.list]
        push :local => Buildr.repositories.local
        Buildr.repositories.remote.each { |remote| push :remote => remote }
        push :mvnrepository => []
      end
    end

    def depend_version(spec)
      spec[:version][/[\w\.]+/]
    end

    def runtime_versions(spec, artifacts)
      spec_classif = spec.values_at(:group, :id, :type)
      artifacts.inject([]) do |in_memory, str|
        candidate = Artifact.to_hash(str)
        if spec_classif == candidate.values_at(:group, :id, :type)
          in_memory << candidate[:version]
        end
        in_memory
      end
    end
    
    def local_versions(spec, repo)
      path = (spec[:group].split(/\./) + [spec[:id]]).flatten.join('/')
      Dir[File.expand_path(path + "/*", repo)].map { |d| d.pathmap("%f") }.sort.reverse
    end

    def remote_versions(art, base, from = :metadata, fallback = true)
      path = (art[:group].split(/\./) + [art[:id]]).flatten.join('/')
      base ||= "http://mirrors.ibiblio.org/pub/mirrors/maven2"
      uris = {:metadata => "#{base}/#{path}/maven-metadata.xml"}
      uris[:listing] = "#{base}/#{path}/" if base =~ /^https?:/
        xml = nil
      until xml || uris.empty?
        begin
          xml = URI.read(uris.delete(from))
        rescue URI::NotFoundError => e
          from = fallback ? uris.keys.first : nil
        end
      end
      return [] unless xml
      doc = Hpricot(xml)
      case from
      when :metadata then
        doc.search("versions/version").map(&:innerHTML).reverse
      when :listing then
        doc.search("a[@href]").inject([]) { |vers, a|
          vers << a.innerHTML.chop if a.innerHTML[-1..-1] == '/'
          vers
        }.sort.reverse
      else 
        fail "Don't know how to parse #{from}: \n#{xml.inspect}"
      end
    end

    def mvnrepository_versions(art)
      uri = "http://www.mvnrepository.com/artifact/#{art[:group]}/#{art[:id]}"
      xml = begin
              URI.read(uri)
            rescue URI::NotFoundError => e
              puts e.class, e
              return []
            end
      doc = Hpricot(xml)
      doc.search("table.grid/tr/td[1]/a").map(&:innerHTML)
    end

  end # ArtifactSearch
end
