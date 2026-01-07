CREATE TABLE IF NOT EXISTS shedlock (
  name        VARCHAR(64)   NOT NULL,
  lock_until  TIMESTAMPTZ   NOT NULL,
  locked_at   TIMESTAMPTZ   NOT NULL,
  locked_by   VARCHAR(255)  NOT NULL,
  CONSTRAINT pk_shedlock PRIMARY KEY (name)
);
