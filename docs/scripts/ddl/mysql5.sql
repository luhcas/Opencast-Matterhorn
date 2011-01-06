CREATE TABLE `ANNOTATION` (
  `ID` bigint(20) NOT NULL,
  `OUTPOINT` int(11) default NULL,
  `INPOINT` int(11) default NULL,
  `MEDIA_PACKAGE_ID` varchar(36) collate utf8_unicode_ci default NULL,
  `SESSION_ID` varchar(255) collate utf8_unicode_ci default NULL,
  `CREATED` datetime default NULL,
  `USER_ID` varchar(255) collate utf8_unicode_ci default NULL,
  `LENGTH` int(11) default NULL,
  `ANNOTATION_VAL` text collate utf8_unicode_ci,
  `ANNOTATION_TYPE` varchar(255) collate utf8_unicode_ci default NULL,
  PRIMARY KEY  (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE `CAPTURE_AGENT_STATE` (
  `NAME` varchar(255) collate utf8_unicode_ci NOT NULL,
  `STATE` varchar(255) collate utf8_unicode_ci NOT NULL,
  `CAPABILITIES` tinyblob,
  `LAST_HEARD_FROM` bigint(20) NOT NULL,
  `URL` varchar(255) collate utf8_unicode_ci default NULL,
  PRIMARY KEY  (`NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE `DICTIONARY` (
  `TEXT` varchar(255) collate utf8_unicode_ci NOT NULL,
  `LANGUAGE` varchar(255) collate utf8_unicode_ci NOT NULL,
  `WEIGHT` double default NULL,
  `COUNT` bigint(20) default NULL,
  `STOPWORD` tinyint(1) default '0',
  PRIMARY KEY  (`TEXT`,`LANGUAGE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE `HOST_REGISTRATION` (
  `HOST` varchar(255) collate utf8_unicode_ci NOT NULL,
  `MAINTENANCE` tinyint(1) NOT NULL default '0',
  `MAX_JOBS` int(11) NOT NULL,
  `ONLINE` tinyint(1) NOT NULL default '0',
  PRIMARY KEY  (`HOST`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE `SERVICE_REGISTRATION` (
  `HOST` varchar(255) collate utf8_unicode_ci NOT NULL,
  `SERVICE_TYPE` varchar(255) collate utf8_unicode_ci NOT NULL,
  `PATH` varchar(255) collate utf8_unicode_ci NOT NULL,
  `JOB_PRODUCER` tinyint(1) NOT NULL default '0',
  `ONLINE` tinyint(1) NOT NULL default '0',
  `HOSTREGISTRATION_HOST` varchar(255) collate utf8_unicode_ci default NULL,
  PRIMARY KEY  (`HOST`,`SERVICE_TYPE`),
  KEY `FK_SERVICE_REGISTRATION_HOSTREGISTRATION_HOST` (`HOSTREGISTRATION_HOST`),
  CONSTRAINT `FK_SERVICE_REGISTRATION_HOSTREGISTRATION_HOST` FOREIGN KEY (`HOSTREGISTRATION_HOST`) REFERENCES `HOST_REGISTRATION` (`HOST`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE `JOB` (
  `ID` bigint(20) NOT NULL,
  `STATUS` int(11) default NULL,
  `PAYLOAD` text collate utf8_unicode_ci,
  `DATESTARTED` datetime default NULL,
  `DATECREATED` datetime default NULL,
  `RUNTIME` bigint(20) default NULL,
  `QUEUETIME` bigint(20) default NULL,
  `DATECOMPLETED` datetime default NULL,
  `HOST` varchar(255) collate utf8_unicode_ci default NULL,
  `SERVICE_TYPE` varchar(255) collate utf8_unicode_ci default NULL,
  PRIMARY KEY  (`ID`),
  KEY `FK_JOB_HOST` (`HOST`,`SERVICE_TYPE`),
  CONSTRAINT `FK_JOB_HOST` FOREIGN KEY (`HOST`, `SERVICE_TYPE`) REFERENCES `SERVICE_REGISTRATION` (`HOST`, `SERVICE_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE `SCHED_EVENT` (
  `EVENT_ID` bigint(20) NOT NULL,
  `STARTDATE` datetime default NULL,
  `RESOURCES` varchar(255) collate utf8_unicode_ci default NULL,
  `SERIES` varchar(255) collate utf8_unicode_ci default NULL,
  `SUBJECT` varchar(255) collate utf8_unicode_ci default NULL,
  `ENDDATE` datetime default NULL,
  `RECURRENCEPATTERN` varchar(255) collate utf8_unicode_ci default NULL,
  `CREATOR` varchar(255) collate utf8_unicode_ci default NULL,
  `TITLE` varchar(255) collate utf8_unicode_ci default NULL,
  `DURATION` bigint(20) default NULL,
  `RECURRENCE` varchar(255) collate utf8_unicode_ci default NULL,
  `DESCRIPTION` text collate utf8_unicode_ci,
  `CONTRIBUTOR` varchar(255) collate utf8_unicode_ci default NULL,
  `DEVICE` varchar(255) collate utf8_unicode_ci default NULL,
  `LANGUAGE` varchar(255) collate utf8_unicode_ci default NULL,
  `LICENSE` varchar(255) collate utf8_unicode_ci default NULL,
  `SERIESID` varchar(255) collate utf8_unicode_ci default NULL,
  PRIMARY KEY  (`EVENT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE `SCHED_METADATA` (
  `MD_KEY` varchar(255) collate utf8_unicode_ci NOT NULL,
  `MD_VAL` varchar(255) collate utf8_unicode_ci default NULL,
  `EVENT_ID` bigint(20) NOT NULL,
  PRIMARY KEY  (`MD_KEY`,`EVENT_ID`),
  KEY `FK_SCHED_METADATA_EVENT_ID` (`EVENT_ID`),
  CONSTRAINT `FK_SCHED_METADATA_EVENT_ID` FOREIGN KEY (`EVENT_ID`) REFERENCES `SCHED_EVENT` (`EVENT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE `SEQUENCE` (
  `SEQ_NAME` varchar(50) collate utf8_unicode_ci NOT NULL,
  `SEQ_COUNT` decimal(38,0) default NULL,
  PRIMARY KEY  (`SEQ_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE `SERIES` (
  `SERIES_ID` varchar(128) collate utf8_unicode_ci NOT NULL,
  `DESCRIPTION` text collate utf8_unicode_ci,
  PRIMARY KEY  (`SERIES_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE `SERIES_METADATA` (
  `METADATA_KEY` varchar(128) collate utf8_unicode_ci NOT NULL,
  `METADATA_VAL` text collate utf8_unicode_ci,
  `SERIES_ID` varchar(128) collate utf8_unicode_ci NOT NULL,
  PRIMARY KEY  (`METADATA_KEY`,`SERIES_ID`),
  KEY `FK_SERIES_METADATA_SERIES_ID` (`SERIES_ID`),
  CONSTRAINT `FK_SERIES_METADATA_SERIES_ID` FOREIGN KEY (`SERIES_ID`) REFERENCES `SERIES` (`SERIES_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE `UPLOAD` (
  `ID` varchar(255) collate utf8_unicode_ci NOT NULL,
  `total` bigint(20) NOT NULL,
  `received` bigint(20) NOT NULL,
  `filename` varchar(255) collate utf8_unicode_ci NOT NULL,
  PRIMARY KEY  (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE `USER_ACTION` (
  `ID` bigint(20) NOT NULL,
  `OUTPOINT` int(11) default NULL,
  `INPOINT` int(11) default NULL,
  `MEDIA_PACKAGE_ID` varchar(128) collate utf8_unicode_ci default NULL,
  `SESSION_ID` varchar(255) collate utf8_unicode_ci default NULL,
  `CREATED` datetime default NULL,
  `USER_ID` varchar(255) collate utf8_unicode_ci default NULL,
  `LENGTH` int(11) default NULL,
  `TYPE` varchar(255) collate utf8_unicode_ci default NULL,
  PRIMARY KEY  (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

INSERT INTO `SEQUENCE` VALUES('SEQ_GEN', 50);

CREATE INDEX `DICTIONARY_TEXT` ON `DICTIONARY` (`TEXT`);
CREATE INDEX `DICTIONARY_LANGUAGE` ON `DICTIONARY` (`LANGUAGE`);
CREATE INDEX `ANNOTATION_MP_IDX` on `ANNOTATION` (`MEDIA_PACKAGE_ID`);
CREATE INDEX `USER_ACTION_USER_IDX` on `USER_ACTION` (`USER_ID`);
CREATE INDEX `USER_ACTION_MP_IDX` on `USER_ACTION` (`MEDIA_PACKAGE_ID`);
