# coding: utf-8

require 'fileutils'
require 'find'
require 'pathname'

module Transpec
  # This module performs almost the same thing as FileUtils#cp_r, expect ignoring pseudo files
  # (e.g.pipe, socket).
  module DirectoryCloner
    module_function

    def copy_recursively(source_root, destination_root)
      source_root = File.expand_path(source_root)
      source_root_pathname = Pathname.new(source_root)

      destination_root = File.expand_path(destination_root)
      if File.directory?(destination_root)
        destination_root = File.join(destination_root, File.basename(source_root))
      end

      Find.find(source_root) do |source_path|
        relative_path = Pathname.new(source_path).relative_path_from(source_root_pathname).to_s
        destination_path = File.join(destination_root, relative_path)
        copy(source_path, destination_path)
      end

      destination_root
    end

    def copy(source, destination)
      if File.symlink?(source)
        File.symlink(File.readlink(source), destination)
      elsif File.directory?(source)
        FileUtils.mkdir_p(destination)
      elsif File.file?(source)
        FileUtils.copy_file(source, destination)
      end

      copy_permission(source, destination) if File.exist?(destination)
    rescue => error
      raise error.class, "while copying #{source.inspect}.", error.backtrace
    end

    def copy_permission(source, destination)
      source_mode = File.lstat(source).mode
      begin
        File.lchmod(source_mode, destination)
      rescue NotImplementedError, Errno::ENOSYS
        # Should not change mode of symlink's destination.
        File.chmod(source_mode, destination) unless File.symlink?(destination)
      end
    end
  end
end
