
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
	item_location enum('fowler', 'johnson', 'academic commons', 'marketplace', 'green bean') not null,
	image blob not null,
	item_timestamp timestamp,
	primary key (item_id, email),
	foreign key (email) references Users(email)
);

create table Messages (
    message_id int not null,
    sender_email varchar(30) not null,
    receiver_email varchar(30) not null,
    message_content text not null,
    message_timestamp timestamp,
    primary key(message_id),
    foreign key(sender_email) references Users(email),
    foreign key(receiver_email) references Users(email)
);



