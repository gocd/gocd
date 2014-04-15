#!/usr/bin/env ruby
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

require 'optparse'
require 'ostruct'
require 'fileutils'

module GitFlow
  extend self

  attr_accessor :should_run, :trace, :program

  self.program = 'gitflow'
  self.should_run = true # should we run at exit?

  HELP = <<-HELP

GitFlow is a tool to create custom git commands implemented in ruby.
It is generic enougth to be used on any git based project besides Apache Buildr.

OVERVIEW:

gitflow is intended to help developers with their daily git workflow,
performing repetitive git commands for them. It is implemented in
ruby so you can do anything, from invoking rake tasks to telling
people on twitter you are having trouble with their code :P.

To get help for a specific command use:
    gitflow.rb help command
    gitflow.rb command --help

For convenience you can create an alias to be execute using git.
The following example registers buildr-git.rb, which provides apache
svn and git synchronization commands:

    git config alias.apache '!'"ruby $PWD/doc/scripts/buildr-git.rb"

After that you can use
    git apache command --help

EXTENDING YOUR WORKFLOW:

You can create your own gitflow commands, to adapt your development
workflow.

Simply create a ruby script somewhere say ~/.buildr/gitflow.rb
And alias it in your local repo:

    git config alias.flow '!'"ruby ~/.buildr/gitflow.rb"
    git config alias.work '!'"ruby ~/.buildr/gitflow.rb my-flow sub-work"

A sample command would look like this.. (you may want to look at buildr-git.rb)

    #!/usr/bin/env ruby
    require /path/to/gitflow.rb

    class MyCommand < GitFlow/'my-flow'

      @help = "Summary to be displayed when listing commands"
      @documentation = "Very long help that will be paged if necessary. (for --help)"

      # takes an openstruct to place default values and option values.
      # returns an array of arguments given to optparse.on
      def options(opts)
        opts.something = 'default'
        [
         ['--name NAME', lambda { |n| opts.name = n }],
         ['--yes', lambda { |n| opts.yes = true }]
        ]
      end

      # takes the opts openstruct after options have been parsed and
      # an argv array with non-option arguments.
      def execute(opts, argv)
        # you can run another command using
        run('other-command', '--using-this', 'arg')
        some = git('config', '--get', 'some.property').chomp rescue nil
        page { puts "This will be paged on terminal if needed" }
      end

      class SubCommand < MyCommand/'sub-work'
        ... # implement a subcommand
      end

    end

You would then get help for your command with

    git flow my-flow --help
    git work --help

Using gitflow you can customize per-project git interface.

