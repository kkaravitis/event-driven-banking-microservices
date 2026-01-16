CREATE TABLE inbox_message (
  message_id   VARCHAR(256)  PRIMARY KEY,
  received_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_inbox_message_received_at ON inbox_message (received_at);
