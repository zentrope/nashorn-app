create table if not exists extension (
  id bigint auto_increment primary key,
  created timestamp not null default now(),
  updated timestamp not null default now(),
  last_run timestamp,
  name varchar not null,
  description varchar default '',
  category varchar default '',
  crontab varchar not null default '* * * * *',
  script varchar not null,
  language varchar not null default 'javascript',
  status enum ('active', 'inactive')
);

create index if not exists ext_status on extension(status);
