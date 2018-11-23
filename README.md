# SX4
deamon for controlling a selectrix railroad (serial interface) via TCP/IP commands

ALPHA state, please wait for a fully implemented and tested version.

# usage:

   usage: SX4 [-b <arg>] [-d] [-h] [-s <arg>] [-t <arg>] [-v]
    -b,--baudrate <arg>   Baudrate (only needed for SLX825), default=9600
    -d,--debug            debug output on
    -h,--help             show help
    -s,--serial <arg>     Serial Device - default=ttyUSB0
    -t,--type <arg>       Interface Type (SLX825, FCC, SIM), default=SIM
    -v,--version          program version and date


# examples

# FCC an ttyUSB0, debugging on
java -Djava.library.path="/usr/lib/jni/" -jar dist/SX4.jar -s ttyUSB0 -t FCC -d

# SLX825 an ttyUSB0, debugging on
java -Djava.library.path="/usr/lib/jni/" -jar dist/SX4.jar -s ttyUSB0 -t SLX825 -b 9600 -d

# Simulation only, debugging on
java -Djava.library.path="/usr/lib/jni/" -jar dist/SX4.jar -t SIM -d




