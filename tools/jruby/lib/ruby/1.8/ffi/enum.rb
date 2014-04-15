module FFI

  class Enums

    def initialize
      @all_enums = Array.new
      @tagged_enums = Hash.new
      @symbol_map = Hash.new
    end

    def <<(enum)
      @all_enums << enum
      @tagged_enums[enum.tag] = enum unless enum.tag.nil?
      @symbol_map.merge!(enum.symbol_map)
    end

    def find(query)
      if @tagged_enums.has_key?(query)
        @tagged_enums[query]
      else
        @all_enums.detect { |enum| enum.symbols.include?(query) }
      end
    end

    def __map_symbol(symbol)
      @symbol_map[symbol]
    end

    def to_hash
      @symbol_map
    end
  end
  
  class Enum
    attr_reader :tag

    def initialize(info, tag=nil)
      @tag = tag
      @kv_map = Hash.new
      @vk_map = Hash.new
      unless info.nil?
        last_cst = nil
        value = 0
        info.each do |i|
          case i
          when Symbol
            @kv_map[i] = value
            @vk_map[value] = i
            last_cst = i
            value += 1
          when Integer
            @kv_map[last_cst] = i
            @vk_map[i] = last_cst
            value = i+1
          end
        end
      end
    end

    def symbols
      @kv_map.keys
    end

    def [](query)
      case query
      when Symbol
        @kv_map[query]
      when Integer
        @vk_map[query]
      end
    end
    alias find []

    def symbol_map
      @kv_map
    end
    alias to_h symbol_map
    alias to_hash symbol_map

  end

end