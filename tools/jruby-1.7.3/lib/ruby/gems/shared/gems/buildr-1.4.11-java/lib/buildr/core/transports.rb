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

require 'net/http'
# PATCH:  On Windows, Net::SSH 2.0.2 attempts to load the Pageant DLLs which break on JRuby.
$LOADED_FEATURES << 'net/ssh/authentication/pageant.rb' if RUBY_PLATFORM =~ /java/
Net.autoload :SSH, 'net/ssh'
Net.autoload :SFTP, 'net/sftp'
autoload :CGI, 'cgi'
require 'digest/md5'
require 'digest/sha1'
autoload :ProgressBar, 'buildr/core/progressbar'

# Not quite open-uri, but similar. Provides read and write methods for the resource represented by the URI.
# Currently supports reads for URI::HTTP and writes for URI::SFTP. Also provides convenience methods for
# downloads and uploads.
module URI

  # Raised when trying to read/download a resource that doesn't exist.
  class NotFoundError < RuntimeError
  end

  # How many bytes to read/write at once. Do not change without checking BUILDR-214 first.
  RW_CHUNK_SIZE = 128 * 1024 #:nodoc:

  class << self

    # :call-seq:
    #   read(uri, options?) => content
    #   read(uri, options?) { |chunk| ... }
    #
    # Reads from the resource behind this URI. The first form returns the content of the resource,
    # the second form yields to the block with each chunk of content (usually more than one).
    #
    # For example:
    #   File.open 'image.jpg', 'w' do |file|
    #     URI.read('http://example.com/image.jpg') { |chunk| file.write chunk }
    #   end
    # Shorter version:
    #   File.open('image.jpg', 'w') { |file| file.write URI.read('http://example.com/image.jpg') }
    #
    # Supported options:
    # * :modified -- Only download if file modified since this timestamp. Returns nil if not modified.
    # * :progress -- Show the progress bar while reading.
    def read(uri, options = nil, &block)
      uri = URI.parse(uri.to_s) unless URI === uri
      uri.read options, &block
    end

    # :call-seq:
    #   download(uri, target, options?)
    #
    # Downloads the resource to the target.
    #
    # The target may be a file name (string or task), in which case the file is created from the resource.
    # The target may also be any object that responds to +write+, e.g. File, StringIO, Pipe.
    #
    # Use the progress bar when running in verbose mode.
    def download(uri, target, options = nil)
      uri = URI.parse(uri.to_s) unless URI === uri
      uri.download target, options
    end

    # :call-seq:
    #   write(uri, content, options?)
    #   write(uri, options?) { |bytes| .. }
    #
    # Writes to the resource behind the URI. The first form writes the content from a string or an object
    # that responds to +read+ and optionally +size+. The second form writes the content by yielding to the
    # block. Each yield should return up to the specified number of bytes, the last yield returns nil.
    #
    # For example:
    #   File.open 'killer-app.jar', 'rb' do |file|
    #     write('sftp://localhost/jars/killer-app.jar') { |chunk| file.read(chunk) }
    #   end
    # Or:
    #   write 'sftp://localhost/jars/killer-app.jar', File.read('killer-app.jar')
    #
    # Supported options:
    # * :progress -- Show the progress bar while reading.
    def write(uri, *args, &block)
      uri = URI.parse(uri.to_s) unless URI === uri
      uri.write *args, &block
    end

    # :call-seq:
    #   upload(uri, source, options?)
    #
    # Uploads from source to the resource.
    #
    # The source may be a file name (string or task), in which case the file is uploaded to the resource.
    # The source may also be any object that responds to +read+ (and optionally +size+), e.g. File, StringIO, Pipe.
    #
    # Use the progress bar when running in verbose mode.
    def upload(uri, source, options = nil)
      uri = URI.parse(uri.to_s) unless URI === uri
      uri.upload source, options
    end

  end

  class Generic

    # :call-seq:
    #   read(options?) => content
    #   read(options?) { |chunk| ... }
    #
    # Reads from the resource behind this URI. The first form returns the content of the resource,
    # the second form yields to the block with each chunk of content (usually more than one).
    #
    # For options, see URI::read.
    def read(options = nil, &block)
      fail 'This protocol doesn\'t support reading (yet, how about helping by implementing it?)'
    end

    # :call-seq:
    #   download(target, options?)
    #
    # Downloads the resource to the target.
    #
    # The target may be a file name (string or task), in which case the file is created from the resource.
    # The target may also be any object that responds to +write+, e.g. File, StringIO, Pipe.
    #
    # Use the progress bar when running in verbose mode.
    def download(target, options = nil)
      case target
      when Rake::Task
        download target.name, options
      when String
        # If download breaks we end up with a partial file which is
        # worse than not having a file at all, so download to temporary
        # file and then move over.
        modified = File.stat(target).mtime if File.exist?(target)
        temp = Tempfile.new(File.basename(target))
        temp.binmode
        read({:progress=>verbose}.merge(options || {}).merge(:modified=>modified)) { |chunk| temp.write chunk }
        temp.close
        mkpath File.dirname(target)
        mv temp.path, target
      when File
        read({:progress=>verbose}.merge(options || {}).merge(:modified=>target.mtime)) { |chunk| target.write chunk }
        target.flush
      else
        raise ArgumentError, 'Expecting a target that is either a file name (string, task) or object that responds to write (file, pipe).' unless target.respond_to?(:write)
        read({:progress=>verbose}.merge(options || {})) { |chunk| target.write chunk }
        target.flush
      end
    end

    # :call-seq:
    #   write(content, options?)
    #   write(options?) { |bytes| .. }
    #
    # Writes to the resource behind the URI. The first form writes the content from a string or an object
    # that responds to +read+ and optionally +size+. The second form writes the content by yielding to the
    # block. Each yield should return up to the specified number of bytes, the last yield returns nil.
    #
    # For options, see URI::write.
    def write(*args, &block)
      options = args.pop if Hash === args.last
      options ||= {}
      if String === args.first
        ios = StringIO.new(args.first, 'r')
        write(options.merge(:size=>args.first.size)) { |bytes| ios.read(bytes) }
      elsif args.first.respond_to?(:read)
        size = args.first.size rescue nil
        write({:size=>size}.merge(options)) { |bytes| args.first.read(bytes) }
      elsif args.empty? && block
        write_internal options, &block
      else
        raise ArgumentError, 'Either give me the content, or pass me a block, otherwise what would I upload?'
      end
    end

    # :call-seq:
    #   upload(source, options?)
    #
    # Uploads from source to the resource.
    #
    # The source may be a file name (string or task), in which case the file is uploaded to the resource.
    # If the source is a directory, uploads all files inside the directory (including nested directories).
    # The source may also be any object that responds to +read+ (and optionally +size+), e.g. File, StringIO, Pipe.
    #
    # Use the progress bar when running in verbose mode.
    def upload(source, options = nil)
      source = source.name if Rake::Task === source
      options ||= {}
      if String === source
        raise NotFoundError, 'No source file/directory to upload.' unless File.exist?(source)
        if File.directory?(source)
          Dir.glob("#{source}/**/*").reject { |file| File.directory?(file) }.each do |file|
            uri = self + (File.join(self.path, file.sub(source, '')))
            uri.upload file, {:digests=>[]}.merge(options)
          end
        else
          File.open(source, 'rb') { |input| upload input, options }
        end
      elsif source.respond_to?(:read)
        digests = (options[:digests] || [:md5, :sha1]).
          inject({}) { |hash, name| hash[name] = Digest.const_get(name.to_s.upcase).new ; hash }
        size = source.stat.size rescue nil
        write (options).merge(:progress=>verbose && size, :size=>size) do |bytes|
          source.read(bytes).tap do |chunk|
            digests.values.each { |digest| digest << chunk } if chunk
          end
        end
        digests.each do |key, digest|
          self.merge("#{self.path}.#{key}").write digest.hexdigest,
            (options).merge(:progress=>false)
        end
      else
        raise ArgumentError, 'Expecting source to be a file name (string, task) or any object that responds to read (file, pipe).'
      end
    end

  protected

    # :call-seq:
    #   with_progress_bar(show, file_name, size) { |progress| ... }
    #
    # Displays a progress bar while executing the block. The first argument must be true for the
    # progress bar to show (TTY output also required), as a convenient for selectively using the
    # progress bar from a single block.
    #
    # The second argument provides a filename to display, the third its size in bytes.
    #
    # The block is yielded with a progress object that implements a single method.
    # Call << for each block of bytes down/uploaded.
    def with_progress_bar(show, file_name, size, &block) #:nodoc:
      options = { :total=>size || 0, :title=>file_name }
      options[:hidden] = true unless show
      ProgressBar.start options, &block
    end

    # :call-seq:
    #   proxy_uri => URI?
    #
    # Returns the proxy server to use. Obtains the proxy from the relevant environment variable (e.g. HTTP_PROXY).
    # Supports exclusions based on host name and port number from environment variable NO_PROXY.
    def proxy_uri
      proxy = ENV["#{scheme.upcase}_PROXY"]
      proxy = URI.parse(proxy) if String === proxy
      excludes = ENV['NO_PROXY'].to_s.split(/\s*,\s*/).compact
      excludes = excludes.map { |exclude| exclude =~ /:\d+$/ ? exclude : "#{exclude}:*" }
      return proxy unless excludes.any? { |exclude| File.fnmatch(exclude, "#{host}:#{port}") }
    end

    def write_internal(options, &block) #:nodoc:
      fail 'This protocol doesn\'t support writing (yet, how about helping by implementing it?)'
    end

  end


  class HTTP #:nodoc:

    # See URI::Generic#read
    def read(options = nil, &block)
      options ||= {}
      connect do |http|
        trace "Requesting #{self}"
        headers = { 'If-Modified-Since' => CGI.rfc1123_date(options[:modified].utc) } if options[:modified]
        request = Net::HTTP::Get.new(request_uri.empty? ? '/' : request_uri, headers)
        request.basic_auth self.user, self.password if self.user
        http.request request do |response|
          case response
          when Net::HTTPNotModified
            # No modification, nothing to do.
            trace 'Not modified since last download'
            return nil
          when Net::HTTPRedirection
            # Try to download from the new URI, handle relative redirects.
            trace "Redirected to #{response['Location']}"
            rself = self + URI.parse(response['Location'])
            rself.user, rself.password = self.user, self.password
            return rself.read(options, &block)
          when Net::HTTPOK
            info "Downloading #{self}"
            result = nil
            with_progress_bar options[:progress], path.split('/').last, response.content_length do |progress|
              if block
                response.read_body do |chunk|
                  block.call chunk
                  progress << chunk
                end
              else
                result = ''
                response.read_body do |chunk|
                  result << chunk
                  progress << chunk
                end
              end
            end
            return result
          when Net::HTTPUnauthorized
            raise NotFoundError, "Looking for #{self} but repository says Unauthorized/401."
          when Net::HTTPNotFound
            raise NotFoundError, "Looking for #{self} and all I got was a 404!"
          else
            raise RuntimeError, "Failed to download #{self}: #{response.message}"
          end
        end
      end
    end

  private

    def write_internal(options, &block) #:nodoc:
      options ||= {}
      connect do |http|
        trace "Uploading to #{path}"
        content = StringIO.new
        while chunk = yield(RW_CHUNK_SIZE)
          content << chunk
        end
        headers = { 'Content-MD5'=>Digest::MD5.hexdigest(content.string), 'Content-Type'=>'application/octet-stream' }
        request = Net::HTTP::Put.new(request_uri.empty? ? '/' : request_uri, headers)
        request.basic_auth self.user, self.password if self.user
        response = nil
        with_progress_bar options[:progress], path.split('/').last, content.size do |progress|
          request.content_length = content.size
          content.rewind
          stream = Object.new
          class << stream ; self ;end.send :define_method, :read do |count|
            bytes = content.read(count)
            progress << bytes if bytes
            bytes
          end
          request.body_stream = stream
          response = http.request(request)
        end

        case response
        when Net::HTTPRedirection
          # Try to download from the new URI, handle relative redirects.
          trace "Redirected to #{response['Location']}"
          content.rewind
          return (self + URI.parse(response['location'])).write_internal(options) { |bytes| content.read(bytes) }
        when Net::HTTPSuccess
        else
          raise RuntimeError, "Failed to upload #{self}: #{response.message}"
        end
      end
    end

    def connect
      if proxy = proxy_uri
        proxy = URI.parse(proxy) if String === proxy
        http = Net::HTTP.new(host, port, proxy.host, proxy.port, proxy.user, proxy.password)
      else
        http = Net::HTTP.new(host, port)
      end
      if self.instance_of? URI::HTTPS
        require 'net/https'
        http.use_ssl = true
      end
      yield http
    end

  end


  class SFTP < Generic #:nodoc:

    DEFAULT_PORT = 22
    COMPONENT = [ :scheme, :userinfo, :host, :port, :path ].freeze

    class << self
      # Caching of passwords, so we only need to ask once.
      def passwords
        @passwords ||= {}
      end
    end

    def initialize(*arg)
      super
    end

    def read(options = {}, &block)
      # SSH options are based on the username/password from the URI.
      ssh_options = { :port=>port, :password=>password }.merge(options[:ssh_options] || {})
      ssh_options[:password] ||= SFTP.passwords[host]
      begin
        trace "Connecting to #{host}"
        if block
          result = nil
        else
          result = ''
          block = lambda { |chunk| result << chunk }
        end
        Net::SFTP.start(host, user, ssh_options) do |sftp|
          SFTP.passwords[host] = ssh_options[:password]
          trace 'connected'

          with_progress_bar options[:progress] && options[:size], path.split('/').last, options[:size] || 0 do |progress|
            trace "Downloading from #{path}"
            sftp.file.open(path, 'r') do |file|
              while chunk = file.read(RW_CHUNK_SIZE)
                block.call chunk
                progress << chunk
                break if chunk.size < RW_CHUNK_SIZE
              end
            end
          end
        end
        return result
      rescue Net::SSH::AuthenticationFailed=>ex
        # Only if running with console, prompt for password.
        if !ssh_options[:password] && $stdout.isatty
          password = ask("Password for #{host}:") { |q| q.echo = '*' }
          ssh_options[:password] = password
          retry
        end
        raise
      end
    end

  protected

    def write_internal(options, &block) #:nodoc:
      # SSH options are based on the username/password from the URI.
      ssh_options = { :port=>port, :password=>password }.merge(options[:ssh_options] || {})
      ssh_options[:password] ||= SFTP.passwords[host]
      begin
        trace "Connecting to #{host}"
        Net::SFTP.start(host, user, ssh_options) do |sftp|
          SFTP.passwords[host] = ssh_options[:password]
          trace 'Connected'

          # To create a path, we need to create all its parent. We use realpath to determine if
          # the path already exists, otherwise mkdir fails.
          trace "Creating path #{path}"
          File.dirname(path).split('/').reject(&:empty?).inject('/') do |base, part|
            combined = base + part
            sftp.close(sftp.opendir!(combined)) rescue sftp.mkdir! combined, {}
            "#{combined}/"
          end

          with_progress_bar options[:progress] && options[:size], path.split('/').last, options[:size] || 0 do |progress|
            trace "Uploading to #{path}"
            sftp.file.open(path, 'w') do |file|
              while chunk = yield(RW_CHUNK_SIZE)
                file.write chunk
                progress << chunk
              end
              sftp.setstat(path, :permissions => options[:permissions]) if options[:permissions]
            end
          end
        end
      rescue Net::SSH::AuthenticationFailed=>ex
        # Only if running with console, prompt for password.
        if !ssh_options[:password] && $stdout.isatty
          password = ask("Password for #{host}:") { |q| q.echo = '*' }
          ssh_options[:password] = password
          retry
        end
        raise
      end
    end

  end

  @@schemes['SFTP'] = SFTP


  # File URL. Keep in mind that file URLs take the form of <code>file://host/path</code>, although the host
  # is not used, so typically all you will see are three backslashes. This methods accept common variants,
  # like <code>file:/path</code> but always returns a valid URL.
  class FILE < Generic

    COMPONENT = [ :host, :path ].freeze

    def upload(source, options = nil)
      super
      if File === source then
        File.chmod(source.stat.mode, real_path)
      end
    end

    def initialize(*args)
      super
      # file:something (opaque) becomes file:///something
      if path.nil?
        set_path "/#{opaque}"
        unless opaque.nil?
          set_opaque nil
          warn "#{caller[2]}: We'll accept this URL, but just so you know, it needs three slashes, as in: #{to_s}"
        end
      end
      # Sadly, file://something really means file://something/ (something being server)
      set_path '/' if path.empty?

      # On windows, file://c:/something is not a valid URL, but people do it anyway, so if we see a drive-as-host,
      # we'll just be nice enough to fix it. (URI actually strips the colon here)
      if host =~ /^[a-zA-Z]$/
        set_path "/#{host}:#{path}"
        set_host nil
      end
    end

    # See URI::Generic#read
    def read(options = nil, &block)
      options ||= {}
      raise ArgumentError, 'Either you\'re attempting to read a file from another host (which we don\'t support), or you used two slashes by mistake, where you should have file:///<path>.' if host

      path = real_path
      # TODO: complain about clunky URLs
      raise NotFoundError, "Looking for #{self} and can't find it." unless File.exists?(path)
      raise NotFoundError, "Looking for the file #{self}, and it happens to be a directory." if File.directory?(path)
      File.open path, 'rb' do |input|
        with_progress_bar options[:progress], path.split('/').last, input.stat.size do |progress|
          block ? block.call(input.read) : input.read
        end
      end
    end

    def to_s
      "file://#{host}#{path}"
    end

    # Returns the file system path based that corresponds to the URL path.
    # On windows this method strips the leading slash off of the path.
    # On all platforms this method unescapes the URL path.
    def real_path #:nodoc:
      real_path = Buildr::Util.win_os? && path =~ /^\/[a-zA-Z]:\// ? path[1..-1] : path
      URI.unescape(real_path)
    end

  protected

    def write_internal(options, &block) #:nodoc:
      raise ArgumentError, 'Either you\'re attempting to write a file to another host (which we don\'t support), or you used two slashes by mistake, where you should have file:///<path>.' if host
      temp = Tempfile.new(File.basename(path))
      temp.binmode
      with_progress_bar options[:progress] && options[:size], path.split('/').last, options[:size] || 0 do |progress|
        while chunk = yield(RW_CHUNK_SIZE)
          temp.write chunk
          progress << chunk
        end
      end
      temp.close
      mkpath File.dirname(real_path)
      mv temp.path, real_path
      real_path
    end

    @@schemes['FILE'] = FILE

  end

end
