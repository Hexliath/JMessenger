create table users(
	id UUID primary key,
	login VARCHAR(255) not null,
	password VARCHAR(60) not null,
	birth_date TIMESTAMP,
	display_name VARCHAR(255),
	avatar VARCHAR(255)
	);
create table auth (
	user_id UUID not null,
	auth_token varchar(64) primary key,
	validity TIMESTAMP,
	user_ip varchar(128),
	attempts numeric,
	CONSTRAINT fk_auth_user FOREIGN KEY (user_id) references users (id)
);
create table ip_filter (
	user_ip varchar(128) primary key,
	validity TIMESTAMP,
	attempts numeric
);
create table channels(
	id UUID primary key,
	channel_name varchar(255) not null,
	channel_type varchar(255) not null,
	creation_time TIMESTAMP not null
);
create table channel_users (
	user_id UUID not null,
	channel_id UUID not null,
	user_role VARCHAR(255) not null,
	join_time TIMESTAMP not null,
	PRIMARY KEY (user_id, channel_id),
	CONSTRAINT fk_user_id FOREIGN KEY (user_id) references users (id),
	CONSTRAINT fk_channel_id FOREIGN KEY (channel_id) references channels (id)
);
create table attachments (
	id UUID primary key,
	content TEXT not null
);
create table messages (
	id UUID primary key,
	channel_id UUID not null,
	user_id UUID not null,
	creation_time TIMESTAMP not null,
	content TEXT,
	attachment_id UUID,
	CONSTRAINT fk_channel_id FOREIGN KEY (channel_id) references channels (id),
	CONSTRAINT fk_user_id FOREIGN KEY (user_id) references users (id),
	CONSTRAINT fk_attachment_id FOREIGN KEY (attachment_id) references attachments (id)
);
create table sockets (
	user_token VARCHAR(64) not null,
	socket_id UUID not null,
	PRIMARY KEY (user_token, socket_id)
);