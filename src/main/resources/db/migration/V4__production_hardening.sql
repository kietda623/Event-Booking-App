create table if not exists refresh_tokens (
    id bigint not null auto_increment,
    user_id bigint not null,
    token_hash varchar(128) not null,
    expires_at datetime null,
    revoked boolean null,
    primary key (id),
    constraint uk_refresh_tokens_hash unique (token_hash),
    constraint fk_refresh_tokens_user foreign key (user_id) references users(id) on delete cascade
);

create index idx_events_start_time on events(event_date);
create index idx_bookings_user_status on bookings(user_id, status);
create index idx_tickets_user on tickets(user_id);
create index idx_favorites_user_event on favorites(user_id, event_id);
create index idx_refresh_tokens_user on refresh_tokens(user_id);
