create sequence hibernate_sequence start 1 increment 1;

create table Parking (
	started timestamptz not null,
	OPTLOCK int8 not null,
	finished timestamptz,
	parked timestamptz,
	spot_id int4,
	vehicle_license varchar(255) not null,
	primary key (started)
);

create table Spot (
	id int4 not null,
	OPTLOCK int8 not null,
	type int4 not null,
	primary key (id)
);

create table Vehicle (
	license varchar(255) not null,
	OPTLOCK int8 not null,
	present boolean not null,
	primary key (license)
);

alter table Parking
	add constraint FKjouk3ps197dyfof70mrc2vbk4 foreign key (spot_id)references Spot;

alter table Parking
	add constraint FKsqoybondilp8ma15vuft5e586 foreign key (vehicle_license)references Vehicle;
