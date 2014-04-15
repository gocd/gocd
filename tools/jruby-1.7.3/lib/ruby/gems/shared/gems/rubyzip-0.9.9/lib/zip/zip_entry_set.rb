module Zip
  class ZipEntrySet #:nodoc:all
    include Enumerable
    
    def initialize(anEnumerable = [])
      super()
      @entrySet = {}
      anEnumerable.each { |o| push(o) }
    end

    def include?(entry)
      @entrySet.include?(to_key(entry))
    end

    def find_entry(entry)
      @entrySet[to_key(entry)]
    end

    def <<(entry)
      @entrySet[to_key(entry)] = entry
    end
    alias :push :<<

    def size
      @entrySet.size
    end
    
    alias :length :size

    def delete(entry)
      @entrySet.delete(to_key(entry)) ? entry : nil
    end

    def each(&aProc)
      @entrySet.values.each(&aProc)
    end

    def entries
      @entrySet.values
    end

    # deep clone
    def dup
      ZipEntrySet.new(@entrySet.values.map { |e| e.dup })
    end

    def ==(other)
      return false unless other.kind_of?(ZipEntrySet)
      @entrySet == other.entrySet      
    end

    def parent(entry)
      @entrySet[to_key(entry.parent_as_string)]
    end

    def glob(pattern, flags = ::File::FNM_PATHNAME|::File::FNM_DOTMATCH)
      entries.map do |entry|
        next nil unless ::File.fnmatch(pattern, entry.name.chomp('/'), flags)
        yield(entry) if block_given?
        entry
      end.compact
    end	

#TODO    attr_accessor :auto_create_directories
    protected
    attr_accessor :entrySet

    private
    def to_key(entry)
      entry.to_s.sub(/\/$/, "")
    end
  end
end

# Copyright (C) 2002, 2003 Thomas Sondergaard
# rubyzip is free software; you can redistribute it and/or
# modify it under the terms of the ruby license.
