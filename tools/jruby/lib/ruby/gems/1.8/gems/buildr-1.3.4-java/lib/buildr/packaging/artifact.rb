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


require 'buildr/core/project'
require 'buildr/core/transports'
require 'buildr/packaging/artifact_namespace'


module Buildr

  desc 'Download all artifacts'
  task 'artifacts'

  desc "Download all artifacts' sources"
  task 'artifacts:sources'

  # Mixin with a task to make it behave like an artifact. Implemented by the packaging tasks.
  #
  # An artifact has an identifier, group identifier, type, version number and
  # optional classifier. All can be used to locate it in the local repository,
  # download from or upload to a remote repository.
  #
  # The #to_spec and #to_hash methods allow it to be used everywhere an artifact is
  # accepted.
  module ActsAsArtifact

    ARTIFACT_ATTRIBUTES = [:group, :id, :type, :classifier, :version]

    class << self
    private
      def included(mod)
        mod.extend self
      end
    end

    # The artifact identifier.
    attr_reader :id
    # The group identifier.
    attr_reader :group
    # The file type. (Symbol)
    attr_reader :type
    # The version number.
    attr_reader :version
    # Optional artifact classifier.
    attr_reader :classifier

    def snapshot?
      version =~ /-SNAPSHOT$/
    end

    # :call-seq:
    #   to_spec_hash => Hash
    #
    # Returns the artifact specification as a hash. For example:
    #   com.example:app:jar:1.2
    # becomes:
    #   { :group=>'com.example',
    #     :id=>'app',
    #     :type=>:jar,
    #     :version=>'1.2' }
    def to_spec_hash
      base = { :group=>group, :id=>id, :type=>type, :version=>version }
      classifier ? base.merge(:classifier=>classifier) : base
    end
    alias_method :to_hash, :to_spec_hash

    # :call-seq:
    #   to_spec => String
    #
    # Returns the artifact specification, in the structure:
    #   <group>:<artifact>:<type>:<version>
    # or
    #   <group>:<artifact>:<type>:<classifier>:<version>
    def to_spec
      classifier ? "#{group}:#{id}:#{type}:#{classifier}:#{version}" : "#{group}:#{id}:#{type}:#{version}"
    end

    # :call-seq:
    #   pom => Artifact
    # 
    # Convenience method that returns a POM artifact.
    def pom
      return self if type == :pom
      Buildr.artifact(:group=>group, :id=>id, :version=>version, :type=>:pom)
    end

    # :call-seq:
    #   sources_artifact => Artifact
    # 
    # Convenience method that returns a sources artifact.
    def sources_artifact
      sources_spec = to_spec_hash.merge(:classifier=>'sources')
      sources_task = OptionalArtifact.define_task(Buildr.repositories.locate(sources_spec))
      sources_task.send :apply_spec, sources_spec
      sources_task
    end

    # :call-seq:
    #   pom_xml => string
    #
    # Creates POM XML for this artifact.
    def pom_xml
      xml = Builder::XmlMarkup.new(:indent=>2)
      xml.instruct!
      xml.project do
        xml.modelVersion  '4.0.0'
        xml.groupId       group
        xml.artifactId    id
        xml.version       version
        xml.classifier    classifier if classifier
      end
    end
    
    def install
      pom.install if pom && pom != self
      invoke
      installed = Buildr.repositories.locate(self)
      unless installed == name # If not already in local repository.
        mkpath File.dirname(installed)
        cp name, installed
        info "Installed #{installed}"
      end
    end

    def uninstall
      installed = Buildr.repositories.locate(self)
      rm installed if File.exist?(installed) 
      pom.uninstall if pom && pom != self
    end

    # :call-seq:
    #   upload
    #   upload(url)
    #   upload(options)
    #
    # Uploads the artifact, its POM and digital signatures to remote server.
    #
    # In the first form, uses the upload options specified by repositories.release_to.
    # In the second form, uses a URL that includes all the relevant information.
    # In the third form, uses a hash with the options :url, :username, :password,
    # and :permissions. All but :url are optional.
    def upload(upload_to = nil)
      # Where do we release to?
      upload_to ||= Buildr.repositories.release_to
      upload_to = { :url=>upload_to } unless Hash === upload_to
      raise ArgumentError, 'Don\'t know where to upload, perhaps you forgot to set repositories.release_to' unless upload_to[:url]
      invoke # Make sure we exist.

      # Upload POM ahead of package, so we don't fail and find POM-less package (the horror!)
      pom.upload(upload_to) if pom && pom != self

      # Set the upload URI, including mandatory slash (we expect it to be the base directory).
      # Username/password may be part of URI, or separate entities.
      uri = URI.parse(upload_to[:url].clone)
      uri.path = uri.path + '/' unless uri.path[-1] == '/'
      uri.user = upload_to[:username] if upload_to[:username]
      uri.password = upload_to[:password] if upload_to[:password]

      # Upload artifact relative to base URL, need to create path before uploading.
      info "Deploying #{to_spec}"
      path = group.gsub('.', '/') + "/#{id}/#{version}/#{File.basename(name)}"
      URI.upload uri + path, name, :permissions=>upload_to[:permissions]
    end

  protected

    # Apply specification to this artifact.
    def apply_spec(spec)
      spec = Artifact.to_hash(spec)
      ARTIFACT_ATTRIBUTES.each { |key| instance_variable_set("@#{key}", spec[key]) }
      self
    end
    
    def group_path
      group.gsub('.', '/')
    end

  end

 
  # A file task referencing an artifact in the local repository.
  #
  # This task includes all the artifact attributes (group, id, version, etc). It points
  # to the artifact's path in the local repository. When invoked, it will download the
  # artifact into the local repository if the artifact does not already exist.
  #
  # Note: You can enhance this task to create the artifact yourself, e.g. download it from
  # a site that doesn't have a remote repository structure, copy it from a different disk, etc.
  class Artifact < Rake::FileCreationTask

    # The default artifact type.
    DEFAULT_TYPE = :jar

    include ActsAsArtifact

    class << self

      # :call-seq:
      #   lookup(spec) => Artifact
      #
      # Lookup a previously registered artifact task based on its specification (String or Hash).
      def lookup(spec)
        @artifacts ||= {}
        @artifacts[to_spec(spec)]
      end

      # :call-seq:
      #   list => specs
      #
      # Returns an array of specs for all the registered artifacts. (Anything created from artifact, or package).
      def list
        @artifacts ||= {}
        @artifacts.keys
      end

      # :call-seq:
      #   register(artifacts) => artifacts
      #
      # Register an artifact task(s) for later lookup (see #lookup).
      def register(*tasks)
        @artifacts ||= {}
        fail 'You can only register an artifact task, one of the arguments is not a Task that responds to to_spec' unless
          tasks.all? { |task| task.respond_to?(:to_spec) && task.respond_to?(:invoke) }
        tasks.each { |task| @artifacts[task.to_spec] = task }
        tasks
      end

      # :call-seq:
      #   to_hash(spec_hash) => spec_hash
      #   to_hash(spec_string) => spec_hash
      #   to_hash(artifact) => spec_hash
      #
      # Turn a spec into a hash. This method accepts a String, Hash or any object that responds to
      # the method to_spec. There are several reasons to use this method:
      # * You can pass anything that could possibly be a spec, and get a hash.
      # * It will check that the spec includes the group identifier, artifact
      #   identifier and version number and set the file type, if missing.
      # * It will always return a new specs hash.
      def to_hash(spec)
        if spec.respond_to?(:to_spec)
          to_hash spec.to_spec
        elsif Hash === spec
          rake_check_options spec, :id, :group, :type, :classifier, :version
          # Sanitize the hash and check it's valid.
          spec = ARTIFACT_ATTRIBUTES.inject({}) { |h, k| h[k] = spec[k].to_s if spec[k] ; h }
          fail "Missing group identifier for #{spec.inspect}" unless spec[:group]
          fail "Missing artifact identifier for #{spec.inspect}" unless spec[:id]
          fail "Missing version for #{spec.inspect}" unless spec[:version]
          spec[:type] = (spec[:type] || DEFAULT_TYPE).to_sym
          spec
        elsif String === spec
          group, id, type, version, *rest = spec.split(':').map { |part| part.empty? ? nil : part }
          unless rest.empty?
            # Optional classifier comes before version.
            classifier, version = version, rest.shift
            fail "Expecting <group:id:type:version> or <group:id:type:classifier:version>, found <#{spec}>" unless rest.empty?
          end
          to_hash :group=>group, :id=>id, :type=>type, :version=>version, :classifier=>classifier
        else
          fail 'Expecting a String, Hash or object that responds to to_spec'
        end
      end

      # :call-seq:
      #   to_spec(spec_hash) => spec_string
      #
      # Convert a hash back to a spec string. This method accepts
      # a string, hash or any object that responds to to_spec.
      def to_spec(hash)
        hash = to_hash(hash) unless Hash === hash
        version = ":#{hash[:version]}" if hash[:version]
        classifier = ":#{hash[:classifier]}" if hash[:classifier]
        "#{hash[:group]}:#{hash[:id]}:#{hash[:type] || DEFAULT_TYPE}#{classifier}#{version}"
      end

      # :call-seq:
      #   hash_to_file_name(spec_hash) => file_name
      #
      # Convert a hash spec to a file name.
      def hash_to_file_name(hash)
        version = "-#{hash[:version]}" if hash[:version]
        classifier = "-#{hash[:classifier]}" if hash[:classifier]
        "#{hash[:id]}#{version}#{classifier}.#{hash[:type] || DEFAULT_TYPE}"
      end

    end

    def initialize(*args) #:nodoc:
      super
      enhance do |task|
        # Default behavior: download the artifact from one of the remote repositories
        # if the file does not exist. But this default behavior is counter productive
        # if the artifact knows how to build itself (e.g. download from a different location),
        # so don't perform it if the task found a different way to create the artifact.
        task.enhance do
          unless File.exist?(name)
            info "Downloading #{to_spec}"
            download
            pom.invoke rescue nil if pom && pom != self
          end
        end
      end
    end

    # :call-seq:
    #   from(path) => self
    #
    # Use this when you want to install or upload an artifact from a given file, for example:
    #   test = artifact('group:id:jar:1.0').from('test.jar')
    #   install test
    # See also Buildr#install and Buildr#upload.
    def from(path)
      path = File.expand_path(path.to_s)
      enhance [path] do
        mkpath File.dirname(name)
        pom.invoke unless type == :pom
        cp path, name
        info "Installed #{path} as #{to_spec}"
      end
      unless type == :pom
        pom.enhance do
          mkpath File.dirname(pom.name)
          File.open(pom.name, 'w') { |file| file.write pom.pom_xml }
        end
      end
      self
    end

  protected

    # :call-seq:
    #   download
    # 
    # Downloads an artifact from one of the remote repositories, and stores it in the local
    # repository. Raises an exception if the artifact is not found.
    #
    # This method attempts to download the artifact from each repository in the order in
    # which they are returned from #remote, until successful.
    def download
      trace "Downloading #{to_spec}"
      remote = Buildr.repositories.remote.map { |repo_url| URI === repo_url ? repo_url : URI.parse(repo_url) }
      remote = remote.each { |repo_url| repo_url.path += '/' unless repo_url.path[-1] == '/' }
      fail 'No remote repositories defined!' if remote.empty?
      exact_success = remote.find do |repo_url|
        begin
          path = "#{group_path}/#{id}/#{version}/#{File.basename(name)}"
          URI.download repo_url + path, name
          true
        rescue URI::NotFoundError
          false
        rescue Exception=>error
          info error
          trace error.backtrace.join("\n")
          false
        end
      end

      if exact_success
        return
      elsif snapshot?
        download_m2_snapshot(remote)
      else
        fail_download(remote)
      end
    end

    def download_m2_snapshot(remote_uris)
      remote_uris.find do |repo_url|
        snapshot_url = current_snapshot_repo_url(repo_url)
        if snapshot_url
          begin
            URI.download snapshot_url, name
          rescue URI::NotFoundError
            false
          end
        else
          false
        end
      end or fail_download(remote_uris)
    end

    def current_snapshot_repo_url(repo_url)
      begin
        metadata_path = "#{group_path}/#{id}/#{version}/maven-metadata.xml"
        metadata_xml = StringIO.new
        URI.download repo_url + metadata_path, metadata_xml
        metadata = REXML::Document.new(metadata_xml.string).root
        timestamp = REXML::XPath.first(metadata, '//timestamp').text
        build_number = REXML::XPath.first(metadata, '//buildNumber').text
        snapshot_of = version[0, version.size - 9]
        repo_url + "#{group_path}/#{id}/#{version}/#{id}-#{snapshot_of}-#{timestamp}-#{build_number}.#{type}"
      rescue URI::NotFoundError
        nil
      end
    end

    def fail_download(remote_uris)
      fail "Failed to download #{to_spec}, tried the following repositories:\n#{remote_uris.join("\n")}"
    end
  end
  
  
  # An artifact that is optional.
  # If downloading fails, the user will be informed but it will not raise an exception.
  class OptionalArtifact < Artifact
    
    protected
    
    # If downloading fails, the user will be informed but it will not raise an exception.
    def download
      super
    rescue 
      info "Failed to download #{to_spec}. Skipping it."
    end
    
  end


  # Holds the path to the local repository, URLs for remote repositories, and settings for release server.
  #
  # You can access this object from the #repositories method. For example:
  #   puts repositories.local
  #   repositories.remote << 'http://example.com/repo'
  #   repositories.release_to = 'sftp://example.com/var/www/public/repo'
  class Repositories
    include Singleton

    # :call-seq:
    #   local => path
    #
    # Returns the path to the local repository.
    #
    # The default path is .m2/repository relative to the home directory.
    # You can set this using the M2_REPO environment variable or the repositories/local
    # value in your settings.yaml file.
    def local
      @local ||= File.expand_path(ENV['M2_REPO'] || ENV['local_repo'] ||
        (Buildr.settings.user['repositories'] && Buildr.settings.user['repositories']['local']) ||
        File.join(ENV['HOME'], '.m2/repository'))
    end

    # :call-seq:
    #   local = path
    #
    # Sets the path to the local repository.
    #
    # The best place to set the local repository path is from a buildr.rb file
    # located in your home directory. That way all your projects will share the same
    # path, without affecting other developers collaborating on these projects.
    def local=(dir)
      @local = dir ? File.expand_path(dir) : nil
    end

    # :call-seq:
    #   locate(spec) => path
    #
    # Locates an artifact in the local repository based on its specification, and returns
    # a file path.
    #
    # For example:
    #   locate :group=>'log4j', :id=>'log4j', :version=>'1.1'
    #     => ~/.m2/repository/log4j/log4j/1.1/log4j-1.1.jar
    def locate(spec)
      spec = Artifact.to_hash(spec)
      File.join(local, spec[:group].split('.'), spec[:id], spec[:version], Artifact.hash_to_file_name(spec))
    end

    # :call-seq:
    #   remote => Array
    #
    # Returns an array of all the remote repository URLs.
    #
    # When downloading artifacts, repositories are accessed in the order in which they appear here.
    # The best way is to add repositories individually, for example:
    #   repositories.remote << 'http://example.com/repo'
    #
    # You can also specify remote repositories in the settings.yaml (per user) and build.yaml (per build)
    # files.  Both sets of URLs are loaded by default into this array, URLs from the personal setting
    # showing first.
    #
    # For example:
    #   repositories:
    #     remote:
    #     - http://example.com/repo
    #     - http://elsewhere.com/repo
    def remote
      unless @remote
        @remote = [Buildr.settings.user, Buildr.settings.build].inject([]) { |repos, hash|
          repos | Array(hash['repositories'] && hash['repositories']['remote'])
        }
      end
      @remote
    end

    # :call-seq:
    #   remote = Array
    #   remote = url
    #   remote = nil
    #
    # With a String argument, clears the array and set it to that single URL.
    #
    # With an Array argument, clears the array and set it to these specific URLs.
    #
    # With nil, clears the array.
    def remote=(urls)
      case urls
      when nil then @remote = nil
      when Array then @remote = urls.dup
      else @remote = [urls.to_s]
      end
    end

    # :call-seq:
    #   release_to = url
    #   release_to = hash
    #
    # Specifies the release server. Accepts a Hash with different repository settings
    # (e.g. url, username, password), or a String to only set the repository URL.
    #
    # Besides the URL, all other settings depend on the transport protocol in use.
    #
    # For example:
    #   repositories.release_to = 'sftp://john:secret@example.com/var/www/repo/'
    #
    #   repositories.release_to = { :url=>'sftp://example.com/var/www/repo/',
    #                                :username='john', :password=>'secret' }
    # Or in the settings.yaml file:
    #   repositories:
    #     release_to: sftp://john:secret@example.com/var/www/repo/
    #
    #   repositories:
    #     release_to:
    #       url: sftp://example.com/var/www/repo/
    #       username: john
    #       password: secret
    def release_to=(options)
      options = { :url=>options } unless Hash === options
      @release_to = options
    end

    # :call-seq:
    #   release_to => hash
    #
    # Returns the current release server setting as a Hash. This is a more convenient way to
    # configure the settings, as it allows you to specify the settings progressively.
    #
    # For example, the Buildfile will contain the repository URL used by all developers:
    #   repositories.release_to[:url] ||= 'sftp://example.com/var/www/repo'
    # Your private buildr.rb will contain your credentials:
    #   repositories.release_to[:username] = 'john'
    #   repositories.release_to[:password] = 'secret'
    def release_to
      unless @release_to
        value = Buildr.settings.user['repositories'] && Buildr.settings.user['repositories']['release_to']
        @release_to = Hash === value ? value.inject({}) { |hash, (key, value)| hash.update(key.to_sym=>value) } : { :url=>Array(value).first }
      end
      @release_to
    end

  end

  # :call-seq:
  #    repositories => Repositories
  #
  # Returns an object you can use for setting the local repository path, remote repositories
  # URL and release server settings.
  #
  # See Repositories.
  def repositories
    Repositories.instance
  end

  # :call-seq:
  #   artifact(spec) => Artifact
  #   artifact(spec) { |task| ... } => Artifact
  #
  # Creates a file task to download and install the specified artifact in the local repository.
  #
  # You can use a String or a Hash for the artifact specification. The file task will point at
  # the artifact's path inside the local repository. You can then use this tasks as a prerequisite
  # for other tasks.
  #
  # This task will download and install the artifact only once. In fact, it will download and
  # install the artifact if the artifact does not already exist. You can enhance it if you have
  # a different way of creating the artifact in the local repository. See Artifact for more details.
  #
  # For example, to specify an artifact:
  #   artifact('log4j:log4j:jar:1.1')
  #
  # To use the artifact in a task:
  #   compile.with artifact('log4j:log4j:jar:1.1')
  #
  # To specify an artifact and the means for creating it:
  #   download(artifact('dojo:dojo-widget:zip:2.0')=>
  #     'http://download.dojotoolkit.org/release-2.0/dojo-2.0-widget.zip')
  def artifact(spec, &block) #:yields:task
    spec = artifact_ns.fetch(spec) if spec.kind_of?(Symbol)
    spec = Artifact.to_hash(spec)
    unless task = Artifact.lookup(spec)
      task = Artifact.define_task(repositories.locate(spec))
      task.send :apply_spec, spec
      Rake::Task['rake:artifacts'].enhance [task]
      Artifact.register(task)
      Rake::Task['artifacts:sources'].enhance [task.sources_artifact] unless spec[:type] == :pom
    end
    task.enhance &block
  end

  # :call-seq:
  #   artifacts(*spec) => artifacts
  #
  # Handles multiple artifacts at a time. This method is the plural equivalent of
  # #artifact, but can do more things.
  #
  # Returns an array of artifacts built using the supplied
  # specifications, each of which can be:
  # * An artifact specification (String or Hash). Returns the appropriate Artifact task.
  # * An artifact of any other task. Returns the task as is.
  # * A project. Returns all artifacts created (packaged) by that project.
  # * A string. Returns that string, assumed to be a file name.
  # * An array of artifacts or a Struct.
  # * A symbol. Returns the named artifact from the current ArtifactNamespace
  #
  # For example, handling a collection of artifacts:
  #   xml = [ xerces, xalan, jaxp ]
  #   ws = [ axis, jax-ws, jaxb ]
  #   db = [ jpa, mysql, sqltools ]
  #   artifacts(xml, ws, db)
  #
  # Using artifacts created by a project:
  #   artifacts project('my-app')               # All packages
  #   artifacts project('my-app').package(:war) # Only the WAR
  def artifacts(*specs, &block)
    specs.flatten.inject([]) do |set, spec|
      case spec
      when ArtifactNamespace
        set |= spec.artifacts
      when Symbol, Hash
        set |= [artifact(spec)]
      when /([^:]+:){2,4}/ # A spec as opposed to a file name.
        set |= [artifact(spec)]
      when String # Must always expand path.
        set |= [File.expand_path(spec)]
      when Project
        set |= artifacts(spec.packages)
      when Rake::Task
        set |= [spec]
      when Struct
        set |= artifacts(spec.values)
      else
        fail "Invalid artifact specification in #{specs.inspect}"
      end
    end
  end

  def transitive(*specs)
    specs.flatten.inject([]) do |set, spec|
      case spec
      when /([^:]+:){2,4}/ # A spec as opposed to a file name.
        artifact = artifact(spec)
        set |= [artifact] unless artifact.type == :pom
        set |= POM.load(artifact.pom).dependencies.map { |spec| artifact(spec) }
      when Hash
        set |= [transitive(spec)]
      when String # Must always expand path.
        set |= transitive(file(File.expand_path(spec)))
      when Project
        set |= transitive(spec.packages)
      when Rake::Task
        set |= spec.respond_to?(:to_spec) ? transitive(spec.to_spec) : [spec]
      when Struct
        set |= transitive(spec.values)
      else
        fail "Invalid artifact specification in: #{specs.to_s}"
      end
    end
  end

  # :call-seq:
  #   group(ids, :under=>group_name, :version=>number) => artifacts
  #
  # Convenience method for defining multiple artifacts that belong to the same group, type and version.
  # Accepts multiple artifact identifiers followed by two or three hash values:
  # * :under -- The group identifier
  # * :version -- The version number
  # * :type -- The artifact type (optional)
  #
  # For example:
  #   group 'xbean', 'xbean_xpath', 'xmlpublic', :under=>'xmlbeans', :version=>'2.1.0'
  # Or:
  #   group %w{xbean xbean_xpath xmlpublic}, :under=>'xmlbeans', :version=>'2.1.0'
  def group(*args)
    hash = args.pop
    args.flatten.map { |id| artifact :group=>hash[:under], :type=>hash[:type], :version=>hash[:version], :id=>id }
  end 

  # :call-seq:
  #   install(artifacts)
  #
  # Installs the specified artifacts in the local repository as part of the install task.
  #
  # You can use this to install various files in the local repository, for example:
  #   install artifact('group:id:jar:1.0').from('some_jar.jar')
  #   $ buildr install
  def install(*args, &block)
    artifacts = artifacts(args)
    raise ArgumentError, 'This method can only install artifacts' unless artifacts.all? { |f| f.respond_to?(:to_spec) }
    all = (artifacts + artifacts.map { |artifact| artifact.pom }).uniq
    task('install').tap do |task|
      task.enhance all, &block
      task 'uninstall' do
        all.map(&:to_s ).each { |file| rm file if File.exist?(file) }
      end
    end
  end

  # :call-seq:
  #   upload(artifacts)
  #
  # Uploads the specified artifacts to the release server as part of the upload task.
  #
  # You can use this to upload various files to the release server, for example:
  #   upload artifact('group:id:jar:1.0').from('some_jar.jar')
  #   $ buildr upload
  def upload(*args, &block)
    artifacts = artifacts(args)
    raise ArgumentError, 'This method can only upload artifacts' unless artifacts.all? { |f| f.respond_to?(:to_spec) }
    task('upload').tap do |task|
      task.enhance &block if block
      task.enhance artifacts do
        artifacts.each { |artifact| artifact.upload }
      end
    end
  end

end
