class Numeric
	def value
		self
	end
	
	def to_letter
		((self-1)%26 + ?A).chr
	end
end

class String
	# returns an array with the code of the letters,
	# padded with the code of X
	def to_numbers
		res=upcase.unpack("C*").collect { |b|
			if b.between? ?A, ?Z
				b - ?A + 1
			else
				nil
			end
		}.compact
		# 24 == X
		res.fill 24, res.length, (5 - res.length % 5) % 5
	end
	
	def crypt (deck, decrypt=false)
		numbers = to_numbers
		keystream = deck.generate_keystream numbers.length
		result = ""
		numbers.zip(keystream) do |n, k|
			k = -k if decrypt
			result << (n+k).to_letter
		end
		result
	end
	
	def encrypt (deck)
		crypt deck, false
	end
	
	def decrypt (deck)
		crypt deck, true
	end
end

class Joker
	def value
		53
	end
end

A = Joker.new
B = Joker.new

class Array
	def wrap_down pos
		pos %= length
		if pos == 0
			pos = length
		end
		pos
	end

	def next_key
		# step 2: move A joker down 1 card
		pos = index A
		slice! pos
		pos = wrap_down(pos + 1)
		self[pos, 0] = A
		
		# step 3: move B joker down 2 cards
		pos = index B
		slice! pos
		pos = wrap_down(pos + 2)
		self[pos, 0] = B
		
		# step 4: triple cut
		first_joker, second_joker = [index(A), index(B)].sort
		cards_above = slice! 0...first_joker
		second_joker -= cards_above.length
		cards_below = slice! second_joker+1..-1
		push *cards_above
		unshift *cards_below
		
		# step 5: count cut using the value of the bottom card.
		#         reinsert above the last card
		cut = slice! 0, last.value
		self[-1,0] = cut
		
		# step 6: find the letter
		card = self[first.value]
		
		return Joker===card ? nil : card.value
	end
	
	def generate_keystream len
		(1..len).collect {|i| next_key or redo }
	end
end

def new_deck
	(1..52).to_a + [A, B]
end

res = if ARGV[0] == "-d"
		ARGV[1..-1].join("").decrypt(new_deck)
	else
		ARGV.join("").encrypt(new_deck)
	end

puts res.scan(/.{5}/).join(" ")
