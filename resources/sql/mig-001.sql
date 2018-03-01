create table if not exists script (
  id          bigint     auto_increment primary key,
  created     timestamp  not null default now(),
  updated     timestamp  not null default now(),
  last_run    timestamp,
  name        varchar    not null,
  description varchar    default '',
  category    varchar    default '',
  crontab     varchar    not null default '* * * * *',
  script      varchar    not null,
  language    varchar    not null default 'JavaScript',
  status      enum       ('active', 'inactive') not null default 'inactive'
);

create table if not exists environment (
  updated     timestamp  not null default now(),
  key         varchar    not null primary key,
  value       varchar    not null default '',
  hidden      boolean    not null default false
);

create index if not exists env_key on environment (key);
