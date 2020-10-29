create type labelType as enum (
  'label',
  'description');

create table labels (
  uri text,
  type labelType,
  value text,
  count integer not null);

create extension pgcrypto;

create unique index unique_labels on labels using btree (
  digest(uri, 'sha512'::text),
  type,
  digest(value, 'sha512'::text));
