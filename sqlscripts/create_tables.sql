
create table Users (
	user_id varchar(40) not null,
	first_name varchar(30),
	email varchar(30),
	primary key(user_id) 
);

create table Items (
	item_id int not null,
	user_id varchar(40) not null,
	item_name varchar(30) not null,
	item_desc varchar(60),
	item_type varchar(10), 
	item_location enum('fowler', 'johnson', 'academic commons', 'berkus'),
	item_reward decimal(10, 2),
	item_timestamp timestamp,
	primary key (item_id, user_id), 
	foreign key (user_id) references Users(user_id)  
);

-- user_id, first_name, email
-- item_id, user_id, item_name, item_desc, item_type, item_location, item_reward, item_timestamp

-- dependent on Items
create table ClaimedItems (

);

create table Messages (

);

create table Form (

);
