create sequence hibernate_sequence start 1 increment 1;

create table Spot (
	id int4 not null,
	OPTLOCK int8 not null,
	type int4 not null,
	primary key (id)
);

create table Vehicle (
	license varchar(255) not null,
	OPTLOCK int8 not null,
	primary key (license)
);

create table VehicleTransition (
	time timestamptz not null,
	OPTLOCK int8 not null,
	state varchar(255) not null,
	spot_id int4,
	vehicle_license varchar(255) not null,
	primary key (time)
);

alter table VehicleTransition
	add constraint FKfwx6x8epwf1v4meg57gifc5j1 foreign key (spot_id)references Spot;

alter table VehicleTransition
	add constraint FKqpjg4ijjfp76qofrp327h940c foreign key (vehicle_license)references Vehicle;

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
 ON VehicleTransition.vehicle_license = Vehicle.license
  AND time <= $1
 ORDER BY license, time DESC
$$ LANGUAGE sql
IMMUTABLE;

CREATE VIEW vehicle_state AS
 SELECT * FROM vehicle_state_at(CURRENT_TIMESTAMP);

CREATE VIEW parking AS
 SELECT
  VehicleTransition.spot_id,
  vehicle_state.vehicle_license,
  since
 FROM vehicle_state, VehicleTransition
 WHERE vehicle_state.since = VehicleTransition.time
  AND vehicle_state.state = 'Parking'
  AND vehicle_state.state = VehicleTransition.state
 ORDER BY since DESC;
