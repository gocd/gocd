module Declarative
  class DeepDup
    def self.call(args)
      return Array[*dup_items(args)] if args.is_a?(Array)
      return ::Hash[dup_items(args)] if args.is_a?(::Hash)
      args
    end

  private
    def self.dup_items(arr)
      arr.to_a.collect { |v| call(v) }
    end
  end
end
