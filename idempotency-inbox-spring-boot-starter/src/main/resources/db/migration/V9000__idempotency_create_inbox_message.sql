CREATE SEQUENCE inbox_message_seq
  MINVALUE 0
  MAXVALUE 999999999999999999
  INCREMENT BY 1
  START WITH 1
  CACHE 1
  NO CYCLE;

CREATE TABLE inbox_message (
  id           BIGINT        NOT NULL DEFAULT nextval('inbox_message_seq'),
  message_id   VARCHAR(256)  NOT NULL,
  received_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

  CONSTRAINT pk_inbox_message PRIMARY KEY (id),
  CONSTRAINT ux_inbox_message_message_id UNIQUE (message_id)
);

CREATE INDEX ix_inbox_message_received_at ON inbox_message (received_at);

ALTER SEQUENCE inbox_message_seq OWNED BY inbox_message.id;
