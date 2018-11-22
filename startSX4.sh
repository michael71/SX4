# FCC an USB0
java -jar dist/SX4.jar -s /dev/ttyUSB0 -t FCC -d 

# SLX825 an USB0 
# java -jar dist/SX4.jar -s /dev/ttyUSB0 -t SLX825 -b 9600 -d

# Simulation only, debugging on
java -jar dist/SX4.jar -t SIM -d
