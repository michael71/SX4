# SX4

Daemon for controlling a selectrix railroad (serial interface, RS232 or USB) via TCP/IP commands

Currently in BETA state, please wait for a fully implemented and tested version (or contact me when you want to participate in testing).

# USAGE

	usage: SX4 [-b <arg>] [-d] [-h] [-s <arg>] [-t <arg>] [-v]

	-b,--baudrate <arg>   Baudrate (only needed for SLX825), default=9600
	-d,--debug            debug output on
	-h,--help             show help
	-s,--serial <arg>     Serial Device - default=ttyUSB0
	-t,--type <arg>       Interface Type (SLX825, FCC, SIM), default=SIM
	-v,--version          program version and date


# EXAMPLES

## FCC / ttyUSB0, debugging on
	java -Djava.library.path="/usr/lib/jni/" -jar dist/SX4.jar -s ttyUSB0 -t FCC -d

## SLX825 / ttyUSB0, debugging on
	java -Djava.library.path="/usr/lib/jni/" -jar dist/SX4.jar -s ttyUSB0 -t SLX825 -b 9600 -d

## Simulation only, debugging on
	java -Djava.library.path="/usr/lib/jni/" -jar dist/SX4.jar -t SIM -d


# SXnet - ASCII tcp/ip (port 4104)

## sx channel commands

The daemon listens to ASCII commands on port 4104, currently supported

	S <c> <d>  - set an sx-channel <c> to value <d>  ("S 44 12" -> result "OK")
	SX <c> <d> = same as S <c> <d>
	R <c> - read a channel ("R 44 -> result  would be "X 44 12)

## power on/off

	SETPOWER 1  => track power on (SETPOWER 0 => track power off)
	READPOWER  -> result "XPOWER 1" or "XPOWER 0"

## single SX bit operations:

To support easy switching of turnouts (i.e. a single bit, not the complete byte of an SX channel/address), so can for example "set bit 3 of channel 85" with the command:
	
	SET 853 1 ('..0' is 'clear')

	READ 853 
	   response will be something like 'XL 853 1' (or ... 0)



## more info:

https://opensx.net/sx4

## licence

GPL v3.0





