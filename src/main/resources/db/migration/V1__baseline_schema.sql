create table roles (
    id bigint not null auto_increment,
    name varchar(255) not null,
    primary key (id),
    constraint uk_roles_name unique (name)
);

create table users (
    id bigint not null auto_increment,
    username varchar(255) not null,
    full_name varchar(255) null,
    password varchar(255) not null,
    email varchar(255) not null,
    avatar varchar(255) null,
    primary key (id),
    constraint uk_users_username unique (username),
    constraint uk_users_email unique (email)
);

create table user_roles (
    user_id bigint not null,
    role_id bigint not null,
    primary key (user_id, role_id),
    constraint fk_user_roles_user foreign key (user_id) references users(id),
    constraint fk_user_roles_role foreign key (role_id) references roles(id)
);

create table events (
    id bigint not null auto_increment,
    title varchar(255) null,
    description varchar(1000) null,
    location varchar(255) null,
    event_date datetime null,
    total_tickets int null,
    ticket_price double null,
    version bigint null,
    primary key (id)
);

create table bookings (
    id bigint not null auto_increment,
    booking_date datetime null,
    quantity int null,
    total_price double null,
    status varchar(255) null,
    user_id bigint null,
    event_id bigint null,
    primary key (id),
    constraint fk_bookings_user foreign key (user_id) references users(id),
    constraint fk_bookings_event foreign key (event_id) references events(id)
);

create table payments (
    id bigint not null auto_increment,
    amount double null,
    method varchar(255) null,
    status varchar(255) null,
    payment_date datetime null,
    booking_id bigint null,
    primary key (id),
    constraint fk_payments_booking foreign key (booking_id) references bookings(id)
);

create table tickets (
    id bigint not null auto_increment,
    ticket_code varchar(255) null,
    booking_id bigint null,
    primary key (id),
    constraint fk_tickets_booking foreign key (booking_id) references bookings(id)
);

create table reminders (
    id bigint not null auto_increment,
    event_reminder boolean null,
    user_id bigint null,
    primary key (id),
    constraint fk_reminders_user foreign key (user_id) references users(id)
);

create table favorites (
    id bigint not null auto_increment,
    user_id bigint null,
    event_id bigint null,
    primary key (id),
    constraint uk_favorites_user_event unique (user_id, event_id),
    constraint fk_favorites_user foreign key (user_id) references users(id),
    constraint fk_favorites_event foreign key (event_id) references events(id)
);