HELP

  # Pager from http://nex-3.com/posts/73-git-style-automatic-paging-in-ruby
  def pager
    return if RUBY_PLATFORM =~ /win32/
    return unless STDOUT.tty?

    read, write = IO.pipe

    unless Kernel.fork # Child process
      STDOUT.reopen(write)
      STDERR.reopen(write) if STDERR.tty?
      read.close
      write.close
      return
    end

    # Parent process, become pager
    STDIN.reopen(read)
    read.close
    write.close

    ENV['LESS'] = 'FSRX' # Don't page if the input is short enough

    Kernel.select [STDIN] # Wait until we have input before we start the pager
    pager = ENV['PAGER'] || 'less'
    exec pager rescue exec '/bin/sh', '-c', pager
  end

  # Return a class to be extended in order to register a GitFlow command
  # if command name is nil, it will be registered as the top level command.
  # Classes implementing commands also provide this method, allowing for
  # sub-command creation.
  def /(command_name)
    command_name = command_name.to_s unless command_name.nil?
    cls = Class.new { include GitFlow::Mixin }
    (class << cls; self; end).module_eval do
      attr_accessor :help, :documentation, :command
      define_method(:/) do |subcommand|
        raise "Subcommand cannot be nil" unless subcommand
        GitFlow/([command_name, subcommand].compact.join(' '))
      end
      define_method(:inherited) do |subclass|
        subclass.command = command_name
        GitFlow.commands[command_name] = subclass
      end
    end
    cls
  end

  def commands
    @commands ||= Hash.new
  end

  def optparse
    optparse = opt = OptionParser.new
    opt.separator ' '
    opt.separator 'OPTIONS'
    opt.separator ' '
    opt.on('-h', '--help', 'Display this help') do
      GitFlow.pager; puts opt; throw :exit
    end
    opt.on('--trace', 'Display traces') { GitFlow.trace = true }
    optparse
  end

  def command(argv)
    cmds = []
    argv.each_with_index do |arg, i|
      arg = argv[0..i].join(' ')
      cmds << commands[arg] if commands.key?(arg)
    end
    cmds.last || commands[nil]
  end

  def run(*argv)
    catch :exit do
      command = self.command(argv).new
      argv = argv[command.class.command.split.length..-1] if command.class.command
      parser = optparse
      parser.banner = "Usage: #{GitFlow.program} #{command.class.command} [options]"
      options = OpenStruct.new
      if command.respond_to?(:options)
        command.options(options).each { |args| parser.on(*args) }
      end
      if command.class.documentation && command.class.documentation != ''
        parser.separator ' '
        parser.separator command.class.documentation.split(/\n/)
      end
      parser.parse!(argv)
      command.execute(options, argv)
    end
  end

  module Mixin
    include FileUtils

    # Override this method in your command class if it
    # needs to parse command line options.
    #
    # This method takes an openstruct object as argument
    # allowing you to store default values on it, and
    # set option values.
    #
    # The return value must be an array of arguments
    # given to optparse.on
    def options(opt)
      []
    end

    # Override this method in your command class to implement
    # the command.
    # First argument is the openstruct object after
    # it has been populated by the option parser.
    # Second argument is the array of non-option arguments.
    def execute(opt, argv)
      fail "#{self.class.command} not implemented"
    end

    # Run the command line given on argv
    def run(*argv, &block)
      GitFlow.run(*argv, &block)
    end

    # Yield paging the blocks output if necessary.
    def page
      GitFlow.pager
      yield
    end

    def trace(*str)
      STDERR.puts(*str) if GitFlow.trace
    end

    def git(*args)
      cmd = 'git ' + args.map { |arg| arg[' '] ? %Q{"#{arg}"} : arg }.join(' ')
      trace cmd
      `#{cmd}`.tap {
        fail "GIT command `#{cmd}` failed with status #{$?.exitstatus}" unless $?.exitstatus == 0
      }
    end

    def sh(*args)
      `#{args.join(' ')}`.tap {
        fail "Shell command `#{args.join(' ')}` failed with status #{$?.exitstatus}" unless $?.exitstatus == 0
      }
    end

    def expand_path(path, dir=Dir.pwd)
      File.expand_path(path, dir)
    end
  end

  class NoSuchCommand < GitFlow/nil
    @documentation = HELP

    def execute(opts, argv)
      page do
        puts "Command not found: #{argv.join(' ').inspect}"
        puts "Try `#{GitFlow.program} help` to obtain a list of commands."
      end
    end
  end

  class HelpCommand < GitFlow/:help
    @help = "Display help for a command or show command list"
    @documentation = "Displays help for the command given as argument"

    def execute(opts, argv)
      if argv.empty?
        opt = GitFlow.optparse
        opt.banner = "Usage: #{GitFlow.program} command [options]"
        opt.separator ' '
        opt.separator 'COMMANDS'
        opt.separator ' '
        commands = GitFlow.commands.map { |name, cls| [nil, name, cls.help] }.
          sort_by { |a| a[1] || '' }
        commands.each { |a| opt.separator("%-2s%-25s%s" % a) if a[1] }
        opt.separator ' '
        opt.separator 'You can also obtain help for any command giving it --help.'
        page { puts opt }
      else
        run(*(argv + ['--help']))
      end
    end
  end

end

at_exit { GitFlow.run(*ARGV) if GitFlow.should_run }
