/*
 * MySQL 5 upgrade script for Matterhorn 1.0 to 1.1
 */

/**
 * MH-5445 (overview of series and services)
 */
alter table `SERVICE_REGISTRATION` add column `ONLINE` tinyint NOT NULL default '0';
alter table `JOB` change `TYPE` `JOB_TYPE` varchar(255) collate utf8_unicode_ci default NULL;
alter table `JOB` add column `RUNTIME` bigint(20) default NULL;
alter table `JOB` add column `QUEUETIME` bigint(20) default NULL;
create index JOB_TYPE_HOST on `JOB` (`JOB_TYPE`, `HOST`);
alter table `JOB` add FOREIGN KEY (`HOST`, `JOB_TYPE`) REFERENCES `SERVICE_REGISTRATION` (`HOST`, `JOB_TYPE`);
