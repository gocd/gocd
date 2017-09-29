# coding: utf-8

module Transpec
  module Git
    GIT = 'git'
    COMMIT_MESSAGE_FILENAME = 'COMMIT_EDITMSG'

    module_function

    def command_available?
      ENV['PATH'].split(File::PATH_SEPARATOR).any? do |path|
        git_path = File.join(path, GIT)
        File.exist?(git_path)
      end
    end

    def inside_of_repository?
      fail "`#{GIT}` command is not available" unless command_available?
      system("#{GIT} rev-parse --is-inside-work-tree > /dev/null 2> /dev/null")
    end

    def clean?
      fail_unless_inside_of_repository
      `#{GIT} status --porcelain`.empty?
    end

    def git_dir_path
      fail_unless_inside_of_repository
      `#{GIT} rev-parse --git-dir`.chomp
    end

    def write_commit_message(message)
      fail_unless_inside_of_repository
      file_path = File.expand_path(File.join(git_dir_path, COMMIT_MESSAGE_FILENAME))
      File.write(file_path, message.to_s)
      file_path
    end

    def fail_unless_inside_of_repository
      fail 'The current working directory is not a Git repository' unless inside_of_repository?
    end
  end
end
