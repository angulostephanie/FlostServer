
create table Users (
	user_id varchar(40) not null,
	first_name varchar(30) not null,
	email varchar(30) not null,
	primary key(user_id) 
);

create table Items (
	item_id int not null,
	user_id varchar(40) not null,
	item_name varchar(30) not null,
	item_desc varchar(60) not null,
	item_type enum('lost', 'found') not null,
	item_location enum('fowler', 'johnson', 'academic commons', 'berkus') not null,
	item_reward decimal(10, 2) not null,
	item_timestamp timestamp,
	primary key (item_id, user_id), 
	foreign key (user_id) references Users(user_id)  
);

-- dependent on Items
create table ClaimedItems (

);

create table Messages (

);

create table Form (

);
