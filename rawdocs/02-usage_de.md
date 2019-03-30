# SX4 USAGE (Startoptionen)

    usage: SX4 [-b <arg>] [-d] [-h] [-s <arg>] [-t <arg>] [-v] [-r] [-g]

    -b,--baudrate <arg>   Baudrate (only needed for SLX825), default=9600
    -d,--debug            debug output on
    -h,--help             show help
    -s,--serial <arg>     Serial Device - default=ttyUSB0   (on windows use COM3 ..)
    -t,--type <arg>       Interface Type (SLX825, FCC, SIM), default=SIM
    -v,--version          program version and date
    -r,--routing          enable routing
    -g,--gui              GUI with timetables and trips (automatation)

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

# Licence

[GPL v3.0](https://www.gnu.org/licenses/gpl-3.0.en.html)

![](sx4_loco2_core.png)


