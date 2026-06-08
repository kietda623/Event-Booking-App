alter table favorites
    add column created_at datetime null;

alter table tickets
    add column status varchar(32) not null default 'ACTIVE',
    add column checked_in boolean not null default false,
    add column checked_in_at datetime null,
    add column user_id bigint null;

create table if not exists refunds (
    id bigint not null auto_increment,
    booking_id bigint not null,
    amount double null,
    status varchar(32) null,
    created_at datetime null,
    primary key (id),
    constraint fk_refunds_booking foreign key (booking_id) references bookings(id)
);
