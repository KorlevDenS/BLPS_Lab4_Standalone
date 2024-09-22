create table client (
    login varchar primary key,
    rating double precision default 1,
    activity integer default 1
);

create table topic (
    id integer primary key,
    views integer default 0,
    temporal_views integer default 0,
    temporal_comments integer default 0,
    temporal_fame double precision,
    fame double precision,
    owner varchar references client on delete cascade
);

create table rating (
    id serial primary key,
    creator varchar references client,
    topic integer references topic on delete cascade not null,
    rating integer default 0
);