# coding: utf-8

require 'find'

module Transpec
  module SpecFileFinder
    module_function

    def find(paths)
      base_paths(paths).reduce([]) do |file_paths, path|
        if File.directory?(path)
          file_paths.concat(ruby_files_in_directory(path))
        elsif File.file?(path)
          file_paths << path
        elsif !File.exist?(path)
          fail ArgumentError, "No such file or directory #{path.inspect}"
        end
      end
    end

    def base_paths(paths)
      if paths.empty?
        if Dir.exist?('spec')
          ['spec']
        else
          fail ArgumentError, 'Specify target files or directories.'
        end
      else
        if paths.all? { |path| inside_of_current_working_directory?(path) }
          paths
        else
          fail ArgumentError, 'Target path must be inside of the current working directory.'
        end
      end
    end

    def inside_of_current_working_directory?(path)
      File.expand_path(path).start_with?(Dir.pwd)
    end

    def ruby_files_in_directory(directory_path)
      ruby_file_paths = []

      Find.find(directory_path) do |path|
        next unless File.file?(path)
        next unless File.extname(path) == '.rb'
        ruby_file_paths << path
      end

      ruby_file_paths
    end
  end
end
