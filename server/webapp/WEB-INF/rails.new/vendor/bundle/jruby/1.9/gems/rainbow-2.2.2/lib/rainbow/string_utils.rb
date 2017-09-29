module Rainbow
  class StringUtils

    def self.wrap_with_sgr(string, codes)
      return string if codes.empty?

      seq = "\e[" + codes.join(";") + "m"
      match = string.match(/^(\e\[([\d;]+)m)*/)
      seq_pos = match.end(0)
      string = string[0...seq_pos] + seq + string[seq_pos..-1]

      unless string =~ /\e\[0m$/
        string = string + "\e[0m"
      end

      string
    end

  end
end
