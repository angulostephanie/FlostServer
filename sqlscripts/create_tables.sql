
create table Users (
	email varchar(30) not null,
	first_name varchar(30) not null,
	last_name varchar(30) not null,
	photo_url text,
	primary key(email)
);

create table Items (
	item_id int not null,
	email varchar(40) not null,
	item_name varchar(30) not null,
	item_desc varchar(60) not null,
	item_type enum('lost', 'found') not null,
	item_location enum('fowler', 'johnson', 'academic commons', 'berkus') not null,
	item_reward decimal(10, 2) not null,
	item_timestamp timestamp,
	primary key (item_id, email),
	foreign key (email) references Users(email)
);

-- dependent on Items
create table ClaimedItems (

);

create table Messages (

);

create table Form (

);
