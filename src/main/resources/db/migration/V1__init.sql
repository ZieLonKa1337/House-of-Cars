create sequence hibernate_sequence start 1 increment 1;

create table Spot (
	id int4 not null,
	OPTLOCK int8 not null,
	type int4 not null,
	primary key (id)
);

