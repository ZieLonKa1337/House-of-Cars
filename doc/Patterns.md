# Patterns

Bei der Entwicklung haben einige Patterns geholfen.

## Adapter Pattern

Die JPA (Java Persistence API) stellt Lifecycle-Events zur Verfügung,
die bei `de.codazz.houseofcars.service.Monitor` ankommen sollen. Da die
JPA jeden Listener aber selbst durch Reflection instanziiert, kann keine
Instanz des Monitoring-Service als Listener registriert werden. Es
würden also zwei Instanzen erzeugt. Hier hilft der Adapter:
`VehicleTransitionListener` leitet die Events an eine klar definierte
`Monitor`-Instanz weiter.

## MVVP (Model View ViewModel)

Das [Spark]-Framework baut auf diesem Pattern auf. Zum Ausdruck kommt
das beispielsweise wo [Mustache]-Templates eingebunden werden:
`modelAndView(templateValues, "dashboard.html.mustache")`

## Singleton Pattern

Ein ähnliches Problem wie beim [Adapter Pattern](#adapter-pattern)
beschrieben gibt es bei den WebSocket-Klassen. Auch sie werden von Spark
/ Jetty durch Reflection instanziiert. Es ist zwar auch möglich eigene
Instanzen zu nutzen, die Annotation-API ist aber deutlich einfacher.
Da sowieso nur jeweils eine Instanz dieser Klassen existieren soll kommt
hier das Singleton Pattern zum Einsatz, wodurch auch der Zugriff zum
Senden von Updates vereinfacht wird.

Zudem ist `Garage` ein Singleton, um den Zugriff auf Services zu
vereinfachen.

## State Pattern

Interaktionen zwischen Akteuren im System lassen sich gut mit State
Machines darstellen. Im Gegensatz zu den meisten anderen Patterns bietet
sich hier auch eine generische Implementation an, welche im Package
`de.codazz.houseofcars.statemachine` zu finden ist. Eingesetzt wird sie
in `Vehicle`, `Reservation` und `de.codazz.houseofcars.business.Gate`.

Ein weiterer Vorteil ist, dass jeder Übergang persistiert werden kann,
wodurch er für statistische Auswertung und Zustandswiederherstellung
nach Beenden des Programms zur Verfügung steht.

## Template Pattern

`StatefulEntity` überlässt die Initialisierung und Wiederherstellung der
State Machine der konkreten Klasse in Form der abstrakten Methoden
`initLifecycle` und `restoreLifecycle`.

[Mustache]: http://mustache.github.io
[Spark]: http://sparkjava.com
