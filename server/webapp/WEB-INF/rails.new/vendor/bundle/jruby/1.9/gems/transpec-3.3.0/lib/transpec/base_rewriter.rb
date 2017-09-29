# coding: utf-8

require 'transpec/processed_source'

module Transpec
  class BaseRewriter
    def rewrite_file!(arg)
      processed_source = case arg
                         when String          then ProcessedSource.from_file(arg)
                         when ProcessedSource then arg
                         else fail "Invalid argument: #{arg}"
                         end

      fail 'ProcessedSource must be derived from a file' unless processed_source.path

      rewritten_source = rewrite(processed_source)
      return if processed_source.to_s == rewritten_source
      File.write(processed_source.path, rewritten_source)
    end

    def rewrite_source(source, path = nil)
      processed_source = ProcessedSource.new(source, path)
      rewrite(processed_source)
    end

    def rewrite(processed_source)
      fail processed_source.error if processed_source.error

      source_rewriter = create_source_rewriter(processed_source)
      incomplete = false

      begin
        process(processed_source.ast, source_rewriter)
      rescue OverlappedRewriteError
        incomplete = true
      end

      rewritten_source = source_rewriter.process
      rewritten_source = rewrite_source(rewritten_source, processed_source.path) if incomplete

      rewritten_source
    end

    private

    def create_source_rewriter(processed_source)
      Parser::Source::Rewriter.new(processed_source.buffer).tap do |source_rewriter|
        source_rewriter.diagnostics.consumer = proc do
          fail OverlappedRewriteError
        end
      end
    end

    def process(ast, source_rewriter) # rubocop:disable UnusedMethodArgument
      fail NotImplementedError
    end

    class OverlappedRewriteError < StandardError; end
  end
end
