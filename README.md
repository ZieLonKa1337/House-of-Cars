# House of Cars

Digitalisiertes Parkhaus als Praktikumsprojekt der
Informationssicherheit an der H-BRS, Wintersemester 2017.

- [Digitalisierungskonzept](doc/Digitalisierungskonzept.md)
- [Lastenheft](doc/Lastenheft.md)
- [User Stories](doc/User%20Stories.md)
- [Stakeholder-Analyse](doc/Stakeholder-Analyse.txt)
- [Verzeichnis der eingesetzten Patterns](doc/Patterns.md)
- [Summarisches Projektprotokoll](doc/Protokoll.pdf)
- [Resumée und Fazit](doc/Fazit.md)
- Präsentation

## Setup

### Configuration

Not all options have defaults, so you have to configure
`house-of-cars.json` in the working directory. Here is an example with
all available options (comments are invalid syntax):

```json
{
  "port": 8080,                                              // optional
  "jdbcUrl": "jdbc:postgresql://localhost:5432/houseofcars", // optional
  "jdbcUser": "houseofcars",                                 // optional
  "jdbcPassword": "h-brs",
  "currency": {
    "name": " €",
    "scale": 2
  },
  "fee": {
    "CAR": 2,
    "BIKE": 1,
    "HANDICAP": 1.75
  },
  "limit": {
    "reminder": "PT24H",                            // ISO 8601 interval
    "overdue":  "PT72H"                             // ISO 8601 interval
  },
  "motd": "Emergency / Support: +00 123 456789"              // optional
}
```

### Database

Before running, set up a PostgreSQL cluster according to your JDBC
configuration. You can install the database schema with the
`:flywayMigrate` task. Use project properties to specify your custom
database connection if it differs from below:

    ./gradlew :flywayMigrate \
        -Pflyway.url="jdbc:postgresql://localhost:5432/houseofcars" \
        -Pflyway.user=houseofcars

Your database is now ready but there are no spots configured yet. Let's
create some:

    insert_spots () {
        # $1 first id
        # $2 last id
        # $3 type

        for i in `seq $1 $2`; do
            #    database    user
            psql houseofcars houseofcars <<EOF
    INSERT INTO Spot (id, OPTLOCK, type) VALUES ($i, 0, '$3');
    EOF
        done
    }

    insert_spots 1  75  CAR
    insert_spots 76 90  BIKE
    insert_spots 91 100 HANDICAP

Some tests also require a database to be prepared and running
according to the JDBC settings in `config-test.json`.
