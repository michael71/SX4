# SX4 Startoptionen

Mit den Startoptionen wird der Typ der Zentrale ausgewählt (Beispiele unten) 
und eine Betriebsart gewählt, also ob
* mit oder ohne Fahrstraßen("routing")
* mit oder ohne Fahrplan-Fenster ("graphical user interface")

gearbeitet werden soll.

Im Einzelnen: (jeweils Kurzform, zum Beispiel "-b" und Langform "--baudrate")

   
    SX4 [-b <arg>] [-d] [-h] [-s <arg>] [-t <arg>] [-v] [-r] [-g]

    -b,--baudrate <arg>   Baudrate (only needed for SLX825), default=9600

    -d,--debug            debug output on

    -h,--help             show help

    -s,--serial <arg>     Serial Device - default=ttyUSB0   (on windows use COM3 ..)

    -t,--type <arg>       Interface Type (SLX825, FCC, SIM), default=SIM

    -v,--version          program version and date

    -r,--routing          enable routing

    -g,--gui              GUI with timetables and trips (automatation)


# Beispiele

## FCC / ttyUSB0, debugging on

    java -jar SX4.jar -s ttyUSB0 -t FCC -d

## SLX825 / ttyUSB0, debugging on

    java -jar SX4.jar -s ttyUSB0 -t SLX825 -b 9600 -d

## Simulation, debugging on

    java -jar SX4.jar -t SIM -d


### =>> weiter zu [Hauptfenster](03-Hauptfenster.md)

___

![](sx4_loco2_core.png)


