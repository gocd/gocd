# coding: utf-8

require 'rainbow'

module Transpec
  class Report
    attr_reader :records, :conversion_errors, :file_errors

    def initialize
      @records = []
      @conversion_errors = []
      @file_errors = []
    end

    def <<(other)
      records.concat(other.records)
      conversion_errors.concat(other.conversion_errors)
      file_errors.concat(other.file_errors)
      self
    end

    def unique_record_counts
      record_counts = Hash.new(0)

      records.each do |record|
        record_counts[record] += 1
      end

      sorted_record_counts = record_counts.sort_by do |record, count|
        [-count, record]
      end

      Hash[sorted_record_counts]
    end

    def colored_summary(options = nil)
      options ||= { bullet: nil, separate_by_blank_line: false }

      summary = ''

      unique_record_counts.each do |record, count|
        summary << "\n" if options[:separate_by_blank_line] && !summary.empty?
        summary << format_record(record, count, options[:bullet])
      end

      summary
    end

    def summary(options = nil)
      without_color { colored_summary(options) }
    end

    def colored_stats
      base_color = if !conversion_errors.empty?
                     :magenta
                   elsif annotation_count.nonzero?
                     :yellow
                   else
                     :green
                   end

      conversion_incomplete_warning_stats(base_color) + error_stats(base_color)
    end

    def stats
      without_color { colored_stats }
    end

    private

    def rainbow
      @rainbow ||= Rainbow.new
    end

    def colorize(string, *args)
      rainbow.wrap(string).color(*args)
    end

    def without_color
      original_coloring_state = rainbow.enabled
      rainbow.enabled = false
      value = yield
      rainbow.enabled = original_coloring_state
      value
    end

    def format_record(record, count, bullet = nil)
      entry_prefix = bullet ? "#{bullet} " : ''
      text = entry_prefix + colorize(pluralize(count, record.type), :cyan) + "\n"

      justification = entry_prefix.length + 6

      case record.type
      when :conversion
        text << labeled_line('from', record.old_syntax, justification)
        text << labeled_line('to',   record.new_syntax, justification)
      when :addition
        text << labeled_line('of',   record.new_syntax, justification)
      when :removal
        text << labeled_line('of',   record.old_syntax, justification)
      end
    end

    def labeled_line(label, content, justification)
      colorize("#{label.rjust(justification)}: ", :cyan) + content + "\n"
    end

    def conversion_incomplete_warning_stats(color)
      text = pluralize(records.count, 'conversion') + ', '
      text << pluralize(conversion_errors.count, 'incomplete') + ', '
      text << pluralize(annotation_count, 'warning') + ', '
      colorize(text, color)
    end

    def error_stats(base_color)
      color = if file_errors.empty?
                base_color
              else
                :red
              end

      colorize(pluralize(file_errors.count, 'error'), color)
    end

    def pluralize(number, thing, options = {})
      text = ''

      if number.zero? && options[:no_for_zero]
        text = 'no'
      else
        text << number.to_s
      end

      text << " #{thing}"
      text << 's' unless number == 1

      text
    end

    def annotation_count
      records.count(&:annotation)
    end
  end
end
