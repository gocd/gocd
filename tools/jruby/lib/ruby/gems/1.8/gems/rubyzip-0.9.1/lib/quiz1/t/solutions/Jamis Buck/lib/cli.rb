class CLI

  ENCRYPTED = /^([A-Z]{5} )*[A-Z]{5}$/

  attr_writer :cipher
  attr_writer :options

  def run
    return unless @options.run_app?

    @cipher.use_algorithm @options.keying_algorithm

    @options.strings.each do |arg|
      if arg =~ ENCRYPTED
        puts arg.inspect
        puts "  (decrypt)--> #{@cipher.decrypt(arg).inspect}"
      else
        puts arg.inspect
        puts "  (encrypt)--> #{@cipher.encrypt(arg).inspect}"
      end
    end
  end

end
