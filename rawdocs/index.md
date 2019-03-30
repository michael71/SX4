# SX4 (Core)

Das SX4 Programm wird verwendet zur Steuerung einer Selectrix 
Modellbahn über das Netzwerk (per TCP/IP Befehlen, 
siehe [Protokoll](20_protocol.md)
). Hierzu muss die 
"Command Station" per seriellem Interface mit dem PC verbunden sein (RS232 oder USB).

Das SX4 Programm kann auch als Dämon gestartet werden ohne GUI (ohne Fahrplan).

Falls ein panel.xml file im Startverzeichnis existiert 
(erzeugt durch [SX4Draw](https://michael71.github.io/SX4Draw)>SX4Draw), 
so können auch Fahrstraßen gestellt werden.
(siehe auch die LanbahnPanel und SX4Draw Dokumentation)

Falls darüberhinaus ein GUI aktiviert wird durch die "-g" Option, so kann auch
ein Fahrplan gestartet werden, der mehrere Zugfahrten nacheinander automatisch 
abarbeitet (für das GUI muss JavaFX installiert sein - dies ist in der Java-8-JRE
enthalten, muss aber bei Verwendung von Java-11 separat installiert werden).

![](fahrplan1.png)

Mehr allgemeine Info: https://opensx.net/sx4

# Startoptionen

siehe: [Startoptionen (de)](02_usage_de.md)
bzw englisch: [Usage (en)](01_usage_en.md)

# Handbuch Fahrplanbedienung

https://michael71.github.io/SX4

kommt noch ....


# SXnet

[SXnet ASCII Protocol](20_protocol.md)


# Licence

[GPL v3.0](https://www.gnu.org/licenses/gpl-3.0.en.html)

![](sx4_loco2_core.png)


