# SX4 - ASCII Protocol

## SXnet - Port

Es wird Port 4104 verwendet (und TCP/IP)

# Befehlsübersicht

## reine SX Befehle

Über folgende ASCII Befehle werden SX-Bytes gesetzt und gelesen

    S <c> <d>  - set an sx-channel <c> to value <d>  ("S 44 12" -> result "OK")
    SX <c> <d> = same as S <c> <d>
    R <c> - read a channel ("R 44 -> result  would be "X 44 12)

    hierbei kann c die Werte von 1 bis 111 annehmen und d die Werte 0 bis 255 (8bit)

## erweiterte, SX Bit Befehle

Um unabhängig von der anderen Bits eines SX Kanals zB eine einzelne Weiche stellen 
zu können, gibt es die "Bit-Befehle" SET und GET, Beispiel:

    SET 853 1 => Setze Bit 3 auf Adresse 85
    SET 853 0 => Clear Bit 3 auf Adresse 85
    (Bit sind hier die typischen SX-Bits, die von 1..8 numeriert sind)

    READ 853  => gibt den Wert von Bit 3 auf Adresse 85 aus
       response will be something like 'XL 853 1' (or ... 0)


## Gleisspannungs Befehle Ein/AUS

    SETPOWER 1  => track power on (SETPOWER 0 => track power off)
    READPOWER   => lese den aktuellen "Gleisspannungszustand"
                      Antwort:  "XPOWER 1" or "XPOWER 0"


zurück zum [Index](index.md)

# Licence

[GPL v3.0](https://www.gnu.org/licenses/gpl-3.0.en.html)

![](sx4_loco2_core.png)


