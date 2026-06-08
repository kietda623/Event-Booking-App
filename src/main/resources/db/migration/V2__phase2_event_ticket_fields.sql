alter table events add column image_url varchar(255);
alter table events add column latitude double precision;
alter table events add column longitude double precision;
alter table events add column created_at timestamp;
alter table events add column updated_at timestamp;

alter table tickets add column ticket_type varchar(255);
alter table tickets add column seat_number varchar(255);
