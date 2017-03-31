CREATE TABLE TSK_PROCESS (
  process_id    VARCHAR(36)         NOT NULL,
  start_task_id VARCHAR(36)         NOT NULL,
  custom_id     VARCHAR(256)        NULL,
  start_time    BIGINT              NOT NULL,
  end_time      BIGINT              NULL,
  state         INT                 NOT NULL,
  return_value  JSONB               NULL,
  start_json    JSONB               NULL,
  actor_id      VARCHAR(500)        NOT NULL,
  task_list     VARCHAR(500)        NULL
);

CREATE INDEX TSK_PROCESS_STATE_IDX ON TSK_PROCESS(STATE);
CREATE INDEX TSK_PROCESS_CUSTOM_ID_IDX ON TSK_PROCESS(CUSTOM_ID);
CREATE INDEX TSK_PROCESS_START_TIME_IDX ON TSK_PROCESS(START_TIME);
CREATE INDEX TSK_PROCESS_ACTOR_ID_IDX ON TSK_PROCESS(ACTOR_ID);
CREATE INDEX TSK_PROCESS_END_TIME_IDX ON TSK_PROCESS(END_TIME);


CREATE TABLE TSK_INTERRUPTED_TASKS (
  ID                SERIAL        NOT NULL PRIMARY KEY,
  PROCESS_ID        VARCHAR(50)   NOT NULL,
  TASK_ID           VARCHAR(50)   NOT NULL,
  STARTER_ID        VARCHAR(500)  NOT NULL,
  ACTOR_ID          VARCHAR(500)  NOT NULL,
  CREATION_DATE     TIMESTAMP(6)  NOT NULL,
  TIME              BIGINT        NOT NULL,
  ERROR_MESSAGE     VARCHAR(500),
  ERROR_CLASS_NAME  VARCHAR(500),
  STACK_TRACE       text,
  MESSAGE_FULL      text,
  CONSTRAINT UNIQUE_TASK UNIQUE (PROCESS_ID, TASK_ID)
);

COMMENT ON COLUMN TSK_INTERRUPTED_TASKS.PROCESS_ID IS 'Interrupted process GUID';
COMMENT ON COLUMN TSK_INTERRUPTED_TASKS.TASK_ID IS 'Interrupted task GUID';
COMMENT ON COLUMN TSK_INTERRUPTED_TASKS.STARTER_ID IS 'Actor ID of process starter actor';
COMMENT ON COLUMN TSK_INTERRUPTED_TASKS.ACTOR_ID IS 'Actor ID of interrupted task';
COMMENT ON COLUMN TSK_INTERRUPTED_TASKS.CREATION_DATE IS 'Row creation date';
COMMENT ON COLUMN TSK_INTERRUPTED_TASKS.TIME IS 'Long representation of a failing time';
COMMENT ON COLUMN TSK_INTERRUPTED_TASKS.ERROR_MESSAGE IS 'Message of the occured actor exception';
COMMENT ON COLUMN TSK_INTERRUPTED_TASKS.ERROR_CLASS_NAME IS 'Full class name for the actor exception';
COMMENT ON COLUMN TSK_INTERRUPTED_TASKS.STACK_TRACE IS 'Full stack trace of the exception got from actor';
COMMENT ON COLUMN TSK_INTERRUPTED_TASKS.MESSAGE_FULL IS 'Full error message text';

CREATE INDEX TSK_INTERRUPTED_TASKS_IP ON TSK_INTERRUPTED_TASKS(STARTER_ID);
CREATE INDEX TSK_INTERRUPTED_TASKS_IA ON TSK_INTERRUPTED_TASKS(ACTOR_ID);
CREATE INDEX TSK_INTERRUPTED_TASKS_IE ON TSK_INTERRUPTED_TASKS(ERROR_CLASS_NAME);
CREATE INDEX TSK_INTERRUPTED_TASKS_IT ON TSK_INTERRUPTED_TASKS(TASK_ID);
CREATE INDEX TSK_INTERRUPTED_TASKS_ITIME ON TSK_INTERRUPTED_TASKS(TIME);

CREATE TABLE TSK_SCHEDULED
(
  ID                    SERIAL              NOT NULL PRIMARY KEY ,
  NAME                  VARCHAR(256)        NOT NULL,
  CRON                  VARCHAR(256)        NOT NULL,
  STATUS                INT                 NOT NULL,
  JSON                  jsonb               NOT NULL,
  CREATED               TIMESTAMP,
  LIMIT_CNT             INT,
  MAX_ERRORS            INT,
  ERR_COUNT             INT,
  LAST_ERR_MESSAGE      TEXT
);


CREATE TABLE TSK_NFN_TRIGGERS (
  ID                SERIAL NOT NULL PRIMARY KEY,
  TYPE              VARCHAR(200) NOT NULL,
  STATE_JSON        jsonb,
  CFG_JSON          jsonb,
  CHANGE_DATE       TIMESTAMP(6) NOT NULL
);

COMMENT ON COLUMN TSK_NFN_TRIGGERS.ID IS 'Row unique identifier';
COMMENT ON COLUMN TSK_NFN_TRIGGERS.TYPE IS 'Trigger type, for the proper handler use';
COMMENT ON COLUMN TSK_NFN_TRIGGERS.STATE_JSON IS 'JSON representation of the state the trigger was previouslt called';
COMMENT ON COLUMN TSK_NFN_TRIGGERS.CFG_JSON IS 'JSON representation of trigger configuration parameters';
COMMENT ON COLUMN TSK_NFN_TRIGGERS.CHANGE_DATE IS 'Row data change date';


CREATE TABLE TSK_NFN_SUBSCRIPTIONS (
  ID              SERIAL NOT NULL PRIMARY KEY,
  ACTORS_JSON     jsonb NOT NULL,
  EMAILS_JSON     jsonb NOT NULL,
  CHANGE_DATE     TIMESTAMP(6) NOT NULL
);

COMMENT ON COLUMN TSK_NFN_SUBSCRIPTIONS.ID IS 'Row unique identifier';
COMMENT ON COLUMN TSK_NFN_SUBSCRIPTIONS.ACTORS_JSON IS 'JSON representation of actors list';
COMMENT ON COLUMN TSK_NFN_SUBSCRIPTIONS.EMAILS_JSON IS 'JSON representation of emails list';
COMMENT ON COLUMN TSK_NFN_SUBSCRIPTIONS.CHANGE_DATE IS 'Row data change date';


CREATE TABLE TSK_NFN_LINKS (
  ID                SERIAL NOT NULL PRIMARY KEY,
  SUBSCRIPTION_ID   BIGINT NOT NULL,
  TRIGGER_ID        BIGINT NOT NULL
);

COMMENT ON COLUMN TSK_NFN_LINKS.ID IS 'Row unique identifier';
COMMENT ON COLUMN TSK_NFN_LINKS.SUBSCRIPTION_ID IS 'Foreign key for subscription id';
COMMENT ON COLUMN TSK_NFN_LINKS.TRIGGER_ID IS 'Foreign key for trigger id';
