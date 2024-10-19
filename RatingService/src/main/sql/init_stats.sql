create table client_stats (
    login varchar primary key,
    rating double precision default 1,
    activity integer default 1
);

create table topic_stats (
    id integer primary key,
    views integer default 0,
    temporal_views integer default 0,
    temporal_comments integer default 0,
    temporal_fame double precision,
    fame double precision,
    owner varchar references client_stats on delete cascade
);

create table rating_stats (
    id serial primary key,
    creator varchar references client_stats,
    topic integer references topic_stats on delete cascade not null,
    rating integer default 0
);