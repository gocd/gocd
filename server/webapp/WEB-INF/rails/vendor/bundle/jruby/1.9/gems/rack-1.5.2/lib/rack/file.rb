require 'time'
require 'rack/utils'
require 'rack/mime'

module Rack
  # Rack::File serves files below the +root+ directory given, according to the
  # path info of the Rack request.
  # e.g. when Rack::File.new("/etc") is used, you can access 'passwd' file
  # as http://localhost:9292/passwd
  #
  # Handlers can detect if bodies are a Rack::File, and use mechanisms
  # like sendfile on the +path+.

  class File
    SEPS = Regexp.union(*[::File::SEPARATOR, ::File::ALT_SEPARATOR].compact)
    ALLOWED_VERBS = %w[GET HEAD]

    attr_accessor :root
    attr_accessor :path
    attr_accessor :cache_control

    alias :to_path :path

    def initialize(root, headers={}, default_mime = 'text/plain')
      @root = root
      @headers = headers
      @default_mime = default_mime
    end

    def call(env)
      dup._call(env)
    end

    F = ::File

    def _call(env)
      unless ALLOWED_VERBS.include? env["REQUEST_METHOD"]
        return fail(405, "Method Not Allowed")
      end

      path_info = Utils.unescape(env["PATH_INFO"])
      parts = path_info.split SEPS

      clean = []

      parts.each do |part|
        next if part.empty? || part == '.'
        part == '..' ? clean.pop : clean << part
      end

      @path = F.join(@root, *clean)

      available = begin
        F.file?(@path) && F.readable?(@path)
      rescue SystemCallError
        false
      end

      if available
        serving(env)
      else
        fail(404, "File not found: #{path_info}")
      end
    end

    def serving(env)
      last_modified = F.mtime(@path).httpdate
      return [304, {}, []] if env['HTTP_IF_MODIFIED_SINCE'] == last_modified

      headers = { "Last-Modified" => last_modified }
      mime = Mime.mime_type(F.extname(@path), @default_mime)
      headers["Content-Type"] = mime if mime

      # Set custom headers
      @headers.each { |field, content| headers[field] = content } if @headers

      response = [ 200, headers, env["REQUEST_METHOD"] == "HEAD" ? [] : self ]

      # NOTE:
      #   We check via File::size? whether this file provides size info
      #   via stat (e.g. /proc files often don't), otherwise we have to
      #   figure it out by reading the whole file into memory.
      size = F.size?(@path) || Utils.bytesize(F.read(@path))

      ranges = Rack::Utils.byte_ranges(env, size)
      if ranges.nil? || ranges.length > 1
        # No ranges, or multiple ranges (which we don't support):
        # TODO: Support multiple byte-ranges
        response[0] = 200
        @range = 0..size-1
      elsif ranges.empty?
        # Unsatisfiable. Return error, and file size:
        response = fail(416, "Byte range unsatisfiable")
        response[1]["Content-Range"] = "bytes */#{size}"
        return response
      else
        # Partial content:
        @range = ranges[0]
        response[0] = 206
        response[1]["Content-Range"] = "bytes #{@range.begin}-#{@range.end}/#{size}"
        size = @range.end - @range.begin + 1
      end

      response[1]["Content-Length"] = size.to_s
      response
    end

    def each
      F.open(@path, "rb") do |file|
        file.seek(@range.begin)
        remaining_len = @range.end-@range.begin+1
        while remaining_len > 0
          part = file.read([8192, remaining_len].min)
          break unless part
          remaining_len -= part.length

          yield part
        end
      end
    end

    private

    def fail(status, body)
      body += "\n"
      [
        status,
        {
          "Content-Type" => "text/plain",
          "Content-Length" => body.size.to_s,
          "X-Cascade" => "pass"
        },
        [body]
      ]
    end

  end
end
