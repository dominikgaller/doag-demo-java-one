# doag-demo-java-one

Das ist der erste Dienst für die Demo. Er enthält im wesentlichen, die Verwaltung des Loggins, das User-Management sowie ein rudimentäres Session-Handling. Dieser Dienst sollte als erstes gestartet werden.

## Starten

- Auchecken
- mvn clean install
- java -jar target/*-fat.jar ausführen

## Wichtig
Es wird Maven sowie Java 1.8 benötigt. 

## Startreihenfolge
- service-one
- service-two
- service-js
- Dann unbedingt warten, bis das Hazelcast-Cluster hergestellt werden konnte!
