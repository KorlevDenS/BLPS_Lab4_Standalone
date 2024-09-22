create table client (
    id serial primary key,
    login varchar not null unique,
    password varchar not null,
    email varchar not null unique,
    sex varchar(3) default 'нет',
    registered DATE not null,
    birthday DATE not null
);

create table chapter (
    id serial primary key,
    title varchar not null unique,
    description varchar not null default '',
    creator integer references client,
    created DATE not null
);

create table topic (
    id serial primary key,
    chapter integer references chapter on delete cascade not null,
    title varchar not null,
    text varchar not null,
    owner integer references client,
    created DATE not null
);

create table image (
    id serial primary key,
    topic integer references topic on delete cascade not null,
    link varchar,
    created DATE not null
);

create table rating (
    id serial primary key,
    creator integer references client,
    topic integer references topic on delete cascade not null,
    rating integer,
    created DATE not null
);

create table comment (
    id serial primary key,
    text varchar default '' not null,
    commentator integer references client,
    created DATE not null,
    quote int references comment,
    topic integer references topic on delete cascade
);

create table notification (
    id serial primary key,
    recipient integer references client on delete cascade not null,
    initiator integer references client,
    description varchar default '' not null,
    topic integer references topic on delete cascade
);

create table subscription (
    client integer references client on delete cascade not null,
    topic integer references topic on delete cascade,
    primary key(client, topic)
);

create table role (
    id serial primary key,
    name varchar not null
);

insert into role (name) values ('ROLE_USER');
insert into role (name) values ('ROLE_MODER');
insert into role (name) values ('ROLE_ADMIN');

create table permission (
    client integer references client on delete cascade not null,
    role integer references role,
    primary key(client, role)
);
