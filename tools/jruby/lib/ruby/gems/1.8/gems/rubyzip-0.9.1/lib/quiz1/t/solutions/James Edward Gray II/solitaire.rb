#!/usr/bin/ruby -w

$deck = (1..52).to_a + ["A", "B"]		# Unkeyed deck - Keystream Step 1

def encrypt(message)
	# Step 1
	message = message.upcase.tr("^A-Z", "")
	i = 5
	while i < message.size
		message[i, 0] = " "
		i += 6
	end
	message += "X" while message.rindex(" ") != message.size - 6
	
	# Step 2
	key_stream = generate(message.count("^ "))
	
	# Step 3
	values = message.split("").map { |letter| letter[0] - ?A + 1 }

	# Step 4
	key_values = key_stream.split("").map { |letter| letter[0] - ?A + 1 }
	
	# Step 5
	values.each_with_index do |value, index|
		next if value < 0
		values[index] = value + key_values[index]
		values[index] -= 26 if values[index] > 26
	end
	
	# Step 6
	message = (values.map { |number| (number - 1 + ?A).chr }).join("")
	
	return message
end

def decrypt(message)
	# Step 1
	key_stream = generate(message.size)
	
	# Step 2
	values = message.split("").map { |letter| letter[0] - ?A + 1 }

	# Step 3
	key_values = key_stream.split("").map { |letter| letter[0] - ?A + 1 }
	
	# Step 4
	values.each_with_index do |value, index|
		next if value < 0
		if value <= key_values[index]
			values[index] = value + 26 - key_values[index]
		else
			values[index] = value - key_values[index]
		end
	end
	
	# Step 5
	message = (values.map { |number| (number - 1 + ?A).chr }).join("")
	
	return message
end

def generate(count)		# Keystream Steps
	key_stream = [ ]

	until key_stream.size == count
		# Step 2
		a = $deck.index("A")
		if a == 53
			$deck.insert(1, $deck.pop)
		else
			$deck.insert(a + 1, $deck.delete_at(a))
		end

		# Step 3
		b = $deck.index("B")
		if b == 53
			$deck.insert(2, $deck.pop)
		elsif b == 52
			$deck.insert(1, $deck.delete_at(b))
		else
			$deck.insert(b + 2, $deck.delete_at(b))
		end

		# Step 4
		a = $deck.index("A")
		b = $deck.index("B")
		top = [a, b].min
		bottom = [a, b].max
		$deck = $deck.values_at((bottom + 1)..53, top..bottom, 0...top)

		# Step 5
		if $deck[53].kind_of? Integer
			$deck = $deck.values_at($deck[53]..52, 0...$deck[53], 53)
		end

		# Step 5
		if $deck[0].kind_of? Integer
			if $deck[$deck[0]].kind_of? Integer
				key_stream.push($deck[$deck[0]])
			end
		else
			if $deck[53].kind_of? Integer
				key_stream.push($deck[53])
			end
		end
	end		# Step 7

	key_stream.map! do |number|
		if number > 26
			(number - 26 - 1 + ?A).chr
		else
			(number - 1 + ?A).chr
		end
	end
	key_stream = key_stream.join("")
	i = 5
	while i < key_stream.size
		key_stream[i, 0] = " "
		i += 6
	end
	return key_stream
end

# Mind reading interface
if ARGV.size == 1 and ARGV[0] =~ /^(?:[A-Z]{5} )*[A-Z]{5}$/
	puts decrypt(ARGV[0])
elsif ARGV.size == 1
	puts encrypt(ARGV[0])
else
	puts "Usage:  solitaire.rb MESSAGE"
end
