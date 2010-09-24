/*
 * PostgreSQL 8.4 upgrade script for Matterhorn 1.0 to 1.1
 */

/**
 * MH-5445 (overview of series and services)
 */
alter table service_registration add column online boolean NOT NULL default 'f';
alter table job rename column type to job_type;
alter table job add column runtime bigint;
alter table job add column queuetime bigint;
alter table job add FOREIGN KEY (job_type, host) REFERENCES service_registration(job_type, host);
