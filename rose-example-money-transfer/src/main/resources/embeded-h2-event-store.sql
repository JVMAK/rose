-- local jdbc event store/snapshot db init script.

DROP table IF EXISTS events;
DROP table IF EXISTS  snapshots;

create table events (
  entity_type VARCHAR,
  entity_id VARCHAR,
  entity_version INTEGER,
  event_type varchar,
  event_json varchar,
  PRIMARY KEY(entity_type, entity_id, entity_version)
);

create table snapshots (
  entity_type VARCHAR,
  entity_id VARCHAR,
  entity_version INTEGER,
  snapshot_json VARCHAR,
  PRIMARY KEY(entity_type, entity_id,entity_version)
);
