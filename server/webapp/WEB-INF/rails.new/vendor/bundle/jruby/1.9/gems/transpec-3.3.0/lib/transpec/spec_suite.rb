# coding: utf-8

require 'transpec/processed_source'
require 'transpec/spec_file_finder'
require 'transpec/syntax'

Transpec::Syntax.require_all

module Transpec
  class SpecSuite
    include Syntax::Dispatcher

    attr_reader :project, :target_paths, :runtime_data

    def initialize(project = Project.new, target_paths = [], runtime_data = nil)
      @project = project
      @target_paths = target_paths
      @runtime_data = runtime_data
      @analyzed = false
    end

    def specs
      @specs ||= Dir.chdir(project.path) do
        SpecFileFinder.find(target_paths).map do |path|
          ProcessedSource.from_file(path)
        end
      end
    end

    def analyze
      return if @analyzed

      specs.each do |spec|
        next unless spec.ast
        spec.ast.each_node do |node|
          dispatch_node(node, runtime_data, project)
        end
      end

      @analyzed = true
    end

    def need_to_modify_yield_receiver_to_any_instance_implementation_blocks_config?
      analyze
      @need_to_modify_yield_receiver_to_any_instance_implementation_blocks_config
    end

    def main_rspec_configure_node?(node)
      analyze

      if @main_rspec_configure
        @main_rspec_configure.node.equal?(node)
      else
        true
      end
    end

    private

    def process_any_instance_block(syntax)
      @need_to_modify_yield_receiver_to_any_instance_implementation_blocks_config ||=
        syntax.need_to_add_receiver_arg_to_any_instance_implementation_block?
    end

    def process_rspec_configure(rspec_configure)
      return unless runtime_data
      run_order = runtime_data[rspec_configure.node, :run_order]
      return unless run_order

      unless @main_rspec_configure
        @main_rspec_configure = rspec_configure
        return
      end

      if run_order < runtime_data[@main_rspec_configure.node, :run_order]
        @main_rspec_configure = rspec_configure
      end
    end
  end
end
