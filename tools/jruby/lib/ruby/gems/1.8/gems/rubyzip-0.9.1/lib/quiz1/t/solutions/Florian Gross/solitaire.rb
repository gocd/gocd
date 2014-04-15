class Array
  # Moves the item from a specified index to
  # just before the item with the specified index.
  def move(from_index, to_index)
    from_index += self.size if from_index < 0
    to_index += self.size if to_index < 0

    item = self.slice!(from_index)
    self.insert(to_index, item)
  end
end

module Solitaire
  extend self

  Letters = ('A' .. 'Z').to_a

  class Card < Struct.new(:face, :type)
    Faces = [:ace, :two, :three, :four, :five, :six, :seven,
             :eight, :nine, :ten, :jack, :queen, :king]
    Types = [:clubs, :diamonds, :hearts, :spades, :special]
    SpecialFaces = [:joker_a, :joker_b]

    def self.deck
      Types.map do |type|
        if type == :special
          SpecialFaces.map do |face|
            new(face, type)
          end
        else
          Faces.map do |face|
            new(face, type)
          end
        end
      end.flatten
    end

    def special?; type == :special; end

    def value
      if special? then 53
      else
        Faces.index(face) + 1 + 13 * Types.index(type)
      end
    end

    def letter
      Letters[(value - 1) % 26]
    end

    def name
      if face == :joker_a then "JokerA"
      elsif face == :joker_b then "JokerB"
      else
        face_str = face.to_s.capitalize.gsub(/_(\w)/) { $1.upcase }
        type_str = type.to_s.capitalize
        face_str + " of " + type_str
      end
    end

    def compact_inspect
      if face == :joker_a then "A"
      elsif face == :joker_b then "B"
      else value end
    end

    def inspect
      "#<#{self.class} #{name} (#{letter}/#{value})>"
    end
    alias :to_s :inspect

    deck.each do |card|
      const_set(card.name.sub(" of ", "Of"), card)
    end
  end

  class KeyStream
    def initialize(key_method = nil)
      case key_method
        when true then
          @deck = Card.deck.sort_by { rand }
        when String then
          @deck = Card.deck
          generate_letter(key_method)
        else
          @deck = Card.deck
      end
    end

    def generate_letter(seed_phrase = nil)
      if seed_phrase
        seed_phrase = Solitaire.clean(seed_phrase)
        seed_phrase = nil if seed_phrase.empty?
      end

      result = nil

      until result
        deck_size = @deck.size

        # Move JokerA down one card
        old_a_pos = @deck.index(Card::JokerA)
        new_a_pos = case old_a_pos
          when deck_size - 1 then 1
          else old_a_pos + 1
        end
        @deck.move(old_a_pos, new_a_pos)

        # Move JokerB down two cards
        old_b_pos = @deck.index(Card::JokerB)
        new_b_pos = case old_b_pos
          when deck_size - 1 then 2
          when deck_size - 2 then 1
          else old_b_pos + 2
        end
        @deck.move(old_b_pos, new_b_pos)

        # Perform triple cut
        top_pos, bot_pos = [@deck.index(Card::JokerA), @deck.index(Card::JokerB)].sort
        @deck.replace(
          @deck[(bot_pos + 1) .. -1] +
          @deck[top_pos .. bot_pos] + 
          @deck[0 ... top_pos])

        # Perform count cut
        top = @deck.slice!(0 ... @deck.last.value)
        @deck.insert(-2, *top)

        if seed_phrase
          key = seed_phrase.slice!(0, 1)
          top = @deck.slice!(0 ... Solitaire.letter_to_number(key))
          @deck.insert(-2, *top)
          result = true if seed_phrase.empty?
        else
          # Fetch result
          card = @deck[@deck.first.value]
          result = card.letter unless card.special?
        end
      end

      return result
    end
    alias :shift :generate_letter
  end

  def letter_to_number(letter)
    Letters.index(letter) + 1
  end

  def number_to_letter(number)
    Letters[number - 1]
  end

  def clean(text)
    text.upcase.delete("^A-Z")
  end

  def pretty(text)
    clean(text).scan(/.{1,5}/).join(" ")
  end

  def encrypt(raw_text, keystream = nil, pretty = true)
    keystream ||= KeyStream.new
    text = clean(raw_text)
    text += "X" * ((text.size / 5.0).ceil * 5 - text.size)

    result = ""
    0.upto(text.size - 1) do |index|
      source_num = letter_to_number(text[index, 1])
      key_num = letter_to_number(keystream.shift)
      result << number_to_letter((source_num + key_num) % 26)
    end

    result = pretty(result) if pretty
    return result
  end

  def decrypt(raw_text, keystream = nil, pretty = true)
    keystream ||= KeyStream.new
    text = clean(raw_text)

    result = ""
    0.upto(text.size - 1) do |index|
      source_num = letter_to_number(text[index, 1])
      key_num = letter_to_number(keystream.shift)
      result << number_to_letter((source_num - key_num) % 26)
    end

    result = pretty(result) if pretty
    return result
  end
