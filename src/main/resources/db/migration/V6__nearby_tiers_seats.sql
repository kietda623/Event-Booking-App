create table if not exists ticket_tiers (
    id bigint not null auto_increment,
    event_id bigint not null,
    name varchar(255) null,
    price double null,
    total_quantity int null,
    sold_quantity int null default 0,
    description varchar(1000) null,
    created_at datetime null,
    version bigint null,
    primary key (id),
    constraint fk_ticket_tiers_event foreign key (event_id) references events(id)
);

create index idx_ticket_tiers_event on ticket_tiers(event_id);

alter table bookings
    add column tier_id bigint null,
    add column seat_numbers varchar(1000) null;

alter table bookings
    add constraint fk_bookings_tier foreign key (tier_id) references ticket_tiers(id);

alter table tickets
    add column tier_id bigint null;

alter table tickets
    add constraint fk_tickets_tier foreign key (tier_id) references ticket_tiers(id);

create table if not exists seats (
    id bigint not null auto_increment,
    event_id bigint not null,
    tier_id bigint null,
    seat_number varchar(255) null,
    seat_row varchar(255) null,
    seat_col int null,
    status varchar(32) null,
    held_until datetime null,
    held_by_user_id bigint null,
    version bigint null,
    primary key (id),
    constraint uk_seats_event_number unique (event_id, seat_number),
    constraint fk_seats_event foreign key (event_id) references events(id),
    constraint fk_seats_tier foreign key (tier_id) references ticket_tiers(id)
);

create index idx_seats_event on seats(event_id);
create index idx_seats_hold_expiry on seats(status, held_until);
