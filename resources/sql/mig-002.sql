create table if not exists script_log (
  script_id   bigint     references script(id),
  created     timestamp  not null default now(),
  result      varchar    not null default '',
  output      varchar    not null default '',
  error       varchar    not null default '',
  status      enum       ('success', 'failure') not null default 'success'
);