end

if __FILE__ == $0
  require 'optparse'

  options = {
    :mode => nil,
    :keystream => nil,
    :keylength => 80,
    :text => nil
  }

  ARGV.options do |opts|
    script_name = File.basename($0)
    opts.banner = "Usage: ruby #{script_name} [options]"

    opts.separator ""

    opts.on("-d", "--decrypt",
      "Decrypt an encrypted message.",
      "This is the default if the message looks encrypted.") do
      options[:mode] = :decrypt
    end
    opts.on("-e", "--encrypt",
      "Encrypt an unencrypted message.") do
      options[:mode] = :encrypt
    end
    opts.on("-m", "--message message",
      "Specify the message.",
      "Default: Read from terminal.") do |text|
      options[:text] = text
    end
    opts.on("-k", "--key=key",
      "Specify the key that will be used for shuffling the deck.",
      "Default: Use an unshuffled deck.") do |key|
      options[:keystream] = Solitaire::KeyStream.new(key)
    end
    opts.on("-R", "--random-key length", Integer,
      "Use a randomly generated key for shuffling the deck.",
      "The key length can be specified. It defaults to 80.",
      "The key will be printed to the first line of STDOUT.") do |width|
      options[:keylength] = width if width
      options[:keystream] = :random
    end
    opts.on("-W", "--word-key file",
      "Use a randomly generated key phrase.",
      "It will consist of random words in the specified file.",
      "The key length can be specified via the -R option.",
      "The key phrase and the key will be printed to STDOUT.") do |word_file|
      options[:keystream] = :random_words
      options[:word_file] = word_file
    end   

    opts.separator ""

    opts.on("-h", "--help",
      "Show this help message.") do
      puts opts; exit
    end

    opts.parse!
  end

  input = options[:text] || STDIN.read

  options[:mode] = :decrypt if /\A(?:[A-Z]{5}\s*)+\Z/.match(input)

  case options[:keystream]
    when :random then
      key = Array.new(options[:keylength]) { Solitaire::Letters[rand(26)] }.join

      puts "Key: " + Solitaire.pretty(key)
      options[:keystream] = Solitaire::KeyStream.new(key)
    when :random_words then
      begin
        words = File.read(options[:word_file]).scan(/\w+/)
      rescue
        STDERR.puts "Word file doesn't exist or can't be read."
        exit -1
      end

      words_size = words.size

      min_words = options[:keylength] / 6
      if words_size < min_words
        STDERR.puts "Word file must contain at least #{min_words} words," +
          " but it contains only #{words_size} words!"
        exit -2
      end

      key = []
      until key.join("").length >= options[:keylength]
        key << words[rand(words_size)]
      end
      key = key.join(" ")

      puts "Keyphrase: " + key
      puts "Key: " + Solitaire.pretty(key)
      options[:keystream] = Solitaire::KeyStream.new(key)
  end

  if options[:mode] == :decrypt
    puts Solitaire.decrypt(input, options[:keystream])
  else
    unless options[:keystream]
      STDERR.puts "WARNING: Using an unshuffled deck for encrypting!"
    end
    puts Solitaire.encrypt(input, options[:keystream])
  end
end
