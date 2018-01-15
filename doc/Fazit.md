# Resumée und Fazit

Mustache hat sich als Template-Engine flexibel gezeigt. Scheinbar ist
nicht nur konventioneller Property-Zugriff unterstützt, sondern auch die
Variante ohne `get`/`is` und `set`-Präfixe (z.B. `state()` statt
`getState()`). Außerdem lassen sich Werte in Objekten mit entsprechenden
JNDI-Keys überschreiben, was aber keine Verwendung fand.

State Machines stellten sich als sehr mächtig heraus. Ein Großteil der
Funktionalität basiert darauf, Übergänge zwischen States auszuwerten und
zu vergleichen. Leider basierte die erste Implementation auf Annotations
und wurde gegen eine Leichtgewichtigere ausgetauscht, wodurch etwas Zeit
verloren ging.

In einer Weiterentwicklung ließen sich aber auch noch einige Details
verbessern.

- Anstatt JPQL (Java Persistence Query Language) in Strings zu schreiben
  sollte `CriteriaBuilder` verwendet werden, um statisch typisierte
  Queries zu ermöglichen.
- Die Verwendung von `static`s sollte reduziert werden. Der Code kann
  vereinfacht werden, wenn jegliche Funktionalität durch eine zentrierte
  Stelle erreichbar ist - bekannt als Facade Pattern. Als Ansatz sind
  alle Services von der `Garage` erreichbar, aber einige WebSocket-
  Klassen sind noch Singletons.
- Es sollte in Betracht gezogen werden, einige Services mit Hilfe des
  `java.util.ServiceLoader` zu implementieren.
- Einige Auswertungen wie z.B. die Berechnung des Preises sollten nicht
  von der Anwendung sondern der Datenbank selber durchgeführt werden.
  Das ist nicht nur performanter sondern vereinfacht auch die
  Datenverarbeitung von der Datenbank zum Frontend.
