create type labelType as enum (
  'label',
  'description');

create table labels (
  uri text,
  type labelType,
  value text,
  count integer not null,
  unique (uri, type, value));
