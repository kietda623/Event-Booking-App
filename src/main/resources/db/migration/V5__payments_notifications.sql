alter table payments
    add column payment_intent_id varchar(255) null,
    add column client_secret varchar(1000) null;

create index idx_payments_payment_intent on payments(payment_intent_id);

create table if not exists push_subscriptions (
    id bigint not null auto_increment,
    endpoint varchar(1000) not null,
    p256dh varchar(255) not null,
    auth varchar(255) not null,
    user_id bigint null,
    primary key (id),
    constraint uk_push_subscriptions_endpoint unique (endpoint),
    constraint fk_push_subscriptions_user foreign key (user_id) references users(id)
);

create index idx_push_subscriptions_user on push_subscriptions(user_id);
