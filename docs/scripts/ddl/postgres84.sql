CREATE TABLE annotation (
    id bigint NOT NULL,
    outpoint integer,
    inpoint integer,
    media_package_id character varying(36),
    session_id character varying(255),
    created timestamp without time zone,
    user_id character varying(255),
    length integer,
    annotation_val text,
    annotation_type character varying(255)
);

CREATE TABLE capture_agent_state (
    name character varying(255) NOT NULL,
    state character varying(255) NOT NULL,
    capabilities bytea,
    last_heard_from bigint NOT NULL,
    url character varying(255)
);

CREATE TABLE dictionary (
    text character varying(255) NOT NULL,
    language character varying(255) NOT NULL,
    weight double precision,
    count bigint,
    stopword boolean
);

CREATE TABLE host_registration (
    host character varying(255) NOT NULL,
    maintenance boolean NOT NULL,
    max_jobs integer NOT NULL,
    online boolean NOT NULL
);

CREATE TABLE job (
    id bigint NOT NULL,
    status integer,
    payload text,
    datestarted timestamp without time zone,
    datecreated timestamp without time zone,
    runtime bigint,
    queuetime bigint,
    datecompleted timestamp without time zone,
    host character varying(255),
    service_type character varying(255)
);

CREATE TABLE sched_event (
    event_id bigint NOT NULL,
    startdate timestamp without time zone,
    resources character varying(255),
    series character varying(255),
    subject character varying(255),
    enddate timestamp without time zone,
    recurrencepattern character varying(255),
    creator character varying(255),
    title character varying(255),
    duration bigint,
    recurrence character varying(255),
    description text,
    contributor character varying(255),
    device character varying(255),
    language character varying(255),
    license character varying(255),
    seriesid character varying(255)
);

CREATE TABLE sched_metadata (
    md_key character varying(255) NOT NULL,
    md_val character varying(255),
    event_id bigint NOT NULL
);

CREATE TABLE sequence (
    seq_name character varying(50) NOT NULL,
    seq_count numeric(38,0)
);

CREATE TABLE series (
    series_id character varying(128) NOT NULL,
    description text
);

CREATE TABLE series_metadata (
    metadata_key character varying(128) NOT NULL,
    metadata_val text,
    series_id character varying(128) NOT NULL
);

CREATE TABLE service_registration (
    host character varying(255) NOT NULL,
    service_type character varying(255) NOT NULL,
    path character varying(255) NOT NULL,
    job_producer boolean NOT NULL,
    online boolean NOT NULL,
    hostregistration_host character varying(255)
);

CREATE TABLE upload (
    id character varying(255) NOT NULL,
    total bigint NOT NULL,
    received bigint NOT NULL,
    filename character varying(255) NOT NULL
);

CREATE TABLE user_action (
    id bigint NOT NULL,
    outpoint integer,
    inpoint integer,
    media_package_id character varying(128),
    session_id character varying(255),
    created timestamp without time zone,
    user_id character varying(255),
    length integer,
    type character varying(255)
);


ALTER TABLE ONLY annotation
    ADD CONSTRAINT annotation_pkey PRIMARY KEY (id);

ALTER TABLE ONLY capture_agent_state
    ADD CONSTRAINT capture_agent_state_pkey PRIMARY KEY (name);

ALTER TABLE ONLY dictionary
    ADD CONSTRAINT dictionary_pkey PRIMARY KEY (text, language);

ALTER TABLE ONLY host_registration
    ADD CONSTRAINT host_registration_pkey PRIMARY KEY (host);

ALTER TABLE ONLY job
    ADD CONSTRAINT job_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sched_event
    ADD CONSTRAINT sched_event_pkey PRIMARY KEY (event_id);

ALTER TABLE ONLY sched_metadata
    ADD CONSTRAINT sched_metadata_pkey PRIMARY KEY (md_key, event_id);

ALTER TABLE ONLY sequence
    ADD CONSTRAINT sequence_pkey PRIMARY KEY (seq_name);

ALTER TABLE ONLY series_metadata
    ADD CONSTRAINT series_metadata_pkey PRIMARY KEY (metadata_key, series_id);

ALTER TABLE ONLY series
    ADD CONSTRAINT series_pkey PRIMARY KEY (series_id);

ALTER TABLE ONLY service_registration
    ADD CONSTRAINT service_registration_pkey PRIMARY KEY (host, service_type);

ALTER TABLE ONLY upload
    ADD CONSTRAINT upload_pkey PRIMARY KEY (id);

ALTER TABLE ONLY user_action
    ADD CONSTRAINT user_action_pkey PRIMARY KEY (id);

ALTER TABLE ONLY job
    ADD CONSTRAINT fk_job_host FOREIGN KEY (host, service_type) REFERENCES service_registration(host, service_type);

ALTER TABLE ONLY sched_metadata
    ADD CONSTRAINT fk_sched_metadata_event_id FOREIGN KEY (event_id) REFERENCES sched_event(event_id);

ALTER TABLE ONLY series_metadata
    ADD CONSTRAINT fk_series_metadata_series_id FOREIGN KEY (series_id) REFERENCES series(series_id);

ALTER TABLE ONLY service_registration
    ADD CONSTRAINT fk_service_registration_hostregistration_host FOREIGN KEY (hostregistration_host) REFERENCES host_registration(host);

INSERT INTO SEQUENCE VALUES('SEQ_GEN', 50);

CREATE INDEX dictionary_text on dictionary (text);
CREATE INDEX dictionary_language on dictionary (language);
CREATE INDEX annotation_mp_idx on annotation (media_package_id);
CREATE INDEX user_action_user_idx on user_action (user_id);
CREATE INDEX user_action_mp_idx on user_action (media_package_id);
