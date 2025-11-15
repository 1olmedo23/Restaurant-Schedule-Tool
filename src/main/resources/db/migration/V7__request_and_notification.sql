-- REQUESTS
create table if not exists request (
                                       id bigserial primary key,
                                       type varchar(16) not null,
    status varchar(16) not null,
    requester_id bigint not null references app_user(id),
    request_date date not null,
    created_at timestamptz not null default now(),
    decided_by bigint references app_user(id),
    decided_at timestamptz,
    note varchar(1000),

    offer_assignment_id bigint references assignment(id),
    receiver_id bigint references app_user(id),
    receiver_confirmed boolean not null default false,
    receiver_confirmed_at timestamptz
    );

create index if not exists idx_request_requester_created on request(requester_id, created_at);
create index if not exists idx_request_date on request(request_date);

-- NOTIFICATIONS
create table if not exists notification (
                                            id bigserial primary key,
                                            recipient_id bigint not null references app_user(id),
    type varchar(32) not null,
    payload varchar(2000),
    is_read boolean not null default false,
    created_at timestamptz not null default now()
    );

create index if not exists idx_notification_recipient_created on notification(recipient_id, created_at);
