# FCC an USB0, debugging on
java -Djava.library.path="/usr/lib/jni/" -jar dist/SX4.jar -s /dev/ttyUSB0 -t FCC -d

# SLX825 an USB0, debugging on
# java -Djava.library.path="/usr/lib/jni/" -jar dist/SX4.jar -s /dev/ttyUSB0 -t SLX825 -b 9600 -d

# Simulation only, debugging on
##java -Djava.library.path="/usr/lib/jni/" -jar dist/SX4.jar -t SIM -d
