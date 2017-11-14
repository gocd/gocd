require "tempfile"
require "fileutils"

module Rack
  module Test

    # Wraps a Tempfile with a content type. Including one or more UploadedFile's
    # in the params causes Rack::Test to build and issue a multipart request.
    #
    # Example:
    #   post "/photos", "file" => Rack::Test::UploadedFile.new("me.jpg", "image/jpeg")
    class UploadedFile

      # The filename, *not* including the path, of the "uploaded" file
      attr_reader :original_filename

      # The tempfile
      attr_reader :tempfile

      # The content type of the "uploaded" file
      attr_accessor :content_type

      def initialize(path, content_type = "text/plain", binary = false)
        raise "#{path} file does not exist" unless ::File.exist?(path)

        @content_type = content_type
        @original_filename = ::File.basename(path)

        @tempfile = Tempfile.new([@original_filename, ::File.extname(path)])
        @tempfile.set_encoding(Encoding::BINARY) if @tempfile.respond_to?(:set_encoding)
        @tempfile.binmode if binary

        ObjectSpace.define_finalizer(self, self.class.finalize(@tempfile))

        FileUtils.copy_file(path, @tempfile.path)
      end

      def path
        @tempfile.path
      end

      alias_method :local_path, :path

      def method_missing(method_name, *args, &block) #:nodoc:
        @tempfile.__send__(method_name, *args, &block)
      end

      def respond_to?(method_name, include_private = false) #:nodoc:
        @tempfile.respond_to?(method_name, include_private) || super
      end

      def self.finalize(file)
        proc { actually_finalize file }
      end

      def self.actually_finalize(file)
        file.close
        file.unlink
      end

    end

  end
end
