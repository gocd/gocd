# -*- ruby encoding: utf-8 -*-

$LOAD_PATH.unshift File.expand_path('../../lib', __FILE__)
require 'mime/types'
require 'fileutils'

class Convert
  class << self
    def from_yaml(path = nil)
      new(path: path, from: :yaml)
    end

    def from_json(path = nil)
      new(path: path, from: :json)
    end

    def from_v1(path = nil)
      new(path: path, from: :v1)
    end

    def from_yaml_to_json(args)
      from_yaml(yaml_path(args.source)).
        to_json(destination:    json_path(args.destination),
                multiple_files: multiple_files(args.multiple_files))
    end

    def from_json_to_yaml(args)
      from_json(json_path(args.source)).
        to_yaml(destination:    yaml_path(args.destination),
                multiple_files: multiple_files(args.multiple_files))
    end

    private :new

    private
    def yaml_path(path)
      if path.nil? or path.empty?
        'type-lists'
      else
        path
      end
    end

    def json_path(path)
      if path.nil? or path.empty?
        'data'
      else
        path
      end
    end

    def multiple_files(flag)
      case flag
      when "true", "yes"
        true
      else
        false
      end
    end
  end

  def initialize(options = {})
    if options[:path].nil? or options[:path].empty?
      raise ArgumentError, ':path is required'
    end
    if options[:from].nil? or options[:from].empty?
      raise ArgumentError, ':from is required'
    end

    path = options[:path]

    @loader = MIME::Types::Loader.new(options[:path])
    load_from(options[:from])
  end

  def to_json(options = {})
    raise ArgumentError, 'destination is required' unless options[:destination]
    write_types(options.merge(format: :json))
  end

  def to_yaml(options = {})
    raise ArgumentError, 'destination is required' unless options[:destination]
    write_types(options.merge(format: :yaml))
  end

  private
  def load_from(source_type)
    method = :"load_#{source_type}"
    @loader.send(method)
  end

  def write_types(options)
    if options[:multiple_files]
      write_multiple_files(options)
    else
      write_one_file(options)
    end
  end

  def write_one_file(options)
    d = options[:destination]
    d = File.join(d, "mime-types.#{options[:format]}") if File.directory?(d)

    File.open(d, 'wb') { |f|
      f.puts convert(@loader.container.map.sort, options[:format])
    }
  end

  def write_multiple_files(options)
    d = options[:destination]
    if File.exist?(d) and not File.directory?(d)
      raise ArgumentError, 'Cannot write multiple files to a file.'
    end

    FileUtils.mkdir_p d unless File.exist?(d)

    media_types = MIME::Types.map(&:media_type).uniq
    media_types.each { |media_type|
      n = File.join(d, "#{media_type}.#{options[:format]}")
      t = @loader.container.select { |e| e.media_type == media_type }
      File.open(n, 'wb') { |f|
        f.puts convert(t.sort, options[:format])
      }
    }
  end

  def convert(data, format)
    data.send(:"to_#{format}")
  end
end
