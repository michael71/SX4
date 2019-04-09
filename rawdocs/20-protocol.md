# SXnet Protocol

## Port

Es wird Port 4104 verwendet (und TCP/IP) - die Befehle werden als ASCII Texte übertragen

# Befehlsübersicht

## reine SX Befehle

Über folgende ASCII Befehle werden SX-Bytes gesetzt und gelesen, d.h. Selectrix "Kanal-Daten" geschrieben und gelesen (Kanal = eine SX-Adresse zwischen 0 und 111)

    S  <c> <d>  heisst: eine Adresse <c> auf einen Wert <d>  setzen
        Beispiel: "S 44 12" schicken an SX4, dann antwortet SX4 mit "OK" (oder "ERROR", wenn der Befehl nicht verstanden wird)

    SX <c> <d>  heisst exakt dasselbe wie    S <c> <d>

    R  <c>      heisst: eine Adresse lesen (read)
        Beispie: "R 44" schicken an SX4, dann antwortet in diesem Fall SX4 mit "X 44 12"

    hierbei kann c die Werte von 1 bis 111 annehmen und d die Werte 0 bis 255 (8bit)

## erweiterte SX Bit Befehle

Um unabhängig von der anderen Bits eines SX Kanals zB eine einzelne Weiche stellen 
zu können, gibt es die "Bit-Befehle" SET und GET, Beispiel:

    SET 853 1 => Setze Bit 3 auf Adresse 85
    SET 853 0 => Clear Bit 3 auf Adresse 85
        Achtung: Bit sind hier die typischen SX-Bits, die von 1..8 numeriert sind

    READ 853  => gibt den Wert von Bit 3 auf Adresse 85 aus
       Antwort von SX4 in diesem Fall (s.o.):  "XL 853 1" (or ... 0)  


## Gleisspannungs Befehle Ein/AUS

    SETPOWER 1  => track power on (SETPOWER 0 => track power off)
    READPOWER   => lese den aktuellen "Gleisspannungszustand"
                      Antwort:  "XPOWER 1" or "XPOWER 0"

## Fahrstraßen Befehle (Route)

Grundsätzlich funktionieren Fahrstraßen-Befehle nur, wenn beim Start in der Kommandozeile die Option "-r" aktiviert wurde - und wenn ein "panel<xyz>.xml" File im Startverzeichnis des Programms liegt, in dem die Fahrstraßen definiert sind. <id> ist hierbei eine Zahl zwischen 1200 und 9999, die im XML File als "Route"-Adresse definiert wurde.
    
    Beispiel: <route adr="2203" .....   />

Die Befehle im Einzelnen:

    REQ <id> 1  => setze Fahrstraße mit der Adresse <id> 

    REQ <id> 0  => lösche Fahrstraße mit der Adresse <id>

    Die Antwort von der SX4 Zentrale ist "XL <id> 1", wenn das Setzen der Fahrstraße möglich ist

    Die Antworten "ERROR", "ROUTE_LOCKED", "ROUTE_INVALID" bedeuten, dass die Fahrstraße NICHT gesetzt werden konnte

    READ <id>   => gibt den aktuellen Zustand der Fahrstraße aus "XL <id> 1" heisst zb: Fahrstraße mit der Adresse <id> ist aktuell aktiviert


zurück zum [Index](index.md)

# Licence

[GPL v3.0](https://www.gnu.org/licenses/gpl-3.0.en.html)

![](sx4_loco2_core.png)


