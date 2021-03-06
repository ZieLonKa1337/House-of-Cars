create sequence hibernate_sequence start 1 increment 1;

create table Customer (
	id int8 not null,
	OPTLOCK int8 not null,
	pass varchar(60) not null,
	primary key (id)
);

create table Reservation (
	id int8 not null,
	OPTLOCK int8 not null,
	"end" timestamptz not null,
	spotType varchar(255),
	start timestamptz not null,
	customer_id int8 not null,
	primary key (id)
);

create table ReservationTransition (
	time timestamptz not null,
	OPTLOCK int8 not null,
	state varchar(255) not null,
	entity_id int8 not null,
	primary key (time)
);

create table Spot (
	id int4 not null,
	OPTLOCK int8 not null,
	type varchar(255) not null,
	primary key (id)
);

create table Vehicle (
	license varchar(255) not null,
	OPTLOCK int8 not null,
	owner_id int8,
	primary key (license)
);

create table VehicleTransition (
	time timestamptz not null,
	OPTLOCK int8 not null,
	fee numeric(16, 8), -- big enough for Bitcoin
	"limit" varchar(255), -- iso_8601 interval
	paid boolean,
	reminder varchar(255), -- iso_8601 interval
	state varchar(255) not null,
	recommendedSpot_id int4,
	spot_id int4,
	entity_license varchar(255) not null,
	primary key (time)
);

alter table Reservation
	add constraint FKmrfygyy9wi4be7nlyuogrn24n foreign key (customer_id)references Customer;

alter table ReservationTransition
	add constraint FK5qt0age6uv59fn156x0l0npxv foreign key (entity_id)references Reservation;

alter table Vehicle
	add constraint FKbca7rhv01903thhh98q0xa54d foreign key (owner_id)references Customer;

alter table VehicleTransition
	add constraint FKgm8tm50ergw7krq4w4dmwnfdc foreign key (recommendedSpot_id)references Spot;

alter table VehicleTransition
	add constraint FKfwx6x8epwf1v4meg57gifc5j1 foreign key (spot_id)references Spot;

alter table VehicleTransition
	add constraint FKh02j9g0mtsusi3lkej2amgu4c foreign key (entity_license)references Vehicle;

--

CREATE TYPE vehicle_state_t AS (
 vehicle_license varchar(255),
 state           varchar(255),
 since           timestamptz
);

CREATE FUNCTION vehicle_state_at(timestamptz)
RETURNS SETOF vehicle_state_t AS $$
 SELECT DISTINCT ON (license)
  license,
  CASE WHEN state IS NULL THEN 'Away'::varchar(255) ELSE state END AS state,
  time AS since
 FROM Vehicle
  LEFT JOIN VehicleTransition
   ON VehicleTransition.entity_license = Vehicle.license
    AND time <= $1
 ORDER BY license, time DESC
$$ LANGUAGE sql
IMMUTABLE;

CREATE VIEW vehicle_state AS
 SELECT * FROM vehicle_state_at(CURRENT_TIMESTAMP);

CREATE VIEW spot_state AS
 SELECT
  Spot.id AS spot_id,
  vehicle_state.vehicle_license,
  since -- TODO show freed time
 FROM vehicle_state
  LEFT JOIN VehicleTransition
   ON VehicleTransition.time = since
  RIGHT JOIN Spot
   ON Spot.id = VehicleTransition.spot_id
 ORDER BY Spot.id;
