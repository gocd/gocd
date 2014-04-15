#!/usr/bin/env ruby

require 'copland'

libdir = File.join( File.dirname( __FILE__ ), "..", "lib" )
$: << libdir

registry = Copland::Registry.build libdir, :log_device => STDOUT

cli = registry.service( "solitaire.cipher.CLI" )
cli.run

registry.shutdown
