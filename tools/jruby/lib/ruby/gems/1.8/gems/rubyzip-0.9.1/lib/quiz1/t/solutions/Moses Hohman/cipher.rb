#!/usr/bin/env ruby
require 'util'
require 'deck'

module Solitaire
	ASCII_OFFSET = 64
	ALPHABET_SIZE = 26
		
	class Chunker
		CHUNK_SIZE = 5

		def initialize(text)
			@legal_chars_only = text.gsub(/[^A-Za-z]/, "").upcase
			@legal_chars_only <<= "X" * (-@legal_chars_only.size % CHUNK_SIZE)
			raise "Nothing to chunk (non-alphabet characters removed): #{text}" if @legal_chars_only.size==0
			@chunks = []
			@number_chunks = []
		end

		def chunks
			@chunks if @chunks.size > 0
			@chunks = @legal_chars_only.gsub(/(.{#{CHUNK_SIZE}})/, '\1 ').rstrip.split(" ")
		end

		def number_chunks
			@number_chunks if @number_chunks.size > 0
			chunks.collect { |chunk| chunk.split("").collect { |char_string| char_string[0]-ASCII_OFFSET } }
		end

		def Chunker.to_letters(number_chunks)
			number_chunks.collect { |chunk| chunk.collect { |num| (num+ASCII_OFFSET).chr }.join }
		end
	end

	class Keystream
		A_JOKER = Card.joker(?A)
		B_JOKER = Card.joker(?B)

		def initialize(deck=Deck.new)
			@deck = deck
		end

		def keystream_letters(chunks)
			chunks.collect { |chunk| (1..chunk.size).collect { next_keystream_letter }.join }
		end

		def Keystream.card_to_letter(card)
			return "" if card.is_joker?
			(card.value.offset_mod(ALPHABET_SIZE)+ASCII_OFFSET).chr
		end

		private

		def next_keystream_letter
			process_deck
			top_card = @deck[0]
			keystream_card = @deck[top_card.value]
			letter = Keystream.card_to_letter(keystream_card)
			letter = next_keystream_letter if letter==""
			letter
		end

		def process_deck
			@deck.move_card!(A_JOKER, +1)
			@deck.move_card!(B_JOKER, +2)
			@deck.triple_cut!([A_JOKER, B_JOKER])
			@deck.count_cut!
		end
	end

	class Cipher
		ENCRYPTED_TEXT_PATTERN = /\A[A-Z]{5}( [A-Z]{5})*\Z/

		def initialize(text, deck=Deck.new)
			@chunker = Chunker.new(text)
			keystream = Keystream.new(deck)
			@keystream_chunker = Chunker.new(keystream.keystream_letters(@chunker.chunks).join)
			if text =~ ENCRYPTED_TEXT_PATTERN
				@mode = "decrypt"
				@calc_number = proc { |num, keystream_num| num - keystream_num }
			else
				@mode = "encrypt"
				@calc_number = proc { |num, keystream_num| num + keystream_num }
			end
		end
		attr_reader :mode

		def crypt
			ciphered = [@chunker.number_chunks, @keystream_chunker.number_chunks].collect_peel do |num_chunk, keystream_num_chunk|
				[num_chunk, keystream_num_chunk].collect_peel do |num, keystream_num|
					@calc_number.call(num,keystream_num).offset_mod(ALPHABET_SIZE)
				end
			end
			Chunker.to_letters(ciphered).join(" ").rstrip
		end
	end
end
