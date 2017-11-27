create sequence hibernate_sequence start 1 increment 1;

create table LicensePlate (
	code varchar(255) not null,
	OPTLOCK int8 not null,
	primary key (code)
);

create table Parking (
	created timestamptz not null,
	OPTLOCK int8 not null,
	finished timestamptz,
	started timestamptz,
	licensePlate_code varchar(255) not null,
	spot_id int4,
	primary key (created)
);

create table Spot (
	id int4 not null,
	OPTLOCK int8 not null,
	type int4 not null,
	primary key (id)
);

alter table Parking
	add constraint UK_kawfsygge06ewo6sefwej2e3w unique (licensePlate_code);

alter table Parking
	add constraint FK3hdgwo6uqbdkbf38bor3ifmc3 foreign key (licensePlate_code)references LicensePlate;

alter table Parking
	add constraint FKjouk3ps197dyfof70mrc2vbk4 foreign key (spot_id)references Spot;
