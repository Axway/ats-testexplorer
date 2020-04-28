--CREATE USER "AtsUser" WITH SUPERUSER CREATEDB LOGIN PASSWORD 'AtsPassword';

DO
$body$
BEGIN
   IF NOT EXISTS (
      SELECT
      FROM   pg_catalog.pg_user
      WHERE  usename = 'AtsUser') THEN

      CREATE USER "AtsUser" WITH SUPERUSER CREATEDB LOGIN PASSWORD 'AtsPassword';
   END IF;
END
$body$;

CREATE SCHEMA IF NOT EXISTS "AtsUser" AUTHORIZATION "AtsUser";

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA "AtsUser" TO "AtsUser";

CREATE TABLE "tInternal" (
    id    serial       PRIMARY KEY,
    key   varchar(50)  NOT NULL,
    value varchar(256) NOT NULL
);

INSERT INTO "tInternal" ("key","value") VALUES ('version', '4.0.7_draft');
INSERT INTO "tInternal" ("key","value") VALUES ('initialVersion', '19');
INSERT INTO "tInternal" ("key","value") VALUES ('internalVersion', '19');
INSERT INTO "tInternal" ("key", "value") VALUES ('Install_of_intVer_19', now());

CREATE TABLE "tRuns" (
    runId       serial       PRIMARY KEY,
    productName varchar(50)  NOT NULL,
    versionName varchar(50)  NOT NULL,
    buildName   varchar(50)  NOT NULL,
    runName     varchar(255) NOT NULL,
    OS          varchar(50)  NULL,
    dateStart   timestamp    NOT NULL,
    dateEnd     timestamp    NULL,
    userNote    varchar(255) NULL,
    hostName    varchar(255) NULL
);

CREATE TABLE "tSuites" (
    suiteId   serial       PRIMARY KEY,
    runId     integer      NOT NULL,
    name      varchar(255) NOT NULL,
    dateStart timestamp    NOT NULL,
    dateEnd   timestamp    NULL,
    userNote  varchar(255) NULL,
    package   varchar(255) NULL
);

CREATE TABLE "tScenarios" (
    scenarioId  serial        PRIMARY KEY,
    name        varchar(255)  NOT NULL,
    fullName    varchar(1000) NOT NULL UNIQUE,
    description varchar(4000) NULL,
    userNote    varchar(255)  NULL
);

CREATE TABLE "tTestcases" (
    testcaseId serial       PRIMARY KEY,
    scenarioId integer      NOT NULL,
    suiteId    integer      NOT NULL,
    name       varchar(255) NOT NULL,
    dateStart  timestamp     NOT NULL,
    dateEnd    timestamp     NULL,
    result     smallint      NULL,
    userNote   varchar(255)  NULL
);

CREATE TABLE "tMachines" (
    machineId    serial       PRIMARY KEY,
    machineName  varchar(255) NOT NULL UNIQUE,
    machineAlias varchar(100) NULL,
    machineInfo  text         NULL
);

CREATE TABLE "tRunMetainfo" (
  metainfoId serial       PRIMARY KEY,
  runId      integer      NOT NULL,
  name       varchar(50)  NOT NULL,
  value      varchar(512) NOT NULL
);

CREATE TABLE "tScenarioMetainfo" (
  metainfoId serial       PRIMARY KEY,
  scenarioId integer      NOT NULL,
  name       varchar(50)  NOT NULL,
  value      varchar(512) NOT NULL
);

CREATE TABLE "tTestcaseMetainfo" (
  metainfoId serial       PRIMARY KEY,
  testcaseId integer      NOT NULL,
  name       varchar(50)  NOT NULL,
  value      varchar(512) NOT NULL
);

CREATE TABLE "tSytemProperties" (
    id        serial       PRIMARY KEY,
    runId     int          NOT NULL,
    machineId int          NOT NULL,
    key       varchar(50)  NOT NULL,
    value     varchar(256) NULL
);

CREATE TABLE "tMessageTypes" (
    messageTypeId serial      PRIMARY KEY,
    name          varchar(50) NULL
);

CREATE TABLE "tUniqueMessages" (
    uniqueMessageId serial        PRIMARY KEY,
    hash            bytea         NOT NULL,
    message         varchar(3950) NULL UNIQUE
);

CREATE TABLE "tRunMessages" (
  runMessageId    serial       PRIMARY KEY,
  runId           integer      NOT NULL,
  messageTypeId   integer      NOT NULL,
  timestamp       timestamp    NOT NULL,
  escapeHtml      boolean      NOT NULL,
  uniqueMessageId integer      NOT NULL,
  machineId       integer      NOT NULL,
  threadName      varchar(255) NOT NULL
);

CREATE TABLE "tSuiteMessages" (
  suiteMessageId  serial       PRIMARY KEY,
  suiteId         integer      NOT NULL,
  messageTypeId   integer      NOT NULL,
  timestamp       timestamp    NOT NULL,
  escapeHtml      boolean      NOT NULL,
  uniqueMessageId integer      NOT NULL,
  machineId       integer      NOT NULL,
  threadName      varchar(255) NOT NULL
);

CREATE TABLE "tMessages" (
    messageId       serial       PRIMARY KEY,
    testcaseId      integer      NULL,
    messageTypeId   integer      NOT NULL,
    timestamp       timestamp    NOT NULL,
    escapeHtml      boolean      NULL,
    uniqueMessageId integer      NOT NULL,
    machineId       integer      NOT NULL,
    threadName      varchar(255) NOT NULL,
    parentMessageId integer
);

CREATE TABLE "tStatsTypes" (
    statsTypeId  serial       PRIMARY KEY,
    name         varchar(255) NOT NULL,
    units        varchar(50)  NOT NULL,
    params       text NULL,
    parentName   varchar(255) DEFAULT '' NOT NULL,
    internalName varchar(255) DEFAULT '' NOT NULL
);

CREATE TABLE "tSystemStats" (
    systemStatsId serial    PRIMARY KEY,
    testcaseId    integer   NOT NULL,
    machineId     integer   NOT NULL,
    statsTypeId   integer   NOT NULL,
    timestamp     timestamp NOT NULL,
    value         real      NOT NULL
);

CREATE UNIQUE INDEX system_type_per_timestamp_idx ON "tSystemStats" (testcaseId, machineId, statsTypeId, timestamp);

CREATE TABLE "tLoadQueues" (
    loadQueueId      serial        PRIMARY KEY,
    testcaseId       integer       NOT NULL,
    name             varchar(255)  NOT NULL,
    sequence         integer       NULL,
    hostsList        varchar(255)  NULL,
    threadingPattern varchar(255)  NOT NULL,
    numberThreads    integer       NOT NULL,
    machineId        integer       NOT NULL,
    dateStart        timestamp     NOT NULL,
    dateEnd          timestamp     NULL,
    result           smallint      NOT NULL,
    userNote         varchar(255)  NULL
);

CREATE TABLE "tCheckpointsSummary" (
    checkpointSummaryId serial       PRIMARY KEY,
    loadQueueId         integer      NOT NULL,
    name                varchar(150) NOT NULL,
    numRunning          integer      NULL,
    numPassed           integer      NULL,
    numFailed           integer      NULL,
    minResponseTime     integer      NOT NULL,
    maxResponseTime     integer      NOT NULL,
    avgResponseTime     real         NOT NULL,
    minTransferRate     real         NOT NULL,
    maxTransferRate     real         NOT NULL,
    avgTransferRate     real         NOT NULL,
    transferRateUnit    varchar(50)  NULL
);

CREATE TABLE "tCheckpoints" (
    checkpointId        bigserial    PRIMARY KEY,
    checkpointSummaryId integer      NOT NULL,
    name                varchar(150) NOT NULL,
    responseTime        integer      NOT NULL,
    transferRate        real         NOT NULL,
    transferRateUnit    varchar(50)  NULL,
    result              smallint     NOT NULL,
    endTime             timestamp    NULL
);

CREATE TABLE "tColumnDefinition" (
    id              serial      PRIMARY KEY,
    columnName      varchar(50) NOT NULL,
    columnPosition  smallint    NOT NULL,
    columnLength    smallint    NOT NULL,
    parentTable     varchar(10) NOT NULL,
    isVisible       boolean     NOT NULL
);

ALTER TABLE "tTestcases" ADD CONSTRAINT FK_tTestcases_tSuites FOREIGN KEY(suiteId)
REFERENCES "tSuites" (suiteId)
ON UPDATE CASCADE
ON DELETE CASCADE;

/*
ALTER TABLE [dbo].[tTestcases] CHECK CONSTRAINT [FK_tTestcases_tSuites]
*/


ALTER TABLE "tRunMetainfo" ADD CONSTRAINT FK_tRunMetainfo_tRuns FOREIGN KEY(runId)
REFERENCES "tRuns" (runId)
ON UPDATE CASCADE
ON DELETE CASCADE;

/*
ALTER TABLE [dbo].[tRunMetainfo] CHECK CONSTRAINT [FK_tRunMetainfo_tRuns]
*/

ALTER TABLE "tScenarioMetainfo" ADD CONSTRAINT FK_tScenarioMetainfo_tScenarios FOREIGN KEY(scenarioId)
REFERENCES "tScenarios" (scenarioId)
ON UPDATE CASCADE
ON DELETE CASCADE;

ALTER TABLE "tTestcaseMetainfo" ADD CONSTRAINT FK_tTestcaseMetainfo_tTestcases FOREIGN KEY(testcaseId)
REFERENCES "tTestcases" (testcaseId)
ON UPDATE CASCADE
ON DELETE CASCADE;

/*
ALTER TABLE [dbo].[tTestcaseMetainfo] CHECK CONSTRAINT [FK_tTestcaseMetainfo_tTestcases]
*/

ALTER TABLE "tRunMessages" ADD CONSTRAINT FK_tRunMessages_tMessageTypes FOREIGN KEY(messageTypeId) 
REFERENCES "tMessageTypes" (messageTypeId);

ALTER TABLE "tRunMessages" ADD CONSTRAINT FK_tRunMessages_tUniqueMessages FOREIGN KEY(uniqueMessageId) 
REFERENCES "tUniqueMessages" (uniqueMessageId);

ALTER TABLE "tRunMessages" ADD CONSTRAINT FK_tRunMessages_tMachines FOREIGN KEY( machineId)
REFERENCES "tMachines" (machineId);

ALTER TABLE "tSuiteMessages" ADD CONSTRAINT FK_tSuiteMessages_tMessageTypes FOREIGN KEY(messageTypeId) 
REFERENCES "tMessageTypes" (messageTypeId);

ALTER TABLE "tSuiteMessages" ADD CONSTRAINT FK_tSuiteMessages_tUniqueMessages FOREIGN KEY(uniqueMessageId) 
REFERENCES "tUniqueMessages" (uniqueMessageId);

ALTER TABLE "tSuiteMessages" ADD CONSTRAINT FK_tSuiteMessages_tMachines FOREIGN KEY(machineId)
REFERENCES "tMachines" (machineId);

/*
ALTER TABLE [dbo].[tSystemStats] ADD  CONSTRAINT [DF_tSystemStats_machineId]  DEFAULT ((0)) FOR [machineId]
*/

ALTER TABLE "tCheckpoints" ADD CONSTRAINT FK_tCheckpoints_tCheckpointsSummary FOREIGN KEY(checkpointSummaryId)
REFERENCES "tCheckpointsSummary" (checkpointSummaryId)
ON UPDATE CASCADE
ON DELETE CASCADE;

/*
ALTER TABLE [dbo].[tCheckpoints] CHECK CONSTRAINT [FK_tCheckpoints_tCheckpointsSummary]
*/

ALTER TABLE "tCheckpointsSummary" ADD CONSTRAINT FK_tCheckpointsSummary_tLoadQueues FOREIGN KEY(loadQueueId)
REFERENCES "tLoadQueues" (loadQueueId)
ON UPDATE CASCADE
ON DELETE CASCADE;

/*
ALTER TABLE [dbo].[tCheckpointsSummary] CHECK CONSTRAINT [FK_tCheckpointsSummary_tLoadQueues]
*/

ALTER TABLE "tLoadQueues" ADD CONSTRAINT FK_tLoadQueues_tTestcases FOREIGN KEY(testcaseId)
REFERENCES "tTestcases" (testcaseId)
ON UPDATE CASCADE
ON DELETE CASCADE;

/*
ALTER TABLE [dbo].[tLoadQueues] CHECK CONSTRAINT [FK_tLoadQueues_tTestcases]
*/

ALTER TABLE "tMessages" ADD CONSTRAINT FK_tMessages_tMachines FOREIGN KEY(machineId)
REFERENCES "tMachines" (machineId);

/*
ALTER TABLE [dbo].[tMessages] CHECK CONSTRAINT [FK_tMessages_tMachines]
*/

ALTER TABLE "tMessages" ADD CONSTRAINT FK_tMessages_tMessageTypes FOREIGN KEY(messageTypeId)
REFERENCES "tMessageTypes" (messageTypeId);

/*
ALTER TABLE [dbo].[tMessages] CHECK CONSTRAINT [FK_tMessages_tMessageTypes]
*/

ALTER TABLE "tMessages" ADD CONSTRAINT FK_tMessages_tTestcases FOREIGN KEY(testcaseId)
REFERENCES "tTestcases" (testcaseId)
ON UPDATE CASCADE
ON DELETE CASCADE;

/*
ALTER TABLE [dbo].[tMessages] CHECK CONSTRAINT [FK_tMessages_tTestcases]
*/

ALTER TABLE "tMessages" ADD CONSTRAINT FK_tMessages_tUniqueMessages1 FOREIGN KEY(uniqueMessageId)
REFERENCES "tUniqueMessages" (uniqueMessageId);

/*
ALTER TABLE [dbo].[tMessages] CHECK CONSTRAINT [FK_tMessages_tUniqueMessages1]
*/


ALTER TABLE "tSuites" ADD CONSTRAINT FK_tSuites_tRuns FOREIGN KEY(runId)
REFERENCES "tRuns" (runId)
ON UPDATE CASCADE
ON DELETE CASCADE;

/*
ALTER TABLE [dbo].[tSuites] CHECK CONSTRAINT [FK_tSuites_tRuns]
*/


ALTER TABLE "tSystemStats" ADD CONSTRAINT FK_tSystemStats_tMachines FOREIGN KEY(machineId)
REFERENCES "tMachines" (machineId);

/*
ALTER TABLE [dbo].[tSystemStats] CHECK CONSTRAINT [FK_tSystemStats_tMachines]
*/

ALTER TABLE "tSystemStats" ADD CONSTRAINT FK_tSystemStats_tStatsTypes FOREIGN KEY(statsTypeId)
REFERENCES "tStatsTypes" (statsTypeId)
ON UPDATE CASCADE
ON DELETE CASCADE;

/*
ALTER TABLE [dbo].[tSystemStats] CHECK CONSTRAINT [FK_tSystemStats_tStatsTypes]
*/


ALTER TABLE "tSystemStats" ADD CONSTRAINT FK_tSystemStats_tTestcases FOREIGN KEY(testcaseId)
REFERENCES "tTestcases" (testcaseId)
ON UPDATE CASCADE
ON DELETE CASCADE;

/*
ALTER TABLE [dbo].[tSystemStats] CHECK CONSTRAINT [FK_tSystemStats_tTestcases]
*/


ALTER TABLE "tSytemProperties" ADD CONSTRAINT FK_tSytemProperties_tMachines FOREIGN KEY(machineId)
REFERENCES "tMachines" (machineId);

/*
ALTER TABLE [dbo].[tSytemProperties] CHECK CONSTRAINT [FK_tSytemProperties_tMachines]
*/

ALTER TABLE "tSytemProperties" ADD CONSTRAINT FK_tSytemProperties_tRuns FOREIGN KEY(runId)
REFERENCES "tRuns" (runId)
ON UPDATE CASCADE
ON DELETE CASCADE;

/*
ALTER TABLE [dbo].[tSytemProperties] CHECK CONSTRAINT [FK_tSytemProperties_tRuns]
*/


ALTER TABLE "tTestcases" ADD CONSTRAINT FK_tTestcases_tScenarios FOREIGN KEY(scenarioId)
REFERENCES "tScenarios" (scenarioId)
ON UPDATE CASCADE
ON DELETE CASCADE;

/*
ALTER TABLE [dbo].[tTestcases] CHECK CONSTRAINT [FK_tTestcases_tScenarios]
*/


-- THE FOLLOWING DATA MUST BE ADDED AFTER THE SCHEMA IS READY --

-- Fill in the message type levels we support --
INSERT INTO "tMessageTypes" (messageTypeId, "name") VALUES (1, 'fatal');
INSERT INTO "tMessageTypes" (messageTypeId, "name") VALUES (2, 'error');
INSERT INTO "tMessageTypes" (messageTypeId, "name") VALUES (3, 'warning');
INSERT INTO "tMessageTypes" (messageTypeId, "name") VALUES (4, 'info');
INSERT INTO "tMessageTypes" (messageTypeId, "name") VALUES (5, 'debug');
INSERT INTO "tMessageTypes" (messageTypeId, "name") VALUES (6, 'trace');
INSERT INTO "tMessageTypes" (messageTypeId, "name") VALUES (7, 'system');

INSERT INTO "tColumnDefinition" (columnName, columnPosition, columnLength, parentTable, isVisible) 
VALUES ('Run',1,200,'tRuns',true),('Product',2,80,'tRuns',true),('Version',3,55,'tRuns',true),('Build',4,55,'tRuns',true),
       ('OS',5,110,'tRuns',true),('Total',6,65,'tRuns',true),('Failed',7,65,'tRuns',true),('Skipped',8,40,'tRuns',true),
       ('Passed',9,40,'tRuns',true),('Running',10,80,'tRuns',false),('Start',11,130,'tRuns',true),('End',12,130,'tRuns',true),
       ('Duration',13,145,'tRuns',true),('User Note',14,135,'tRuns',true),
       
       ('Suite',1,300,'tSuite',true),('Total',2,65,'tSuite',true),('Failed',3,65,'tSuite',true),('Skipped',4,40,'tSuite',true),
       ('Passed',5,40,'tSuite',true),('Running',6,50,'tSuite',false),('Start',7,130,'tSuite',true),('End',8,130,'tSuite',true),
       ('Duration',9,145,'tSuite',true),('User Note',10,310,'tSuite',true),('Package',11,100,'tSuite',false),
       
       ('Scenario',1,245,'tScenario',true),('Description',2,130,'tScenario',true),('State',3,110,'tScenario',true),('TestcasesTotal',4,65,'tScenario',true),
       ('TestcasesFailed',5,65,'tScenario',true),('Passed',6,40,'tScenario',true),('Running',7,50,'tScenario',false),('Start',8,130,'tScenario',true),
       ('End',9,130,'tScenario',true),('Duration',10,145,'tScenario',true),('User Note',11,163,'tScenario',true),
     
       ('Testcase',1,350,'tTestcases',true),('State',2,110,'tTestcases',true),('Start',3,80,'tTestcases',true),('End',4,80,'tTestcases',true),
       ('Duration',5,145,'tTestcases',true),('User Note',6,360,'tTestcases',true),
     
       ('Date',1,60,'tTestcase',false),('Time',2,110,'tTestcase',true),('Thread',3,100,'tTestcase',true),('Machine',4,100,'tTestcase',true),
       ('Level',5,80,'tTestcase',true),('Message',6,840,'tTestcase',true);
       

CREATE FUNCTION sp_get_db_version()
RETURNS TABLE (
	dbVersion VARCHAR(50)
) AS $func$
BEGIN
	RETURN QUERY
	SELECT value FROM "tInternal" WHERE "key" = 'version';
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_get_db_initial_version()
RETURNS TABLE (
	initialVersion VARCHAR(50)
) AS $func$
BEGIN
	RETURN QUERY
	SELECT value FROM "tInternal" WHERE "key" = 'initialVersion';
END;
$func$ LANGUAGE plpgsql;

CREATE FUNCTION sp_get_db_internal_version()
RETURNS TABLE (
	internalVersion VARCHAR(50)
) AS $func$
BEGIN
	RETURN QUERY
	SELECT value FROM "tInternal" WHERE "key" = 'internalVersion';
END;
$func$ LANGUAGE plpgsql;

CREATE FUNCTION sp_get_column_definitions()
RETURNS TABLE (
	columnName      varchar(50),
    columnPosition  smallint,
    columnLength    smallint,
    parentTable     varchar(10),
    isVisible       boolean
) AS $func$
BEGIN
   RETURN QUERY
   SELECT "tColumnDefinition".columnName, "tColumnDefinition".columnPosition, 
          "tColumnDefinition".columnLength, "tColumnDefinition".parentTable, "tColumnDefinition".isVisible 
   FROM "tColumnDefinition";
END;
$func$ LANGUAGE plpgsql;

CREATE FUNCTION sp_get_navigation_for_suites(_runId VARCHAR(30))
RETURNS TABLE (
    runName varchar(255)
) AS $func$
BEGIN
	RETURN QUERY
    SELECT "tRuns".runName FROM "tRuns" WHERE "tRuns".runId = (SELECT CAST(_runId AS integer));
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_get_navigation_for_scenarios(_suiteId VARCHAR(30))
RETURNS TABLE (
    runId INTEGER,
    runName VARCHAR(50),
    suiteName VARCHAR(50)
) AS $func$
BEGIN
    RETURN QUERY
    SELECT tr.runId, tr.runName, ts.name AS suiteName FROM "tSuites" ts
    INNER JOIN "tRuns" tr ON (ts.runId = tr.runId) WHERE ts.suiteId = CAST(_suiteId AS INTEGER);
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_get_navigation_for_testcases(_suiteId varchar(30))
RETURNS TABLE (
    runId INTEGER,
    runName VARCHAR(50),
    scenarioId INTEGER,
    suiteName VARCHAR(50),
    scenarioName VARCHAR(50)
) AS $func$
BEGIN
    RETURN QUERY
    SELECT "tRuns".runId, 
           "tRuns".runName, 
           "tTestcases".scenarioId, 
           "tSuites".name AS suiteName, 
           "tScenarios".name AS scenarioName
    FROM "tTestcases"
    INNER JOIN "tScenarios" ON ("tScenarios".scenarioId = "tTestcases".scenarioId)
    INNER JOIN "tSuites"  ON ("tSuites".suiteId = "tTestcases".suiteId)
    INNER JOIN "tRuns"  ON ("tSuites".runId = "tRuns".runId)
    WHERE "tTestcases".suiteId = CAST(_suiteId AS INTEGER);
END;
$func$ LANGUAGE plpgsql;

CREATE FUNCTION sp_get_navigation_for_testcase(_testcaseId varchar(30))
RETURNS TABLE (
    runId INTEGER,
    runName VARCHAR(50),
    suiteId INTEGER,
    suiteName VARCHAR(50),
    scenarioId INTEGER,
    scenarioName VARCHAR(50),
    testcaseName VARCHAR(50),
    dateStart TIMESTAMP,
    dateEnd TIMESTAMP
) AS $func$
BEGIN
    RETURN QUERY
    SELECT	tr.runId,
            tr.runName,
            tt.suiteId,
            ts.name AS suiteName,
            tss.scenarioId,
            tss.name AS scenarioName,
            tt.name AS testcaseName,
            tt.dateStart,
            tt.dateEnd
    FROM "tTestcases" tt
    INNER JOIN "tScenarios" tss ON (tt.scenarioId = tss.scenarioId)
    INNER JOIN "tSuites" ts ON (ts.suiteId = tt.suiteId)
    INNER JOIN "tRuns" tr ON (ts.runId = tr.runId)
    WHERE tt.testcaseId = CAST(_testcaseId AS INTEGER);
END;
$func$ LANGUAGE plpgsql;

CREATE FUNCTION sp_start_run(productName VARCHAR(50), versionName VARCHAR(50), buildName VARCHAR(50), runName VARCHAR(255), 
                             OSName VARCHAR(50), dateStart timestamp, hostName VARCHAR(255), OUT rowsInserted INTEGER, OUT RunID INTEGER)
RETURNS record AS $$
BEGIN
    INSERT INTO "tRuns" (productName, versionName, buildName, runName, OS, dateStart, hostName) VALUES (productName, versionName, buildName, runName, OSName, dateStart, hostName);
	SELECT last_value INTO RunID FROM "tRuns_runid_seq";
    GET DIAGNOSTICS rowsInserted = ROW_COUNT;
END;
$$ LANGUAGE plpgsql;


CREATE FUNCTION sp_get_runs_count(whereClause VARCHAR(1000))
RETURNS TABLE (
	runsCount bigint
) AS $func$
BEGIN
	RETURN QUERY EXECUTE
	'SELECT COUNT(*) AS runsCount FROM ' || $$"tRuns"$$ || ' ' || $$whereClause$$;
END;
$func$ LANGUAGE plpgsql;

CREATE FUNCTION sp_get_runs(startRecord VARCHAR(100), recordsCount VARCHAR(100), whereClause VARCHAR(1000), 
                            sortCol VARCHAR(100), sortType VARCHAR(100))
RETURNS TABLE (
	    runId integer,
		productName varchar(50),
		versionName varchar(50),
		buildName varchar(50),
		runName varchar(255),
		OS varchar(50),
		hostName varchar(255),
		testcasesTotal integer,
		testcasesFailed integer,
		testcasesPassedPercent double precision,
		testcaseIsRunning integer,
		scenariosTotal integer,
		scenariosFailed integer,
		scenariosSkipped integer,
		dateStart timestamp,
		dateEnd timestamp,
		duration integer,
		userNote varchar(255)
) AS $func$
DECLARE
    runsCursor refcursor;
    runsCursorSQL VARCHAR(1000);
    
    testcasesTotal integer;
    testcasesFailed integer;
    testcasesSkipped integer;
    
    testcaseIsRunning integer;
    
    scenariosTotal integer;
    scenariosFailed integer;
    scenariosSkipped integer;
    
    _runId integer;
    
    fetchStatus boolean DEFAULT true;
BEGIN
	DROP TABLE IF EXISTS "tmpRuns";
	CREATE TEMP TABLE IF NOT EXISTS "tmpRuns" (
		runId integer NOT NULL,
		productName varchar(50) NOT NULL,
		versionName varchar(50) NOT NULL,
		buildName varchar(50) NOT NULL,
		runName varchar(255) NOT NULL,
		OS varchar(50) NULL,
		hostName varchar(255) NULL,
		testcasesTotal integer NOT NULL,
		testcasesFailed integer NOT NULL,
		testcasesPassedPercent double precision NOT NULL,
		testcaseIsRunning integer NOT NULL,
		scenariosTotal integer NOT NULL,
		scenariosFailed integer NOT NULL,
		scenariosSkipped integer NOT NULL,
		dateStart timestamp NOT NULL,
		dateEnd timestamp NULL,
		duration integer NOT NULL,
		userNote varchar(255) NULL
    );
    DROP TABLE IF EXISTS "tmpTestcases";
	CREATE TEMP TABLE IF NOT EXISTS "tmpTestcases" (
		testcaseId integer NOT NULL,
		scenarioId integer NOT NULL,
		result integer NOT NULL
	);
	OPEN runsCursor FOR EXECUTE 'SELECT tr.runId FROM (
                                 SELECT  runId, ROW_NUMBER() OVER (ORDER BY ' || sortCol || ' ' || sortType || ') AS Row
                                 FROM ' || $$"tRuns"$$
                                 || whereClause || ' ) as tr
                                 WHERE tr.Row >= ' || startRecord || ' AND tr.Row <= ' || recordsCount;
	WHILE fetchStatus LOOP
		FETCH NEXT FROM runsCursor INTO _runId;
		fetchStatus := FOUND;
		IF fetchStatus THEN
			-- cache the testcases
			INSERT INTO "tmpTestcases" SELECT testcaseId, scenarioId, result FROM "tTestcases" WHERE suiteId IN (SELECT suiteId FROM "tSuites" WHERE "tSuites".runId = _runId);
			-- data about all testcases in this run
            testcasesTotal       = (SELECT COUNT(testcaseId) FROM "tmpTestcases" );
            testcasesFailed	     = (SELECT COUNT(testcaseId) FROM "tmpTestcases" WHERE result = 0);
            testcasesSkipped     = (SELECT COUNT(testcaseId) FROM "tmpTestcases" WHERE result = 2);
            testcaseIsRunning    = (SELECT COUNT(testcaseId) FROM "tmpTestcases" WHERE result = 4);
            -- data about all disctinct scenarios in this run
			scenariosTotal   = (select COUNT(DISTINCT(scenarioId)) from "tmpTestcases");
			scenariosFailed  = (select COUNT(DISTINCT(scenarioId)) from "tmpTestcases" where result = 0);
			scenariosSkipped = (select COUNT(DISTINCT(scenarioId)) from "tmpTestcases" where result = 2);
			INSERT INTO "tmpRuns"
                SELECT  "tRuns".runId,
                        "tRuns".productName,
                        "tRuns".versionName,
                        "tRuns".buildName,
                        "tRuns".runName,
                        "tRuns".OS,
                        "tRuns".hostName,

                        testcasesTotal,
                        testcasesFailed,
                        (testcasesTotal - testcasesFailed - testcasesSkipped) * 100.00 / CASE WHEN testcasesTotal=0 THEN 1 ELSE testcasesTotal END AS testcasesPassedPercent,
                        testcaseIsRunning,

                        scenariosTotal,
                        scenariosFailed,
                        scenariosSkipped,

                        "tRuns".dateStart,
                        "tRuns".dateEnd,

                        CASE WHEN "tRuns".dateEnd IS NULL
                            THEN EXTRACT(EPOCH FROM now() at time zone 'utc' - "tRuns".dateStart )
                            ELSE EXTRACT(EPOCH FROM "tRuns".dateEnd - "tRuns".dateStart)
                        END AS duration,

                        "tRuns".userNote

                FROM    "tRuns"
                WHERE   "tRuns".runId = _runId;
                -- cleanup the temp testcases table
                DELETE FROM "tmpTestcases";
		END IF;
	END LOOP;
	CLOSE runsCursor;
	--DEALLOCATE runsCursor;
	RETURN QUERY
	SELECT * from "tmpRuns";
END;
$func$ LANGUAGE plpgsql;

CREATE FUNCTION sp_end_run(_runId INTEGER, _dateEnd timestamp, OUT rowsUpdated INTEGER)
RETURNS integer AS $$
BEGIN
	UPDATE "tRuns" SET dateEnd = _dateEnd WHERE "tRuns".runId = _runId;
    GET DIAGNOSTICS rowsUpdated = ROW_COUNT;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION sp_delete_run(runIds VARCHAR(1000))
RETURNS VOID as $$
DECLARE
	_delimiter VARCHAR(10) DEFAULT ',';
	idIndex SMALLINT;
    idToken VARCHAR(100);
BEGIN
	WHILE runIds <> '' LOOP
		idIndex := POSITION(_delimiter in runIds);
		IF idIndex > 0 THEN
			idToken := LEFT(runIds, idIndex-1);
            runIds := RIGHT(runIds, LENGTH(runIds)-idIndex);
		ELSE
			idToken := runIds;
            runIds := '';
		END IF;
		DELETE FROM "tRunMessages" WHERE "tRunMessages".runId = CAST(idToken AS INTEGER);
        DELETE FROM "tSuiteMessages" WHERE "tSuiteMessages".suiteId IN (SELECT suiteId FROM "tSuites" WHERE "tSuites".runId = CAST(idToken AS INTEGER));

        DELETE FROM "tRuns" WHERE runId = CAST(idToken AS INTEGER);

        -- Delete any possible orphan messages in tUniqueMessages. This is used as some kind of cleanup
        -- DELETE FROM tUniqueMessages WHERE tUniqueMessages.uniqueMessageId NOT IN ( SELECT tMessages.uniqueMessageId FROM tMessages );
	END LOOP;
END;
$$ LANGUAGE plpgsql;


CREATE FUNCTION sp_update_run(_runId VARCHAR(50), _productName VARCHAR(50), _versionName VARCHAR(50), 
                              _buildName VARCHAR(50), _runName VARCHAR(255), _osName VARCHAR(50), 
                              _userNote VARCHAR(255), _hostName VARCHAR(255), OUT rowsUpdated INTEGER )
RETURNS INTEGER AS $$
DECLARE
	currentProductName VARCHAR(50);
	currentVersionName VARCHAR(50);
	currentBuildName VARCHAR(50);
	currentRunName VARCHAR(255);
	currentOsName VARCHAR(50);
	currentUserNote VARCHAR(255);
	currentHostName VARCHAR(255);
BEGIN
	-- get the current values
	currentProductName = (SELECT "tRuns".productName FROM "tRuns" WHERE runId = _runId::INTEGER);
    currentVersionName = (SELECT "tRuns".versionName FROM "tRuns" WHERE runId = _runId::INTEGER);
    currentBuildName   = (SELECT "tRuns".buildName FROM "tRuns" WHERE runId = _runId::INTEGER);
    currentRunName     = (SELECT "tRuns".runName FROM "tRuns" WHERE runId = _runId::INTEGER);
    currentOsName      = (SELECT "tRuns".OS FROM "tRuns" WHERE runId = _runId::INTEGER);
    currentUserNote    = (SELECT "tRuns".userNote FROM "tRuns" WHERE runId = _runId::INTEGER);
    currentHostName    = (SELECT "tRuns".hostName FROM "tRuns" WHERE runId = _runId::INTEGER);

	-- update the Run info, if the provided value is null, then use the current value
    UPDATE "tRuns"
    SET productName =  CASE
                           WHEN _productName IS NULL THEN currentProductName
					       ELSE _productName
                       END,
        versionName =  CASE
                           WHEN _versionName IS NULL  THEN currentVersionName
                           ELSE _versionName
                       END,
        buildName =    CASE
                           WHEN _buildName IS NULL THEN currentBuildName
                           ELSE _buildName
                       END,
        runName =      CASE
                           WHEN _runName IS NULL THEN currentRunName
                           ELSE _runName
                       END,
        OS =           CASE
                           WHEN _osName IS NULL THEN currentOsName
                           ELSE _osName
                       END,
        userNote =     CASE
                          WHEN _userNote IS NULL THEN currentUserNote
                          ELSE _userNote
                       END,
        hostName =     CASE
                          WHEN _hostName IS NULL THEN currentHostName
                          ELSE _hostName
                       END
	WHERE "tRuns".runId = _runId::INTEGER;

    GET DIAGNOSTICS rowsUpdated = ROW_COUNT;
END;
$$ LANGUAGE plpgsql;


CREATE FUNCTION sp_add_run_metainfo(runId INTEGER, metaKey VARCHAR(50), metaValue VARCHAR(50), OUT rowsInserted INTEGER)
RETURNS INTEGER AS $$
BEGIN
	INSERT INTO "tRunMetainfo" (runId, "name", "value") VALUES (runId, metaKey, metaValue);
	GET DIAGNOSTICS rowsInserted = ROW_COUNT;
END;
$$ LANGUAGE plpgsql;


CREATE FUNCTION sp_start_suite(suiteName VARCHAR(255), _runId INT, package VARCHAR(255), _dateStart timestamp, OUT rowsInserted INTEGER, OUT suiteId INTEGER)
RETURNS record AS $$
BEGIN
	INSERT INTO "tSuites" (name, runId, package, dateStart) VALUES (suiteName, _runId, package, _dateStart);
	SELECT last_value INTO suiteId FROM "tSuites_suiteid_seq";
    GET DIAGNOSTICS rowsInserted = ROW_COUNT;
END;
$$ LANGUAGE plpgsql;


CREATE FUNCTION sp_get_suites_count(whereClause VARCHAR(1000))
RETURNS TABLE (
	suitesCount bigint
) AS $func$
BEGIN
	RETURN QUERY EXECUTE
	'SELECT COUNT(*) AS suitesCount FROM ' || $$"tSuites"$$ || whereClause;
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_get_suites(startRecord VARCHAR(100), recordsCount VARCHAR(100), whereClause VARCHAR(1000), sortCol VARCHAR(100), sortType VARCHAR(100))
RETURNS TABLE (
    suiteId integer,
    runId integer,
    name varchar(255),
    testcasesTotal integer,
    testcasesFailed integer,
    testcasesPassedPercent double precision,
    testcaseIsRunning integer,
    scenariosTotal integer,
    scenariosFailed integer,
    scenariosSkipped integer,
    dateStart timestamp,
    dateEnd timestamp,
    duration integer,
    userNote varchar(255),
    package varchar(255)
) AS $func$
DECLARE
    suitesCursor refcursor;
    suitesCursorSQL VARCHAR(1000);
    
    testcasesTotal integer;
    testcasesFailed integer;
    testcasesSkipped integer;
    testcaseIsRunning integer;

    scenariosTotal integer;
    scenariosFailed integer;
    scenariosSkipped integer;

    _suiteId integer;
    fetchStatus boolean DEFAULT true;
BEGIN
    DROP TABLE IF EXISTS "tmpSuites";
    CREATE TEMP TABLE IF NOT EXISTS "tmpSuites" (
        suiteId integer NOT NULL,
        runId integer NOT NULL,
        name varchar(255) NOT NULL,
        testcasesTotal integer NOT NULL,
        testcasesFailed integer NOT NULL,
        testcasesPassedPercent double precision NOT NULL,
        testcaseIsRunning integer NOT NULL,
        scenariosTotal integer NOT NULL,
        scenariosFailed integer NOT NULL,
        scenariosSkipped integer NOT NULL,
        dateStart timestamp NOT NULL,
        dateEnd timestamp NULL,
        duration integer NOT NULL,
        userNote varchar(255) NULL,
        package varchar(255) NULL
    );
    DROP TABLE IF EXISTS "tmpTestcases";
    CREATE TEMP TABLE IF NOT EXISTS "tmpTestcases" (
        testcaseId integer NOT NULL,
        scenarioId integer NOT NULL,
        result integer NOT NULL
    );
    OPEN suitesCursor FOR EXECUTE 'SELECT tr.suiteId FROM ( SELECT suiteId, ROW_NUMBER() OVER (ORDER BY ' || sortCol || ' ' || sortType || ') AS Row
                                                            FROM ' || $$"tSuites"$$ || whereClause || 
                                                          ' ) as tr
                                   WHERE tr.Row >= ' || startRecord || ' AND tr.Row <= ' || recordsCount;
    WHILE fetchStatus LOOP
        FETCH NEXT FROM suitesCursor INTO _suiteId;
        fetchStatus := FOUND;
        IF fetchStatus THEN
            -- cache the testcases
            INSERT INTO "tmpTestcases" SELECT testcaseId, scenarioId, result FROM "tTestcases" WHERE "tTestcases".suiteId = _suiteId;
            -- data about all testcases in this suite
            testcasesTotal       = (SELECT COUNT(testcaseId) FROM "tmpTestcases" );
            testcasesFailed	     = (SELECT COUNT(testcaseId) FROM "tmpTestcases" WHERE result = 0);
            testcasesSkipped     = (SELECT COUNT(testcaseId) FROM "tmpTestcases" WHERE result = 2);
            testcaseIsRunning    = (SELECT COUNT(testcaseId) FROM "tmpTestcases" WHERE result = 4);
            -- data about all disctinct scenarios in this run
			scenariosTotal   = (select COUNT(DISTINCT(scenarioId)) from "tmpTestcases");
			scenariosFailed  = (select COUNT(DISTINCT(scenarioId)) from "tmpTestcases" where result = 0);
			scenariosSkipped = (select COUNT(DISTINCT(scenarioId)) from "tmpTestcases" where result = 2);
			
			INSERT INTO "tmpSuites"
                SELECT  "tSuites".suiteId,
                		"tSuites".runId,
                        "tSuites".name,

                        testcasesTotal,
                        testcasesFailed,
                        (testcasesTotal - testcasesFailed - testcasesSkipped) * 100.00 / CASE WHEN testcasesTotal=0 THEN 1 ELSE testcasesTotal END  AS testcasesPassedPercent,
                        testcaseIsRunning AS testcaseIsRunning,

                        scenariosTotal,
                        scenariosFailed,
                        scenariosSkipped,

                        "tSuites".dateStart,
                        "tSuites".dateEnd,

                        CASE WHEN "tSuites".dateEnd IS NULL
                            THEN EXTRACT(EPOCH FROM now() at time zone 'utc' - "tSuites".dateStart)
                            ELSE EXTRACT(EPOCH FROM "tSuites".dateEnd - "tSuites".dateStart)
                        END AS duration,

                        "tSuites".userNote,
                        "tSuites".package
                FROM    "tSuites"
                WHERE   "tSuites".suiteId = _suiteId;
			
			-- cleanup the temp testcases table
			DELETE FROM "tmpTestcases";
        END IF;
    END LOOP;
    CLOSE suitesCursor;
	--DEALLOCATE suitesCursor;
	RETURN QUERY
	SELECT * from "tmpSuites";
END;
$func$ LANGUAGE plpgsql;

CREATE FUNCTION sp_end_suite(_suiteId INTEGER, _dateEnd timestamp, OUT rowsUpdated INTEGER)
RETURNS integer AS $$
BEGIN
	UPDATE "tSuites" SET dateEnd = _dateEnd WHERE "tSuites".suiteId = _suiteId;
    GET DIAGNOSTICS rowsUpdated = ROW_COUNT;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION sp_delete_suite(suiteIds VARCHAR(1000))
RETURNS VOID as $$
DECLARE
    _delimiter VARCHAR(10) DEFAULT ',';
	idIndex SMALLINT;
    idToken VARCHAR(100);
BEGIN
    WHILE suiteIds <> '' LOOP
		idIndex = position(_delimiter in suiteIds);
		IF idIndex > 0 THEN
			idToken := LEFT(suiteIds, idIndex-1);
            suiteIds := RIGHT(suiteIds, LENGTH(suiteIds)-idIndex);
		ELSE
			idToken := suiteIds;
            suiteIds := '';
		END IF;
		DELETE FROM "tSuiteMessages" WHERE "tSuiteMessages".suiteId IN (SELECT suiteId FROM "tSuites" WHERE "tSuites".suiteId = CAST(idToken AS INTEGER));
		
        DELETE FROM "tSuites" WHERE "tSuites".suiteId = CAST(idToken AS INTEGER);
	END LOOP;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION sp_update_suite(_suiteId INTEGER, _suiteName VARCHAR(50), _userNote VARCHAR(255), OUT rowsUpdated INTEGER)
RETURNS INTEGER AS $$
DECLARE
    currentSuiteName VARCHAR(50);
    currentUserNote VARCHAR(255);
BEGIN
    -- get the current values
    currentSuiteName = (SELECT "tSuites".name FROM "tSuites" WHERE "tSuites".suiteId = _suiteId);
    currentUserNote  = (SELECT "tSuites".userNote FROM "tSuites" WHERE "tSuites".suiteId = _suiteId);
    -- update the Suite info, if the provided value is null, then use the current value
    UPDATE "tSuites"
    SET name =     CASE
                       WHEN _suiteName IS NULL THEN currentSuiteName
                       ELSE _suiteName
                   END,
        userNote = CASE
                       WHEN _userNote IS NULL THEN currentUserNote
                       ELSE _userNote
                   END
     WHERE  "tSuites".suiteId = _suiteId;
    
    GET DIAGNOSTICS rowsUpdated = ROW_COUNT;
END;
$$ LANGUAGE plpgsql;


CREATE FUNCTION sp_start_testcase(_suiteId INTEGER, suiteFullName VARCHAR(255) ,scenarioName VARCHAR(255), 
                                  scenarioDescription VARCHAR(4000) ,testcaseName VARCHAR(255) ,dateStart TIMESTAMP,
                                  OUT rowsInserted INTEGER ,OUT tcId INTEGER)
RETURNS record AS $$
DECLARE
    scenarioId INTEGER;
    -- POSSIBLE TEST CASE RESULT VALUES
    -- 0 FAILED
    -- 1 PASSED
    -- 2 SKIPPED
    -- 4 RUNNING
    result INTEGER DEFAULT 4; -- The test is RUNNING IN THE BEGINNING
    scenarioFullName VARCHAR(4000);
    existingScenarioId INTEGER DEFAULT 0;
BEGIN
    -- Construct full name. The format is <java package>.<java class>.<name>
    -- in case of data driven tests, the <name> contains java method name and values of input arguments: <java method name>(arg1, arg2)
    scenarioFullName := suiteFullName || '@' || scenarioName;
    -- empty end time for the current Suite
    UPDATE "tSuites" SET dateEnd = null WHERE suiteId = _suiteId;
    -- Check if this scenario already exists. Insert a new one if needed
    existingScenarioId := (SELECT "tScenarios".scenarioId FROM "tScenarios" WHERE "tScenarios".fullName = scenarioFullName);
    IF existingScenarioId > 0 THEN
        -- this is an already existing scenario
        scenarioId := existingScenarioId;
        UPDATE "tScenarios" SET description = scenarioDescription WHERE "tScenarios".scenarioId = existingScenarioId;
    ELSE
        -- this is a new scenario
        INSERT INTO "tScenarios" (fullName, name, description) VALUES (scenarioFullName, scenarioName, scenarioDescription);
        SELECT last_value INTO scenarioId FROM "tScenarios_scenarioid_seq";
    END IF;
    -- Insert a new testcase
    INSERT INTO "tTestcases" (suiteId, scenarioId, name, dateStart, result) VALUES (_suiteId, scenarioId, testcaseName, dateStart, result);
    SELECT last_value INTO tcId FROM "tTestcases_testcaseid_seq";
    GET DIAGNOSTICS rowsInserted = ROW_COUNT;
END;
$$ LANGUAGE plpgsql;


CREATE FUNCTION sp_end_testcase(_testcaseId INTEGER, _result INTEGER, _dateEnd TIMESTAMP, OUT rowsUpdated INTEGER)
RETURNS integer AS $$
DECLARE
    numberFailedQueues INTEGER;
BEGIN
    numberFailedQueues = (SELECT COUNT(loadQueueId) FROM "tLoadQueues" WHERE testcaseId = _testcaseId AND result = 0);
    IF numberFailedQueues > 0 THEN
         _result := 0; 
    END IF;
    -- end the testcase
    UPDATE "tTestcases" SET result = _result, dateEnd = _dateEnd WHERE testcaseId = _testcaseId;

    GET DIAGNOSTICS rowsUpdated = ROW_COUNT;
END;
$$ LANGUAGE plpgsql;


CREATE FUNCTION sp_get_testcases_count(whereClause VARCHAR(1000))
RETURNS TABLE (
	testcasesCount bigint
) AS $func$
BEGIN
	RETURN QUERY EXECUTE
	'SELECT COUNT(*) AS testcasesCount FROM ' || $$"tTestcases"$$ ||' '|| whereClause;
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_get_testcases(startRecord VARCHAR(100), recordsCount VARCHAR(100), whereClause VARCHAR(1000), sortCol VARCHAR(100), sortType VARCHAR(100))
RETURNS TABLE (
	testcaseId integer,
    scenarioId integer,
    suiteId    integer,
    name       varchar(255),
    dateStart  timestamp,
    dateEnd    timestamp,
    duration   integer,
    result     smallint,
    userNote   varchar(255)
) AS $func$
DECLARE
	testcasesCursor refcursor;
    fetchStatus boolean DEFAULT true;
    _testcaseId INTEGER;
BEGIN
    DROP TABLE IF EXISTS "tmpTestcases";
    CREATE TEMP TABLE IF NOT EXISTS "tmpTestcases" (
        	testcaseId integer      PRIMARY KEY,
            scenarioId integer      NOT NULL,
            suiteId    integer      NOT NULL,
            name       varchar(255) NOT NULL,
            dateStart  timestamp    NOT NULL,
            dateEnd    timestamp    NULL,
            duration   integer      NOT NULL,
            result     smallint     NULL,
            userNote   varchar(255) NULL
    );
    
    OPEN testcasesCursor FOR EXECUTE
    'SELECT tr.testcaseId FROM (
                SELECT testcaseId, rank() OVER (ORDER BY testcaseId) AS Row
                FROM ' || $$"tTestcases"$$ ||' ' || whereClause || ') as tr
    WHERE tr.Row >= ' || startRecord || ' AND tr.Row <= ' || recordsCount;
    
    WHILE fetchStatus LOOP
    
        FETCH NEXT FROM testcasesCursor INTO _testcaseId;
		fetchStatus := FOUND;
		
		IF fetchStatus THEN
		
			INSERT INTO "tmpTestcases"
			SELECT	"tTestcases".testcaseId,
				    "tTestcases".scenarioId,
				    "tTestcases".suiteId,
				    "tTestcases".name,
				    "tTestcases".dateStart,
				    "tTestcases".dateEnd,
				    CASE WHEN "tTestcases".dateEnd IS NULL
				         THEN EXTRACT(EPOCH FROM "tTestcases".dateStart - now() at time zone 'utc')
                         ELSE EXTRACT(EPOCH FROM "tTestcases".dateStart - "tTestcases".dateEnd)
				    END AS duration,
				    "tTestcases".result,
                    "tTestcases".userNote
            FROM    "tTestcases"
            WHERE   "tTestcases".testcaseId = _testcaseId;
            
		END IF;
		
	END LOOP;
    CLOSE testcasesCursor;
    RETURN QUERY EXECUTE
    'SELECT * FROM ' || $$"tmpTestcases"$$ || ' ORDER BY ' || sortCol || ' ' || sortType;
    
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_delete_testcase(testcaseIds VARCHAR(1000))
RETURNS VOID as $$
DECLARE
	_delimiter VARCHAR(10) DEFAULT ',';
	idIndex SMALLINT;
    idToken VARCHAR(100);
BEGIN
	WHILE testcaseIds <> '' LOOP
		idIndex = position(_delimiter in testcaseIds);
		IF idIndex > 0 THEN
			idToken := LEFT(testcaseIds, idIndex-1);
            testcaseIds := RIGHT(testcaseIds, LENGTH(testcaseIds)-idIndex);
		ELSE
			idToken := testcaseIds;
            testcaseIds := '';
		END IF;
		
		DELETE FROM "tTestcases" WHERE "tTestcases".testcaseId = CAST(idToken AS INTEGER);
	END LOOP;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION sp_add_testcase_metainfo(_testcaseId INTEGER, metaKey VARCHAR(50), metaValue VARCHAR(50), OUT rowsInserted INTEGER)
RETURNS INTEGER AS $$
BEGIN
	INSERT INTO "tTestcaseMetainfo" (testcaseId, "name", "value") VALUES (_testcaseId, metaKey, metaValue);
	GET DIAGNOSTICS rowsInserted = ROW_COUNT;
END;
$$ LANGUAGE plpgsql;


CREATE FUNCTION sp_change_testcase_state(scenarioIds VARCHAR(255), testcaseIds VARCHAR(500), _result INTEGER)
RETURNS VOID AS $func$
DECLARE
    _sql VARCHAR(4000);
    _scenarioId INTEGER;
BEGIN
    
    IF testcaseIds IS NULL THEN
        _sql := N'UPDATE ' || $$"tTestcases"$$ || ' SET result = ' || CAST( _result AS VARCHAR) || 
                ' WHERE testcaseId IN ( SELECT tt.testcaseId
                FROM ' || $$"tTestcases"$$ || ' tt WHERE tt.scenarioId in ( ' || scenarioIds || ' ) )';
        EXECUTE _sql;
    ELSE
        _sql :=  N'UPDATE ' || $$"tTestcases"$$ || ' SET result = ' || CAST(_result AS VARCHAR) || ' WHERE testcaseId IN ( ' || testcaseIds || ' )';
        EXECUTE _sql;

        -- the result from this query is never used
        _sql := N'SELECT _scenarioId = scenarioId FROM ' || $$"tTestcases"$$ || 'WHERE testcaseId in ( ' || testcaseIds || ' )';
		EXECUTE _sql;
    END IF;
    
END;
$func$ LANGUAGE plpgsql;

CREATE FUNCTION sp_add_scenario_metainfo(_testcaseId INTEGER, metaKey VARCHAR(50), metaValue VARCHAR(50), OUT rowsInserted INTEGER)
RETURNS INTEGER AS $$
DECLARE
    _scenarioId INTEGER;
BEGIN
     _scenarioId := (SELECT scenarioId FROM "tTestcases" WHERE testcaseId = _testcaseId);
	INSERT INTO "tScenarioMetainfo" (scenarioId, "name", "value") VALUES (_scenarioId, metaKey, metaValue);
	GET DIAGNOSTICS rowsInserted = ROW_COUNT;
END;
$$ LANGUAGE plpgsql;


CREATE FUNCTION sp_clear_scenario_metainfo(_testcaseId INTEGER, OUT rowsDeleted INTEGER)
RETURNS INTEGER AS $func$
DECLARE
    _scenarioId INTEGER;
BEGIN
    _scenarioId := (SELECT scenarioId FROM "tTestcases" WHERE testcaseId = _testcaseId);
    IF _scenarioId > 0 THEN
        DELETE FROM "tScenarioMetainfo" WHERE scenarioId = _scenarioId;
        GET DIAGNOSTICS rowsDeleted = ROW_COUNT;
    END IF;
END;
$func$ LANGUAGE plpgsql;



CREATE FUNCTION sp_get_scenarios_count(whereClause VARCHAR(1000))
RETURNS TABLE (
	scenariosCount bigint
) AS $func$
BEGIN
	RETURN QUERY EXECUTE
	'SELECT COUNT(DISTINCT(scenarioId)) AS scenariosCount FROM ' || $$"tTestcases"$$ || ' ' || whereClause;
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_get_scenarios(startRecord VARCHAR(100), recordsCount VARCHAR(100), whereClause VARCHAR(1000), 
                                 sortCol VARCHAR(100), sortType VARCHAR(100))
RETURNS TABLE (
    scenarioId integer,
    suiteId integer,
    name varchar(255),
    description varchar(4000),
    result integer,
    testcasesTotal integer,
    testcasesFailed integer,
    testcasesPassedPercent double precision,
    testcaseIsRunning integer,
    dateStart timestamp,
    dateEnd timestamp,
    duration integer,
    userNote varchar(255)
) AS $func$
DECLARE
    scenariosCursor refcursor;
    fetchStatus boolean DEFAULT true;
    
    testcasesTotal INTEGER;
    testcasesFailed INTEGER;
    testcasesSkipped INTEGER;
    testcaseIsRunning INTEGER;

    scenarioResult INTEGER;
    scenarioDateStart TIMESTAMP;
    scenarioDateEnd TIMESTAMP;
    scenarioDuration INTEGER;
    
    _scenarioId INTEGER;
    
    dates record;
    _currentScenarioDuration INTEGER DEFAULT 0;
    _testDuration INTEGER;
    _dateEnd TIMESTAMP;
    _dateStart TIMESTAMP;
    
BEGIN
    DROP TABLE IF EXISTS "tmpScenarios";
    CREATE TEMP TABLE IF NOT EXISTS "tmpScenarios" (
        scenarioId integer NOT NULL,
        suiteId integer NOT NULL,
        name varchar(255) NOT NULL,
        description varchar(4000) NULL,
        result integer NOT NULL,
        testcasesTotal integer NOT NULL,
        testcasesFailed integer NOT NULL,
        testcasesPassedPercent double precision NOT NULL,
        testcaseIsRunning integer NOT NULL,
        dateStart timestamp NOT NULL,
        dateEnd timestamp NULL,
        duration integer NOT NULL,
        userNote varchar(255) NULL
    );
    DROP TABLE IF EXISTS "tmpTestcases";
    CREATE TEMP TABLE IF NOT EXISTS "tmpTestcases" (
        testcaseId integer NOT NULL,
        scenarioId integer NOT NULL,
        suiteId integer NOT NULL,
        dateStart timestamp NOT NULL,
        dateEnd timestamp NULL,
        result integer NOT NULL
    );
    EXECUTE ('INSERT INTO '|| $$"tmpTestcases"$$ ||' 
              SELECT testcaseId, scenarioId, suiteId, dateStart, dateEnd, result FROM ' || $$"tTestcases"$$ || ' ' || whereClause);
    OPEN scenariosCursor FOR EXECUTE
    'SELECT tr.scenarioId FROM (
                SELECT distinct scenarioId, rank() OVER (ORDER BY scenarioId) AS Row
                FROM ' || $$"tmpTestcases"$$ || ' GROUP BY scenarioId ) as tr
            WHERE tr.Row >= ' || startRecord || ' AND tr.Row <= ' || recordsCount;
    WHILE fetchStatus LOOP
        FETCH NEXT FROM scenariosCursor INTO _scenarioId;
		fetchStatus := FOUND;
		IF fetchStatus THEN
		    -- calculate testcase info
            testcasesTotal     := (SELECT COUNT(testcaseId) FROM "tmpTestcases" WHERE "tmpTestcases".scenarioId = _scenarioId);
            testcasesFailed    := (SELECT COUNT(testcaseId) FROM "tmpTestcases" WHERE "tmpTestcases".scenarioId = _scenarioId AND "tmpTestcases".result = 0);
            testcasesSkipped   := (SELECT COUNT(testcaseId) FROM "tmpTestcases" WHERE "tmpTestcases".scenarioId = _scenarioId AND "tmpTestcases".result = 2);
            testcaseIsRunning  := (SELECT COUNT(testcaseId) FROM "tmpTestcases" WHERE "tmpTestcases".scenarioId = _scenarioId AND "tmpTestcases".result = 4);

            -- calculate scenario result
            scenarioResult := 1;
            IF testcaseIsRunning > 0 THEN
                scenarioResult := 4;
            ELSIF testcasesFailed > 0 THEN
                scenarioResult := 0;
            ELSIF testcasesSkipped > 0 THEN
                scenarioResult := 2;
            END IF;
            -- calculate scenario dates
            scenarioDateStart := (SELECT "tmpTestcases".dateStart FROM "tmpTestcases" WHERE "tmpTestcases".scenarioId = _scenarioId order by testcaseId ASC LIMIT 1);
            scenarioDateEnd   := (SELECT "tmpTestcases".dateEnd   FROM "tmpTestcases" WHERE "tmpTestcases".scenarioId = _scenarioId order by testcaseId DESC LIMIT 1);
            
            FOR dates IN (SELECT "tmpTestcases".dateStart, "tmpTestcases".dateEnd FROM "tmpTestcases" WHERE "tmpTestcases".scenarioId = _scenarioId) LOOP
				IF dates.dateEnd IS NULL THEN
					_testDuration := EXTRACT(EPOCH FROM now() at time zone 'utc' - dates.dateStart);
				ELSE
					_testDuration := EXTRACT(EPOCH FROM dates.dateEnd - dates.dateStart);
				END IF;
            END LOOP;
            
            INSERT INTO "tmpScenarios"
            SELECT  _scenarioId,
                    (SELECT "tmpTestcases".suiteId from "tmpTestcases" WHERE "tmpTestcases".scenarioId = _scenarioId LIMIT 1),
                    "tScenarios".name as name,
                    "tScenarios".description,
                    scenarioResult,

                    testcasesTotal,
                    testcasesFailed,
                    (testcasesTotal - testcasesFailed - testcasesSkipped) * 100.00 / CASE WHEN testcasesTotal=0 THEN 1 ELSE testcasesTotal END AS testcasesPassedPercent,
                    testcaseIsRunning,

                    scenarioDateStart,
                    scenarioDateEnd,

                    _currentScenarioDuration AS duration,
                    "tScenarios".userNote
            FROM    "tScenarios"
            WHERE   "tScenarios".scenarioId = _scenarioId;
            
		END IF;
    END LOOP;
    CLOSE scenariosCursor;
    RETURN QUERY EXECUTE
    'SELECT * FROM ' || $$"tmpScenarios"$$ || ' ORDER BY ' || sortCol || ' ' || sortType;
END;
$func$ LANGUAGE plpgsql;



CREATE FUNCTION sp_update_scenario(_scenarioId VARCHAR(50), _userNote VARCHAR(255),OUT rowsUpdated INTEGER)
RETURNS INTEGER as $$
BEGIN
    UPDATE "tScenarios" SET userNote = _userNote WHERE "tScenarios".scenarioId = _scenarioId::INTEGER;
    GET DIAGNOSTICS rowsUpdated = ROW_COUNT;
END;
$$ LANGUAGE plpgsql;


CREATE FUNCTION sp_delete_scenario(scenarioIds VARCHAR(1000),_suiteId VARCHAR(1000))
RETURNS VOID AS $func$
DECLARE
    -- the used delimiter
    _delimiter VARCHAR(10) DEFAULT ',';
    idIndex SMALLINT;
    idToken VARCHAR(100);
BEGIN
    WHILE scenarioIds <> '' LOOP
        idIndex := POSITION(_delimiter IN scenarioIds);
        IF idIndex > 0 THEN
            idToken := LEFT(scenarioIds, idIndex-1);
            scenarioIds := RIGHT(scenarioIds, LENGTH(scenarioIds)-idIndex);
        ELSE
            idToken := scenarioIds;
            scenarioIds := '';
        END IF;
        DELETE FROM "tTestcases" WHERE "tTestcases".scenarioId = CAST(idToken AS INTEGER) AND "tTestcases".suiteId = CAST(_suiteId AS INTEGER);
    END LOOP;
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION "getMachineId" (machine VARCHAR(255))
RETURNS INTEGER AS $$
DECLARE
    _machineId INTEGER;
BEGIN
    
    BEGIN
		_machineId := (SELECT machineId FROM "tMachines" WHERE machineName = machine);
		IF _machineId IS NULL THEN
			INSERT INTO "tMachines" (machineName) VALUES (machine);
			SELECT last_value INTO _machineId FROM "tMachines_machineid_seq";
		END IF;
    EXCEPTION WHEN unique_violation THEN
		_machineId := (SELECT machineId FROM "tMachines" WHERE machineName = machine);
    END;
    RETURN _machineId;
    
END;
$$ LANGUAGE plpgsql;


CREATE FUNCTION "getUniqueMessageId" (message VARCHAR(3950))
RETURNS integer AS $$
DECLARE
    _hash BYTEA;
    messageId INTEGER;
BEGIN
    -- when more than 1 load agent try to insert at the same time the same unique message
    -- they all see there is no an entry for this unique message, so they all try to insert
    -- the new message into tUniqueMessages, but only the first one succeed as there is
    -- unique constraint on this table

    -- so each thread tries until messageId IS NOT NULL
    
    LOOP
		_hash := md5(message);
		messageId := (SELECT um.uniqueMessageId FROM "tUniqueMessages" um WHERE um.hash = _hash);
		BEGIN
			IF messageId IS NOT NULL THEN
				RETURN messageId;
			END IF;
			INSERT INTO "tUniqueMessages" (hash, message) VALUES (_hash, message);
			SELECT last_value INTO messageId FROM "tUniqueMessages_uniquemessageid_seq";
		EXCEPTION
			WHEN unique_violation THEN
				messageId := NULL;
			WHEN foreign_key_violation THEN
				messageId := NULL;
		END;
    END LOOP;
    
END;
$$ LANGUAGE plpgsql;


CREATE FUNCTION sp_insert_message( _testcaseId INTEGER, _messageTypeId INTEGER , _message TEXT , 
                                  _escapeHtml boolean , _machine VARCHAR(255) , _threadName VARCHAR(255) 
                                  , _timestamp TIMESTAMP)
RETURNS VOID AS $$
DECLARE
    uniqueMessageId  INTEGER;
    messageChunk     VARCHAR(3952);
    parentMessageId  INTEGER;
    pos              INTEGER DEFAULT 1;
    chunkSize        INTEGER DEFAULT 3950;
    machineId        INTEGER;
BEGIN
    machineId := "getMachineId" (_machine);
    WHILE pos*2 <= LENGTH(_message) LOOP
        messageChunk := SUBSTRING(_message, pos, chunkSize);
        uniqueMessageId := "getUniqueMessageId"( messageChunk );
        -- insert the message
        -- VARCHAR uses 2 bytes per char, so we have to double 'chunkSize' in order to compare it with LENGTH
        IF LENGTH(_message) > chunkSize*2 AND pos = 1 THEN
			parentMessageId := (SELECT last_value FROM "tMessages_messageid_seq");
			parentMessageId := parentMessageId + 1;
            INSERT INTO "tMessages"
                (testcaseId, messageTypeId, timestamp, escapeHtml, uniqueMessageId, machineId, threadName , parentMessageId)
            VALUES
                (_testcaseId, _messageTypeId, _timestamp, _escapeHtml, uniqueMessageId, machineId, _threadName, parentMessageId);
        ELSE
            INSERT INTO "tMessages"
            (testcaseId, messageTypeId, timestamp, escapeHtml, uniqueMessageId, machineId, threadName , parentMessageId)
            VALUES
            (_testcaseId, _messageTypeId, _timestamp, _escapeHtml, uniqueMessageId, machineId, _threadName, parentMessageId);
        END IF;
        pos := pos + chunkSize;
    END LOOP;
END;
$$ LANGUAGE plpgsql;


CREATE FUNCTION sp_insert_run_message(_runId INTEGER, _messageTypeId INTEGER, _message TEXT, 
                                      _escapeHtml BOOLEAN, _machine VARCHAR(255), _threadName VARCHAR(255) ,_timestamp TIMESTAMP)
RETURNS VOID AS $$
DECLARE
    uniqueMessageId INTEGER;
    messageChunk    VARCHAR(3952);
    pos             INTEGER DEFAULT 1;
    chunkSize       INTEGER DEFAULT 3950;
    machineId       INTEGER;
BEGIN
    machineId := "getMachineId" (_machine);
    WHILE pos*2 <= LENGTH(_message) LOOP
        messageChunk := SUBSTRING(_message, pos, chunkSize);
        uniqueMessageId := "getUniqueMessageId"( messageChunk );
        
        -- insert the message
        INSERT INTO "tRunMessages"
        ( runId, messageTypeId, timestamp, escapeHtml, uniqueMessageId, machineId, threadName)
        VALUES
        ( _runId, _messageTypeId, _timestamp, _escapeHtml, uniqueMessageId, machineId, _threadName);
        pos := pos + chunkSize;
    END LOOP;
END;
$$ LANGUAGE plpgsql;


CREATE FUNCTION sp_insert_suite_message(_suiteId INTEGER, _messageTypeId INTEGER, _message TEXT, _escapeHtml BOOLEAN, _machine VARCHAR(255),
                                        _threadName VARCHAR(255), _timestamp TIMESTAMP)
RETURNS VOID AS $$
DECLARE
    uniqueMessageId INTEGER;
    messageChunk    VARCHAR(3952);
    pos             INTEGER DEFAULT 1;
    chunkSize       INTEGER DEFAULT 3950;
    machineId       INTEGER;
BEGIN
    machineId := "getMachineId" (_machine);
    WHILE pos*2 <= LENGTH(_message) LOOP
        messageChunk := SUBSTRING(_message, pos, chunkSize);
        uniqueMessageId := "getUniqueMessageId"( messageChunk );
        
        -- insert the message
        INSERT INTO "tSuiteMessages"
        ( suiteId, messageTypeId, timestamp, escapeHtml, uniqueMessageId, machineId, threadName)
        VALUES
        ( _suiteId, _messageTypeId, _timestamp, _escapeHtml, uniqueMessageId, machineId, _threadName);
        pos := pos + chunkSize;
    END LOOP;
END;
$$ LANGUAGE plpgsql; 


CREATE FUNCTION sp_get_run_messages(startRecord VARCHAR(100), recordsCount VARCHAR(100), whereClause VARCHAR(7000), sortCol VARCHAR(100), sortType VARCHAR(100))
RETURNS TABLE (
    runMessageId INTEGER,
    "timestamp" TIMESTAMP,
    threadName VARCHAR(255),
    machineName VARCHAR(255),
    message VARCHAR(3950),
    typeName VARCHAR(50),
    "row" BIGINT
) AS $func$
BEGIN
    RETURN QUERY EXECUTE 'SELECT * FROM (SELECT m.runmessageId, m.timestamp, m.threadName,
                                 CASE
                                     WHEN mach.machineAlias is NULL OR LENGTH(mach.machineAlias) = 0 THEN mach.machineName
                                     ELSE mach.machineAlias
                                 END as machineName,
                                 umsg.message as message,
                                 mt.name AS typeName,
                                 ROW_NUMBER() OVER (ORDER BY ' || sortCol || ' ' || sortType || ') AS row
                          FROM ' || $$"tRunMessages"$$ || ' m
                          LEFT JOIN ' || $$"tMessageTypes"$$ || ' mt ON m.messageTypeId = mt.messageTypeId
                          JOIN ' || $$"tUniqueMessages"$$ || ' umsg ON m.uniqueMessageId = umsg.uniqueMessageId
                          JOIN ' || $$"tMachines"$$ || ' mach ON m.machineId = mach.machineId
                          WHERE runMessageId IN (SELECT ' || $$"tRunMessages"$$ || '.runMessageId FROM ' || $$"tRunMessages"$$ || ' ' || whereClause || ')) as r
                          WHERE r.row >= ' || startRecord || ' AND r.row <= ' || recordsCount;
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_get_run_messages_count(whereClause VARCHAR(7000))
RETURNS TABLE (
    messagesCount INTEGER
) AS $func$
BEGIN
    RETURN QUERY EXECUTE 'SELECT CAST (count(*) AS INTEGER) as messagesCount FROM ' || $$"tRunMessages"$$ || ' m
                          LEFT JOIN ' || $$"tMessageTypes"$$ || ' mt  ON m.messageTypeId = mt.messageTypeId
                          JOIN ' ||    $$"tUniqueMessages"$$ || ' umsg   ON m.uniqueMessageId = umsg.uniqueMessageId
                          JOIN ' || $$"tMachines"$$ || ' mach  ON m.machineId = mach.machineId
                          WHERE runMessageId in( SELECT runMessageId FROM ' || $$"tRunMessages"$$ || '  ' || whereClause || ' )';
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_get_suite_messages(startRecord VARCHAR(100), recordsCount VARCHAR(100), 
                                      whereClause VARCHAR(7000), sortCol VARCHAR(100), sortType VARCHAR(100))
RETURNS TABLE (
    suiteMessageId INTEGER,
    "timestamp" TIMESTAMP,
    threadName VARCHAR(255),
    machineName VARCHAR(255),
    message VARCHAR(3950),
    typeName VARCHAR(50),
    "row" BIGINT
) AS $func$
BEGIN
   RETURN QUERY EXECUTE
   'SELECT * from (
                   SELECT m.suitemessageId,
                          m.timestamp,
                          m.threadName,
                          CASE
                              WHEN mach.machineAlias is NULL OR LENGTH(mach.machineAlias) = 0 THEN mach.machineName
                              ELSE mach.machineAlias
                          END as machineName,
                          umsg.message as message,
                          mt.name AS typeName,
                          ROW_NUMBER() OVER (ORDER BY ' || sortCol || ' ' || sortType || ') AS row
                          FROM ' || $$"tSuiteMessages"$$ || ' m
                              LEFT JOIN ' || $$"tMessageTypes"$$ || ' mt ON m.messageTypeId = mt.messageTypeId
                              JOIN ' || $$"tUniqueMessages"$$ || ' umsg ON m.uniqueMessageId = umsg.uniqueMessageId
                              JOIN ' || $$"tMachines"$$ || ' mach ON m.machineId = mach.machineId
                          WHERE suiteMessageId in(
                                SELECT suiteMessageId FROM ' || $$"tSuiteMessages"$$ ||'
                                ' || whereClause || ')) as r
            WHERE r.row >= ' || startRecord || ' AND r.row <= ' || recordsCount;
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_get_suite_messages_count(whereClause VARCHAR(7000))
RETURNS TABLE (
    messagesCount INTEGER
) AS $func$
BEGIN
    RETURN QUERY EXECUTE
    'SELECT CAST (count(*) AS INTEGER) as messagesCount 
     FROM ' || $$"tSuiteMessages"$$ || ' m
     LEFT JOIN ' || $$"tMessageTypes"$$ || ' mt  ON m.messageTypeId = mt.messageTypeId
     JOIN ' || $$"tUniqueMessages"$$ || ' umsg   ON m.uniqueMessageId = umsg.uniqueMessageId
     JOIN ' || $$"tMachines"$$ || ' mach  ON m.machineId = mach.machineId
     WHERE suiteMessageId in( SELECT suiteMessageId FROM ' || $$"tSuiteMessages"$$ ||' ' || whereClause || ' )';
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_get_messages_count(whereClause VARCHAR(7000))
RETURNS TABLE (
    messagesCount BIGINT
) AS $func$
BEGIN
	
	RETURN QUERY EXECUTE 'SELECT COUNT (DISTINCT 
                                               (
                                                 CASE WHEN m.parentMessageId IS NULL 
                                                     THEN m.messageId 
                                                     ELSE m.parentMessageId
                                                 END
                                               )
                                            ) as messagesCount 
                                FROM ' || $$"tMessages"$$ || ' m 
                                LEFT JOIN ' || $$"tMessageTypes"$$ || '   mt ON m.messageTypeId = mt.messageTypeId 
                                     JOIN ' || $$"tUniqueMessages"$$ || ' umsg ON m.uniqueMessageId = umsg.uniqueMessageId 
                                     JOIN ' || $$"tMachines"$$ || '       mach ON m.machineId = mach.machineId ' 
                                || whereClause ;
	
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_get_messages(startRecord VARCHAR(100), recordsCount VARCHAR(100), whereClause VARCHAR(7000), sortCol VARCHAR(100), sortType VARCHAR(100))
RETURNS TABLE (
    messageId       INTEGER,
    "timestamp"     TIMESTAMP,
    threadName      VARCHAR(250),
    machineName     VARCHAR(250),
    message         VARCHAR(3950),
    typeName        VARCHAR(10),
    parentMessageId INTEGER,
    "row"           INTEGER
) AS $func$
BEGIN
    DROP TABLE IF EXISTS "tmpMessages";
    CREATE TEMP TABLE IF NOT EXISTS "tmpMessages" (
            messageId       INTEGER,
            timestamp       TIMESTAMP,
            threadName      VARCHAR(250),
            machineName     VARCHAR(250),
            message         VARCHAR(3950),
            typeName        VARCHAR(10),
            parentMessageId INTEGER,
            row             INTEGER
    );
    
    EXECUTE ('INSERT INTO '|| $$"tmpMessages"$$
              || 'SELECT * FROM
                               ( SELECT m.messageId,
                                        m.timestamp,
                                        m.threadName,
                                        CASE WHEN mach.machineAlias is NULL OR LENGTH(mach.machineAlias) = 0 
                                            THEN mach.machineName
                                            ELSE mach.machineAlias
                                        END as machineName,
                                        umsg.message as message,
                                        mt.name AS typeName,
                                        m.parentMessageId,
                                        ROW_NUMBER() OVER (ORDER BY ' || sortCol || ' ' || sortType || ') AS row
                                  FROM ' || $$"tMessages"$$ || ' m
                                  LEFT JOIN ' || $$"tMessageTypes"$$ ||' mt ON m.messageTypeId = mt.messageTypeId
                                  JOIN ' || $$"tUniqueMessages"$$ || ' umsg ON m.uniqueMessageId = umsg.uniqueMessageId
                                  JOIN ' || $$"tMachines"$$ || ' mach ON m.machineId = mach.machineId
                                  WHERE m.messageId in ( 
                                          SELECT DISTINCT ( CASE WHEN m.parentMessageId IS NULL THEN m.messageId ELSE m.parentMessageId END ) 
                                          FROM ' || $$"tMessages"$$ || ' m
                                          LEFT JOIN ' || $$"tMessageTypes"$$ || ' mt ON m.messageTypeId = mt.messageTypeId
                                          JOIN ' || $$"tUniqueMessages"$$ || ' umsg ON m.uniqueMessageId = umsg.uniqueMessageId
                                          JOIN ' || $$"tMachines"$$ || ' mach ON m.machineId = mach.machineId ' ||
                                          whereClause || 
                                  ')
          ) as r
          WHERE r.row >= ' || startRecord || ' AND r.row <= ' || recordsCount);
          
          
          
    INSERT INTO "tmpMessages"
    SELECT m.messageId,
           m.timestamp,
           m.threadName,
           CASE
               WHEN mach.machineAlias is NULL OR LENGTH(mach.machineAlias) = 0 THEN mach.machineName
               ELSE mach.machineAlias
           END AS machineName,
           umsg.message AS message,
           mt.name AS typeName,
           m.parentMessageId,
           -1 AS row
    FROM "tMessages" m
    LEFT JOIN "tMessageTypes" mt ON m.messageTypeId = mt.messageTypeId
    JOIN "tUniqueMessages" umsg ON m.uniqueMessageId = umsg.uniqueMessageId
    JOIN "tMachines" mach ON m.machineId = mach.machineId
    WHERE ( m.parentMessageId > 0
            AND m.parentMessageId IN (SELECT tm.parentMessageId FROM "tmpMessages" tm)
            AND m.messageId NOT IN (SELECT tm.messageId FROM "tmpMessages" tm ) );
            
    -- Java code relies on that order
    RETURN QUERY
    SELECT * FROM "tmpMessages"
    ORDER BY
        CASE WHEN sortCol = 'timestamp' AND sortType = 'ASC' THEN "tmpMessages".timestamp END,
        CASE WHEN sortCol = 'timestamp' AND sortType = 'DESC' THEN "tmpMessages".timestamp END DESC,
        CASE WHEN sortCol = 'threadName' AND sortType = 'ASC' THEN "tmpMessages".threadName END,
        CASE WHEN sortCol = 'threadName' AND sortType = 'DESC' THEN "tmpMessages".threadName END DESC,
        CASE WHEN sortCol = 'machineName' AND sortType = 'ASC' THEN "tmpMessages".machineName END,
        CASE WHEN sortCol = 'machineName' AND sortType = 'DESC' THEN "tmpMessages".machineName END DESC,
        CASE WHEN sortCol = 'message' AND sortType = 'ASC' THEN "tmpMessages".message END,
        CASE WHEN sortCol = 'message' AND sortType = 'DESC' THEN "tmpMessages".message END DESC,
        "tmpMessages".messageId;
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION "getStatsTypeId" ( _parentName VARCHAR(255), _internalName VARCHAR(255), _statsName VARCHAR(255), 
                               _statsUnit VARCHAR(50), _statsParams TEXT)
RETURNS INTEGER AS $func$
DECLARE
    _statsTypeId INTEGER;
BEGIN
    _statsTypeId := (SELECT statsTypeId FROM "tStatsTypes" 
                    WHERE parentName = _parentName AND internalName = _internalName AND name = _statsName AND units = _statsUnit AND params = _statsParams);
    IF _statsTypeId IS NULL THEN
        INSERT INTO "tStatsTypes" (parentName, internalName, name, units, params) VALUES (_parentName, _internalName, _statsName, _statsUnit, _statsParams);
        SELECT last_value INTO _statsTypeId FROM "tStatsTypes_statstypeid_seq";
    END IF;
    
    RETURN _statsTypeId;
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_get_system_statistics(_fdate varchar(150), _testcaseIds varchar(150), _machineIds varchar(150), _statsTypeIds varchar(150), _whereClause text)
RETURNS TABLE (
    statsName VARCHAR(255),
    statsParent VARCHAR(255),
    statsUnit VARCHAR(50),
    value REAL,
    statsTypeId INTEGER,
    statsAxis VARCHAR(255),
    statsAxisTimestamp DOUBLE PRECISION,
    machineId INTEGER,
    testcaseId INTEGER
) AS $func$
DECLARE
    _sql VARCHAR(8000);
BEGIN
    -- timestamp conversion note: 20 means yyyy-mm-dd hh:mi:ss
    _sql := 'SELECT st.name AS statsName,
                    st.parentName AS statsParent,
                    st.units AS statsUnit,
                    ss.value,
                    st.statsTypeId,
                    CAST(ss.timestamp AS VARCHAR) AS statsAxis,
                    EXTRACT(EPOCH FROM (ss.timestamp - CAST(''' || _fdate || ''' AS TIMESTAMP))) AS statsAxisTimestamp,
                    ss.machineId,
                    ss.testcaseId

             FROM      ' || $$"tSystemStats"$$ || ' ss
             LEFT JOIN ' || $$"tStatsTypes"$$ || ' st ON (ss.statsTypeId = st.statsTypeId)
             JOIN      ' || $$"tTestcases"$$ ||' tt ON (tt.testcaseId = ss.testcaseId)
             WHERE ss.testcaseId IN ( ' || _testcaseIds || ' )
             AND ss.machineId IN ( ' || _machineIds || ' )
             AND st.statsTypeId IN ('|| _statsTypeIds || ')
              AND ' || _whereClause || '
              GROUP BY st.parentName, st.name, st.units, ss.timestamp, ss.value, st.statsTypeId, ss.machineId, ss.testcaseId
              ORDER BY ss.timestamp';
    RETURN QUERY EXECUTE _sql;
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_get_system_statistic_descriptions(_fdate varchar(150), whereClause varchar(1000))
RETURNS TABLE (
    testcaseId INTEGER,
    testcaseName VARCHAR(255),
    testcaseStarttime DOUBLE PRECISION,
    machineId INTEGER,
    machineName VARCHAR(255),
    statsTypeId INTEGER,
    name VARCHAR(255),
    params TEXT,
    parentName VARCHAR(255),
    internalName VARCHAR(255),
    units VARCHAR(255),
    statsNumberMeasurements BIGINT,
    statsMinValue Decimal(20,2),
    statsAvgValue Decimal(20,2),
    statsMaxValue Decimal(20,2)
) AS $func$
DECLARE
    _sql VARCHAR(8000);
BEGIN
    _sql := 'SELECT tt.testcaseId, 
                    tt.name as testcaseName,
                    EXTRACT(EPOCH FROM (MIN(ss.timestamp) - CAST( ''' || _fdate || ''' AS TIMESTAMP))) as testcaseStarttime,
                    m.machineId,
                    CASE
                        WHEN m.machineAlias is NULL OR LENGTH(m.machineAlias) = 0 THEN m.machineName
                        ELSE m.machineAlias
                    END as machineName,
                    ss.statsTypeId,
                    st.name,
                    st.params,
                    st.parentName,
                    st.internalName,
                    st.units,
                    COUNT(ss.value) as statsNumberMeasurements,
                    CAST( MIN(ss.value) AS Decimal(20,2) ) as statsMinValue,
                    CAST( AVG(ss.value) AS Decimal(20,2) ) as statsAvgValue,
                    CAST( MAX(ss.value) AS Decimal(20,2) ) as statsMaxValue
             FROM ' || $$"tSystemStats"$$ || ' ss
             INNER JOIN ' || $$"tStatsTypes"$$ || ' st on (ss.statsTypeId = st.statsTypeId)
             INNER JOIN ' || $$"tMachines"$$ || ' m on (ss.machineId = m.machineId)
             INNER JOIN ' || $$"tTestcases"$$ || ' tt on (ss.testcaseId = tt.testcaseId)
             ' || whereClause || '
             GROUP BY tt.testcaseId, tt.name, m.machineId, m.machineName, m.machineAlias, st.name, st.params, st.parentName, st.internalName, ss.statsTypeId, st.units
             ORDER BY st.name';
    RETURN QUERY EXECUTE _sql;
END;

$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_get_number_of_checkpoints_per_queue(testcaseIds VARCHAR(100))
RETURNS TABLE (
    name VARCHAR(255),
    numberOfQueue BIGINT
) AS $func$
DECLARE
    _sql VARCHAR(8000);
BEGIN
    _sql := 'SELECT "tLoadQueues".name,
             count("tLoadQueues".name) as numberOfQueue
             FROM ' || $$"tCheckpoints"$$ || '
             INNER JOIN ' || $$"tCheckpointsSummary"$$ || ' on ("tCheckpointsSummary".checkpointSummaryId = "tCheckpoints".checkpointSummaryId)
             INNER JOIN ' || $$"tLoadQueues"$$ || ' on ("tLoadQueues".loadQueueId = "tCheckpointsSummary".loadQueueId)
             WHERE "tLoadQueues".testcaseId in ( ' || testcaseIds || ' )
             GROUP BY "tLoadQueues".testcaseId, "tLoadQueues".name';
    RETURN QUERY EXECUTE _sql;
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_get_specific_testcase_id(_currentTestcaseId VARCHAR(30) , _runName VARCHAR(300), _suiteName VARCHAR(300), _scenarioName VARCHAR(300), _testName VARCHAR(500), _mode INTEGER)
RETURNS TABLE (
    testcaseId INTEGER
) AS $func$
DECLARE
    gtOrLt varchar(5);
    ascOrDesc varchar(5);
    _sql varchar(4000);
BEGIN
    -- mode = 1  ->  next testcase id
    -- mode = 2  ->  previous testcase id
    -- mode = 3  ->  last testcase id
    IF _mode = 1 THEN
        gtOrLt = '>';
        ascOrDesc = 'asc';
    ELSIF _mode = 2 THEN
        gtOrLt = '<';
        ascOrDesc = 'desc';
    ELSIF _mode = 3 THEN
        gtOrLt = '>';
        ascOrDesc = 'desc';
    END IF;
    
    _sql = 'SELECT tt.testcaseId FROM ' || $$"tTestcases"$$ || ' tt
    INNER JOIN ' || $$"tScenarios"$$ || ' tss on (tt.scenarioId = tss.scenarioId)
    INNER JOIN ' || $$"tSuites"$$ || ' ts on (ts.suiteId = tt.suiteId)
    INNER JOIN ' || $$"tRuns"$$ || ' tr on (ts.runId = tr.runId)
    WHERE tr.runName = ''' || _runName || ''' and ts.name = ''' || _suiteName || ''' and tss.name = ''' || _scenarioName
             || ''' and tt.name = ''' || _testName || ''' and tt.testcaseId ' || gtOrLt || ' ' || _currentTestcaseId || '
    order by tt.testcaseId ' || ascOrDesc || ' LIMIT 1';
    
    RETURN QUERY EXECUTE _sql;
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_get_loadqueues(whereClause VARCHAR(1000), sortCol VARCHAR(100), sortType VARCHAR(100))
RETURNS TABLE (
    loadQueueId INTEGER,
    name VARCHAR(255),
    sequence INTEGER,
    hostsList VARCHAR(255),
    threadingPattern VARCHAR(255),
    numberThreads INTEGER,
    machineId INTEGER,
    dateStart TIMESTAMP,
    dateEnd TIMESTAMP,
    duration DOUBLE PRECISION,
    result SMALLINT,
    userNote VARCHAR(255)
) AS $func$
DECLARE
    sql_1 VARCHAR(8000);
BEGIN
    sql_1 := 'SELECT loadQueueId,
                     name,
                     sequence,
                     hostsList,
                     threadingPattern,
                     numberThreads,
                     machineId,
                     dateStart,
                     dateEnd,
                     CASE WHEN dateEnd IS NULL
                         THEN EXTRACT(EPOCH FROM now() at time zone ''utc'' - dateStart )
                         ELSE EXTRACT(EPOCH FROM dateEnd - dateStart)
                     END AS duration,
                     result,
                     userNote
             FROM ' || $$"tLoadQueues"$$ || ' ' || whereClause || ' ORDER BY ' || sortCol || ' ' || sortType;
     
     RETURN QUERY EXECUTE sql_1;
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_start_loadqueue(_testcaseId INTEGER, _name VARCHAR(255), _sequence INTEGER, _hostsList VARCHAR(255),
                                   _threadingPattern VARCHAR(255), _numberThreads INTEGER, _machine VARCHAR(255), _dateStart TIMESTAMP, 
                                   OUT rowsInserted INTEGER, OUT _loadQueueId INTEGER)
RETURNS record AS $func$
DECLARE
    -- POSSIBLE LOAD QUEUE RESULT VALUES
    -- 0 FAILED
    -- 1 PASSED
    -- 2 CANCELLED
    -- 4 RUNNING

    -- The load queue is initially RUNNING
    _result INTEGER DEFAULT 4;

    _machineId INTEGER;
BEGIN
    _machineId := "getMachineId" (_machine);
    
    _loadQueueId = (SELECT loadQueueId FROM "tLoadQueues" 
                    WHERE "tLoadQueues".name = _name 
                    AND "tLoadQueues".sequence = _sequence 
                    AND "tLoadQueues".testcaseId = _testcaseId);
                    
    IF _loadQueueId IS NULL THEN
        -- start a new load queue
        INSERT INTO "tLoadQueues"
                    (testcaseId, name, sequence, hostsList, threadingPattern, numberThreads, machineId, dateStart, result)
        VALUES
                    (_testcaseId, _name, _sequence, _hostsList, _threadingPattern, _numberThreads, _machineId, _dateStart, _result);
        SELECT last_value INTO _loadQueueId FROM "tLoadQueues_loadqueueid_seq";
        GET DIAGNOSTICS rowsInserted = ROW_COUNT;
    ELSE
        -- this load queue is started again
        -- update the number of threads value, reset its result and end date as it is running again
        UPDATE "tLoadQueues"
        SET numberThreads = "tLoadQueues".numberThreads + _numberThreads,
            "tLoadQueues".dateEnd = NULL,
            "tLoadQueues".result = _result
        WHERE "tLoadQueues".name = _name AND "tLoadQueues".testcaseId = _testcaseId AND "tLoadQueues".sequence = _sequence;
    END IF;
    
    GET DIAGNOSTICS rowsInserted = ROW_COUNT;
    
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_end_loadqueue(_loadQueueId INTEGER, _result INTEGER, _dateEnd TIMESTAMP, OUT rowsUpdated INTEGER)
RETURNS INTEGER AS $func$
BEGIN
    -- end the loadqueue
    UPDATE "tLoadQueues"
    SET result = _result, 
        dateEnd = _dateEnd
    WHERE loadQueueId = _loadQueueId;
    GET DIAGNOSTICS rowsUpdated = ROW_COUNT;
END;
$func$ LANGUAGE plpgsql;



CREATE FUNCTION sp_get_checkpoints(perPage VARCHAR(100), rowsCount VARCHAR(100), whereClause VARCHAR(1000), sortCol VARCHAR(100), sortType VARCHAR(100))
RETURNS setof "tCheckpoints" AS $func$
DECLARE
    sortTypeRevert VARCHAR(10);
    _sql1 VARCHAR(8000);
    _sql2 VARCHAR(8000);
BEGIN
    sortTypeRevert := CASE WHEN sortType = 'asc' THEN 'desc' ELSE 'asc' END;
    _sql1 := 'SELECT * FROM ' || $$"tCheckpoints"$$ || ' WHERE ' || whereClause;
    _sql2 := 'SELECT *
              FROM  (' || _sql1 || ') AS cp
              WHERE  checkpointId IN (
                                      SELECT cp.checkpointId
                                      FROM (
                                            SELECT *
                                            FROM (' || _sql1 || ') as Row
                                            ORDER BY ' || sortCol || ' ' || sortType || '
                                            LIMIT ' || rowsCount || '
                                      ) cp 
                                      ORDER BY ' || sortCol || ' ' || sortTypeRevert || 
                                      ' LIMIT '|| perPage || '
                                    )
              ORDER BY ' || sortCol || ' ' || sortType;
    RETURN QUERY EXECUTE _sql2;
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_get_checkpoints_summary(whereClause VARCHAR(500), sortCol VARCHAR(100), sortType VARCHAR(100))
RETURNS setof "tCheckpointsSummary" AS $func$
DECLARE
    sql_1 VARCHAR(8000);
BEGIN
    sql_1 := 'SELECT * FROM ' || $$"tCheckpointsSummary"$$ || ' ' || whereClause || ' ORDER BY ' || sortCol || ' ' || sortType;
    RETURN QUERY EXECUTE sql_1;
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_get_checkpoint_statistics(fdate varchar(150), testcaseIds varchar(150), checkpointNames varchar(1000), parentNames varchar(1000))
RETURNS TABLE (
    statsTypeId BIGINT,
    queueName VARCHAR(255),
    statsName VARCHAR(150),
    value INTEGER,
    statsAxisTimestamp DOUBLE PRECISION,
    testcaseId INTEGER
) AS $func$
DECLARE
    _sql VARCHAR(8000);
BEGIN
    _sql := 'SELECT
                   ch.checkpointId as statsTypeId,
                   c.name as queueName,
                   ch.name as statsName,
                   ch.responseTime as value,
                   EXTRACT(EPOCH FROM (ch.endTime - CAST( ''' || fdate || ''' AS TIMESTAMP))) as statsAxisTimestamp,
                   tt.testcaseId
             FROM ' || $$"tCheckpoints"$$ || ' ch
             INNER JOIN ' || $$"tCheckpointsSummary"$$ || ' chs on (chs.checkpointSummaryId = ch.checkpointSummaryId)
             INNER JOIN ' || $$"tLoadQueues"$$ || ' c on (c.loadQueueId = chs.loadQueueId)
             INNER JOIN ' || $$"tTestcases"$$ || ' tt on (tt.testcaseId = c.testcaseId)
             WHERE tt.testcaseId in ( ' || testcaseIds || ' ) AND ch.name in ( ' || checkpointNames || ' ) AND  c.name in ( ' || parentNames ||' ) AND ch.result = 1 AND ch.endTime IS NOT NULL
             ORDER BY ch.endTime';
    RETURN QUERY EXECUTE _sql;
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_get_checkpoint_statistic_descriptions(fdate varchar(150), whereClause varchar(1000))
RETURNS TABLE (
    testcaseId INTEGER,
    testcaseName VARCHAR(255),
    testcaseStarttime DOUBLE PRECISION,
    queueName VARCHAR(255),
    name VARCHAR(150),
    statsNumberMeasurements BIGINT,
    statsMinValue INTEGER,
    statsMaxValue INTEGER,
    statsAvgValue REAL
) AS $func$
DECLARE
    _sql VARCHAR(8000);
BEGIN
    _sql := 'SELECT tt.testcaseId,
                    tt.name as testcaseName,
                    EXTRACT(EPOCH FROM tt.dateStart - (CAST( ''' || fdate || ''' AS TIMESTAMP))) as testcaseStarttime,
                    c.name as queueName,
                    chs.name as name,
                    sum(chs.numPassed + chs.numFailed) as statsNumberMeasurements,
                    chs.minResponseTime as statsMinValue,
                    chs.maxResponseTime as statsMaxValue,
                    chs.avgResponseTime as statsAvgValue
             FROM ' || $$"tCheckpointsSummary"$$ || ' chs
             INNER JOIN ' || $$"tLoadQueues"$$ || ' c on (c.loadQueueId = chs.loadQueueId)
             INNER JOIN ' || $$"tTestcases"$$ || ' tt on (tt.testcaseId = c.testcaseId)
             ' || whereClause || '
             GROUP BY tt.testcaseId, 
                      tt.dateStart, 
                      tt.name, 
                      c.name, 
                      chs.name, 
                      chs.minResponseTime, 
                      chs.maxResponseTime, 
                      chs.avgResponseTime        
            ORDER BY chs.name';
    RETURN QUERY EXECUTE _sql;
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_populate_system_statistic_definition(parentName VARCHAR(255), internalName VARCHAR(255), _name VARCHAR(255),
                                                        unit VARCHAR(50), params TEXT, OUT statisticId INTEGER)
RETURNS INTEGER AS $func$
DECLARE
BEGIN
    statisticId := "getStatsTypeId" (parentName, internalName, _name, unit, params);
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_update_machine_info(_machine VARCHAR(255), _machineInfo TEXT, OUT rowsUpdated INTEGER)
RETURNS INTEGER AS $func$
DECLARE
    _machineId INTEGER;
BEGIN
    -- get machine's id
    _machineId := "getMachineId" (_machine);
    -- update the machine description
    UPDATE "tMachines" SET machineInfo = _machineInfo WHERE machineId = _machineId;
    GET DIAGNOSTICS rowsUpdated = ROW_COUNT;
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_insert_system_statistic_by_ids(_testcaseId INTEGER, _machine VARCHAR(255), _statisticIds VARCHAR(1000), _statisticValues VARCHAR(8000), _timestamp TIMESTAMP)
RETURNS VOID AS $func$
DECLARE
    _machineId INTEGER;
    _delimiter VARCHAR(10) DEFAULT '_';                         -- the used delimiter
    _testcaseIdString VARCHAR(10) DEFAULT _testcaseId::VARCHAR; -- testcase id to a string
    _machineIdString VARCHAR(10);                               -- machine id to a string
    _timestampString VARCHAR(30) DEFAULT _timestamp::VARCHAR;   -- timestamp to a string
    _idIndex SMALLINT;
    _idToken VARCHAR(100);
    _valueIndex SMALLINT;
    _valueToken VARCHAR(100);
    _sql VARCHAR(8000);
BEGIN
    -- get machine's id
    _machineId := "getMachineId" (_machine);
    _machineIdString := (CAST (_machineId AS VARCHAR(10)));
    
    -- statisticIds is a string with the statistics ids in the tStatsTypes table, delimited with '_'
    -- statisticValues is a string with the statistics values, delimited with '_'

    -- we iterate all the statistic ids and values and add them to the tSystemStats table
    -- we know that statisticIds and statisticValues have the same number of subelements
    WHILE _statisticIds <> '' AND _statisticValues <> '' LOOP
        _idIndex := POSITION(_delimiter IN _statisticIds);
        _valueIndex := POSITION(_delimiter IN _statisticValues);
        
        IF _idIndex > 0 THEN
             _idToken := LEFT(_statisticIds, _idIndex-1);
             _statisticIds := RIGHT(_statisticIds, LENGTH(_statisticIds)-_idIndex);

             _valueToken := LEFT(_statisticValues, _valueIndex-1);
             _statisticValues := RIGHT(_statisticValues, LENGTH(_statisticValues)-_valueIndex);
        ELSE
            _idToken = _statisticIds;
            _statisticIds = '';

            _valueToken = _statisticValues;
            _statisticValues = '';
        END IF;
        
        _sql := 'INSERT INTO ' || $$"tSystemStats"$$ || ' (testcaseId, machineId, statsTypeId, timestamp, value)
                 VALUES (' ||
                        _testcaseIdString || ',' ||
                        _machineIdString || ',' ||
                        _idToken || ', ' ||
                        '''' || _timestampString || '''::TIMESTAMP' || ', ''' ||
                        _valueToken || ''''
                        ')';
        EXECUTE _sql;
    END LOOP;
    -- TODO we should return the number of inserted statistics and check this number in the calling java code
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_insert_user_activity_statistic_by_ids(_testcaseId INTEGER, _machine VARCHAR(255), _statisticIds VARCHAR(1000), 
                                                         _statisticValues VARCHAR(8000), _timestamp TIMESTAMP)
RETURNS VOID AS $func$
DECLARE
    _machineId INTEGER;
    _delimiter VARCHAR(10) DEFAULT '_'; -- the used delimiter
    
    performUpdate INTEGER;
    
    _idIndex SMALLINT;
    _idToken VARCHAR(100);

    _valueIndex SMALLINT;
    _valueToken VARCHAR(100);

    _sql VARCHAR(8000);
    
    _timestampString VARCHAR(30);
BEGIN
    _timestampString := (SELECT to_char(_timestamp, 'yyyy-mm-dd hh24:mi:ss'));
    -- get machine's id
	_machineId := "getMachineId" (_machine);
    
    -- When the test steps are run from more than 1 agent loader, we need to synchronize them here as we want to have
    -- just 1 loader for 1 action queue. This way all checkpoints info from agent instances is merged in a single place
    
    
    -- _statisticIds is a string with the statistics ids in the tStatsTypes table, delimited with '_'
    -- _statisticValues is a string with the statistics values, delimited with '_'

    -- we iterate all the statistic ids and values and add them to the tSystemStats table
    -- we know that _statisticIds and _statisticValues have the same number of subelements
    
    WHILE _statisticIds <> '' AND _statisticValues <> '' LOOP
        _idIndex := POSITION(_delimiter IN _statisticIds);
        _valueIndex := POSITION(_delimiter IN _statisticValues);
        
        IF _idIndex > 0 THEN
            _idToken := LEFT(_statisticIds, _idIndex-1);
            _statisticIds := RIGHT(_statisticIds, LENGTH(_statisticIds)-_idIndex);

            _valueToken := LEFT(_statisticValues, _valueIndex-1);
            _statisticValues := RIGHT(_statisticValues, LENGTH(_statisticValues)-_valueIndex);
        ELSE
            _idToken := _statisticIds;
            _statisticIds := '';

            _valueToken := _statisticValues;
            _statisticValues := '';
        END IF;
        
        -- see if there are already logged user activity statistics for the current testcase and timestamp
        SELECT value INTO performUpdate 
        FROM  "tSystemStats" 
        WHERE "tSystemStats".testcaseId = _testcaseId 
        AND   "tSystemStats".statsTypeId = _idToken::INTEGER 
        AND   to_char("tSystemStats".timestamp, 'yyyy-mm-dd hh24:mi:ss') = _timestampString;
        
		IF performUpdate IS NOT NULL THEN
			_sql := 'UPDATE ' || $$"tSystemStats"$$ || ' SET value = value + ' || _valueToken::REAL || ' WHERE statsTypeId = ' || _idToken::INTEGER || ' AND timestamp = ''' || _timestampString || '''';
			EXECUTE _sql;
		ELSE
			BEGIN
			_sql := 'INSERT INTO ' || $$"tSystemStats"$$ || '(testcaseId, machineId, statsTypeId, timestamp, value)
					VALUES(' || _testcaseId || ', ' || _machineId || ', ' || _idToken::INTEGER  || ', ''' || _timestampString::TIMESTAMP || ''', ' || _valueToken::REAL ||')';
			EXECUTE _sql;
			EXCEPTION WHEN unique_violation THEN
				_sql := 'UPDATE ' || $$"tSystemStats"$$ || ' SET value = value + ' || _valueToken::REAL || ' WHERE statsTypeId = ' || _idToken::INTEGER || ' AND timestamp = ''' || _timestampString || '''';
				EXECUTE _sql;
			END;
		END IF;
		performUpdate = NULL;
    END LOOP;
	
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_db_cleanup()
RETURNS VOID AS $func$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
BEGIN
    start_time := now();
    RAISE NOTICE 'START DELETING RUN MESSAGES: %', start_time::VARCHAR;
    DELETE FROM "tRunMessages" WHERE runId NOT IN (SELECT runId FROM "tRuns");
    end_time := now();
    RAISE NOTICE 'END DELETING RUN MESSAGES: %', end_time::VARCHAR;
    RAISE NOTICE 'EXECUTED FOR TIME IN MILLISECONDS: %', cast(DATE_PART('MILLISECONDS',end_time - start_time) as varchar(100));

    start_time := now();
    RAISE NOTICE 'START DELETING SUITE MESSAGES: %', start_time::VARCHAR;
    DELETE FROM "tSuiteMessages" WHERE suiteId NOT IN (SELECT suiteId FROM "tSuites");
    end_time := now();
    RAISE NOTICE  'END DELETING SUITE MESSAGES: %', end_time::VARCHAR;
    RAISE NOTICE 'EXECUTED FOR TIME IN MILLISECONDS: %', cast(DATE_PART('MILLISECONDS',end_time - start_time) as varchar(100));

    -- When deleting tRunMessages, tSuiteMessages and tMessages we cannot not use AUTO DELETE for tUniqueMessages,
    -- because it is possible to have same message(identified by its uniqueMessageId) in more than one place in 
    -- one or more of these parent tables
    start_time := now();
    RAISE NOTICE 'START DELETING UNIQUE MESSAGES: %', start_time::VARCHAR;
    DELETE FROM "tUniqueMessages" WHERE "tUniqueMessages".uniqueMessageId NOT IN 
  	(
	 SELECT uniqueMessageId FROM "tMessages" 
     UNION SELECT uniqueMessageId FROM "tRunMessages"
     UNION SELECT uniqueMessageId FROM "tSuiteMessages"
    );
    end_time := now();
    RAISE NOTICE 'END DELETING UNIQUE MESSAGES: %', cast(end_time as varchar(20));
    RAISE NOTICE 'EXECUTED FOR TIME IN MILLISECONDS: %', cast(DATE_PART('MILLISECONDS',end_time - start_time)as varchar(100));
  
    start_time := now();
    RAISE NOTICE 'START DELETING SCENARIOS: %', cast(start_time as varchar(20));
    DELETE FROM "tScenarios" WHERE scenarioId NOT IN (SELECT scenarioId FROM "tTestcases");
    end_time := now();
    RAISE NOTICE 'END DELETING SCENARIOS: %', end_time::VARCHAR;
    RAISE NOTICE 'EXECUTED FOR TIME IN MILLISECONDS: %', cast(DATE_PART('MILLISECONDS',end_time - start_time) as varchar(100));
END;
$func$ LANGUAGE plpgsql;

CREATE FUNCTION sp_populate_checkpoint_summary(_loadQueueId INTEGER, _name VARCHAR(255), 
                                                     _transferRateUnit VARCHAR(50), OUT _checkpointSummaryId INTEGER)
RETURNS INTEGER AS $func$
BEGIN
	INSERT INTO "tCheckpointsSummary"
                    ( loadQueueId,  name, numRunning, numPassed, numFailed, minResponseTime, maxResponseTime, avgResponseTime, minTransferRate, maxTransferRate, avgTransferRate, transferRateUnit)
    VALUES
                    -- insert the max possible int value for minResponseTime, so on the first End Checkpoint event we will get a real min value
                    -- insert the same value for maxTransferRate which is float
                    (_loadQueueId, _name, 0, 0, 0, 2147483647, 0, 0, 2147483647, 0, 0, _transferRateUnit);

    SELECT last_value INTO _checkpointSummaryId FROM "tCheckpointsSummary_checkpointsummaryid_seq";
END;
$func$ LANGUAGE plpgsql;


CREATE FUNCTION sp_start_checkpoint(_loadQueueId INTEGER, _name VARCHAR(255), _mode INTEGER, _transferRateUnit VARCHAR(50), OUT _checkpointSummaryId INTEGER, OUT _checkpointId BIGINT)
RETURNS RECORD AS $func$
DECLARE
	-- 0 - FAILED
    -- 1 - PASSED
    -- 4 - RUNNING
	_result INTEGER DEFAULT 4;
BEGIN
	-- get an ID which must be already present
    _checkpointSummaryId := (SELECT  checkpointSummaryId
                             FROM    "tCheckpointsSummary"
                             WHERE   loadQueueId = _loadQueueId AND name = _name);

    -- SUMMARY table - it keeps 1 row for ALL values of a checkpoint
    IF _checkpointSummaryId IS NOT NULL THEN
		 -- update existing entry
         UPDATE "tCheckpointsSummary"
         SET    numRunning = numRunning + 1
         WHERE  checkpointSummaryId = _checkpointSummaryId;

		-- insert in DETAILS table when running FULL mode - it keeps 1 row for EACH value of a checkpoint
		--   @mode == 0 -> SHORT mode
		--   @mode != 0 -> FULL mode
		IF _mode != 0 THEN
			INSERT INTO "tCheckpoints"
				(checkpointSummaryId, name, responseTime, transferRate, transferRateUnit, result, endTime)
			VALUES
				(_checkpointSummaryId, _name, 0, 0, _transferRateUnit, _result, null);

			SELECT last_value INTO _checkpointId FROM "tCheckpoints_checkpointid_seq";
		END IF;
	ELSE
		-- checkpoint summary id was not found
		_checkpointSummaryId := 0;
		_checkpointId := 0;
    END IF;
END;
$func$ LANGUAGE plpgsql;

CREATE FUNCTION sp_insert_checkpoint(_loadQueueId INTEGER, _name VARCHAR(150), _responseTime BIGINT, _endTime TIMESTAMP,
                                     _transferSize BIGINT, _transferRateUnit VARCHAR(50), _result INTEGER, _mode INTEGER)
RETURNS VOID AS $func$
DECLARE
	_checkpointSummaryId INTEGER DEFAULT 0;
	_checkpointId BIGINT DEFAULT 0;
	_transferRate REAL;
BEGIN
	
	IF _result = 0 THEN
		_responseTime := 0;
        _transferSize := 0;
    END IF;
    
    IF _responseTime > 0 THEN
        -- in order to transform transferSize into float we multiply with 1000.0 instead of 1000
        _transferRate := _transferSize*1000.0/_responseTime;
    ELSE 
		_transferRate := 0;
    END IF;
    
    -- get an ID which must be already present
    _checkpointSummaryId := (SELECT  checkpointSummaryId
                             FROM    "tCheckpointsSummary"
                             WHERE   loadQueueId = _loadQueueId AND name = _name);
    
    -- SUMMARY table - it keeps 1 row for ALL values of a checkpoint
    IF _checkpointSummaryId IS NOT NULL THEN
		 -- update existing entry
		 IF _result = 0 THEN
			 -- checkpoint failed
			 UPDATE  "tCheckpointsSummary"
             SET     numFailed = numFailed + 1
             WHERE   checkpointSummaryId = _checkpointSummaryId;
		 ELSE
			-- checkpoint passed
			UPDATE "tCheckpointsSummary"
            SET     numPassed = numPassed + 1,
                    minResponseTime = CASE WHEN _responseTime < minResponseTime THEN _responseTime ELSE minResponseTime END,
                    maxResponseTime = CASE WHEN _responseTime > maxResponseTime THEN _responseTime ELSE maxResponseTime END,
                    avgResponseTime = (avgResponseTime * numPassed + _responseTime)/(numPassed + 1),
                    minTransferRate = CASE WHEN _transferRate < minTransferRate THEN _transferRate ELSE minTransferRate END,
                    maxTransferRate = CASE WHEN _transferRate > maxTransferRate THEN _transferRate ELSE maxTransferRate END,
                    avgTransferRate = (avgTransferRate * numPassed + _transferRate)/(numPassed+ 1)
            WHERE checkpointSummaryId = _checkpointSummaryId;
		 END IF;
		 
		 -- insert in DETAILS table when running FULL mode - it keeps 1 row for EACH value of a checkpoint
		 --   @mode == 0 -> SHORT mode
		 --   @mode != 0 -> FULL mode
		 IF _mode != 0 THEN
             INSERT INTO "tCheckpoints"
            (checkpointSummaryId, name, responseTime, transferRate, transferRateUnit, result, endTime)
            VALUES
            (_checkpointSummaryId, _name, _responseTime, _transferRate, _transferRateUnit, _result, _endTime);

            SELECT last_value INTO _checkpointId FROM "tCheckpoints_checkpointid_seq";
         END IF;
         
    ELSE
		-- checkpoint summary id was not found
		_checkpointSummaryId := 0;
		_checkpointId := 0;
    END IF;
    
END;
$func$ LANGUAGE plpgsql;

CREATE FUNCTION sp_end_checkpoint(_checkpointSummaryId INTEGER, _checkpointId BIGINT, _responseTime INTEGER, _transferSize BIGINT, _result INTEGER, _mode INTEGER, 
								  _endTime TIMESTAMP, OUT rowsInserted INTEGER)
RETURNS INTEGER AS $func$
DECLARE
	_transferRate REAL;
BEGIN

	IF _responseTime > 0 THEN
		-- in order to transform transferSize into float we multiply with 1000.0 instead of 1000
		_transferRate := _transferSize*1000.0/_responseTime;
	ELSE 
		_transferRate := 0;
	END IF;
	
	-- update DETAILS table when running FULL mode - it keeps 1 row for EACH value of a checkpoint
	--  @mode == 0 -> SHORT mode
	--  @mode != 0 -> FULL mode
	IF _mode != 0 THEN
		UPDATE "tCheckpoints"
        SET    responseTime=_responseTime, transferRate=_transferRate, result=_result, endTime=_endTime
        WHERE  checkpointId = _checkpointId;
	END IF;
	
	-- update SUMMARY table - it keeps 1 row for ALL values of a checkpoint
	IF _result = 0 THEN
		-- checkpoint failed
		UPDATE  "tCheckpointsSummary"
        SET     numRunning = numRunning -1,
                numFailed = numFailed + 1
        WHERE   checkpointSummaryId = _checkpointSummaryId;
	ELSE
		 -- checkpoint passed
		 UPDATE "tCheckpointsSummary"
         SET    numRunning = numRunning -1,
                numPassed = numPassed + 1,
                minResponseTime = CASE WHEN _responseTime < minResponseTime THEN _responseTime ELSE minResponseTime END,
                maxResponseTime = CASE WHEN _responseTime > maxResponseTime THEN _responseTime ELSE maxResponseTime END,
                avgResponseTime = (avgResponseTime * numPassed + _responseTime)/(numPassed + 1),
                minTransferRate = CASE WHEN _transferRate < minTransferRate THEN _transferRate ELSE minTransferRate END,
                maxTransferRate = CASE WHEN _transferRate > maxTransferRate THEN _transferRate ELSE maxTransferRate END,
                avgTransferRate = (avgTransferRate * numPassed + _transferRate)/(numPassed+ 1)
         WHERE checkpointSummaryId = _checkpointSummaryId;
	END IF;
	
	GET DIAGNOSTICS rowsInserted = ROW_COUNT;
	
END;
$func$ LANGUAGE plpgsql;

CREATE FUNCTION sp_update_testcase( _testcaseId INTEGER, _suiteFullName VARCHAR(255), _scenarioName VARCHAR(255), 
                                      _scenarioDescription VARCHAR(4000), _testcaseName VARCHAR(255), _userNote VARCHAR(255), 
                                      _testcaseResult INTEGER, _timestamp timestamp, OUT rowsUpdated INTEGER )
RETURNS INTEGER AS $func$
DECLARE
    currentDateEnd TIMESTAMP;
    currentTestcaseName VARCHAR(255);
    
    currentScenarioName VARCHAR(255);
    currentFullName VARCHAR(255);
    currentScenarioDescription VARCHAR(4000);
    
    scenariosWithThisFullNameCount INTEGER DEFAULT 0;
    -- the id of the scenario, that is already inserted and has the same full name as the one, provided as a fuction argument
    alreadyInsertedScenarioId INTEGER DEFAULT -1;
    -- save the current testcase' scenario ID
    currentTestcaseScenarioId INTEGER;
    currentTestcaseResult INTEGER;
BEGIN

    currentDateEnd := (SELECT dateEnd FROM "tTestcases" WHERE testcaseId = _testcaseId);
    currentTestcaseName := (SELECT name FROM "tTestcases" WHERE testcaseId = _testcaseId);
    currentTestcaseResult := (SELECT result FROM "tTestcases" WHERE testcaseId = _testcaseId);
    
    currentScenarioName := (SELECT name FROM "tScenarios" WHERE scenarioId = (SELECT scenarioId FROM "tTestcases" WHERE testcaseId = _testcaseId));
    currentFullName := (SELECT fullName FROM "tScenarios" WHERE scenarioId = (SELECT scenarioId FROM "tTestcases" WHERE testcaseId = _testcaseId));
    currentScenarioDescription := (SELECT description FROM "tScenarios" WHERE scenarioId = (SELECT scenarioId FROM "tTestcases" WHERE testcaseId = _testcaseId));

    scenariosWithThisFullNameCount := (SELECT COUNT(*) FROM "tScenarios" WHERE fullName = (_suiteFullName || '@' || _scenarioName));
    IF scenariosWithThisFullNameCount > 0 THEN
        -- Since there already has scenario with fullName that is equal to the one, that this function will set,
        -- simply change the current testcase's scenarioId to the already existing one and delete the unnecessary scenario (indicated by the currrent testcase ID)
        currentTestcaseScenarioId := (SELECT scenarioId FROM "tTestcases" WHERE testcaseId = _testcaseId);
        alreadyInsertedScenarioId := (SELECT scenarioId FROM "tScenarios" WHERE fullName = (_suiteFullName || '@' || _scenarioName));
        UPDATE "tTestcases" 
        SET scenarioId = (SELECT scenarioId FROM "tScenarios" WHERE fullName = (_suiteFullName || '@' || _scenarioName)) 
        WHERE testcaseId = _testcaseId;
        DELETE FROM "tScenarios" WHERE scenarioId = currentTestcaseScenarioId;
    END IF;
    
    UPDATE "tTestcases" SET name = CASE
                                       WHEN _testcaseName IS NULL THEN currentTestcaseName
                                       ELSE _testcaseName
                                   END,
                            dateEnd = CASE
                                          WHEN _timestamp IS NULL THEN currentDateEnd
                                          ELSE _timestamp
                                      END,
                            result = CASE
                                         WHEN _testcaseResult = -1 THEN currentTestcaseResult
                                         ELSE _testcaseResult
                                     END,
                            userNote = _userNote
    WHERE testcaseId = _testcaseId;
    
    UPDATE "tScenarios" SET name = CASE
                                       WHEN _scenarioName IS NULL THEN currentScenarioName
                                       ELSE _scenarioName
                                   END, 
                            description = CASE
                                              WHEN _scenarioDescription IS NULL THEN currentScenarioDescription
                                              ELSE _scenarioDescription
                                          END,
                            fullName = CASE 
                                          WHEN (_suiteFullName IS NULL OR _scenarioName IS NULL) AND alreadyInsertedScenarioId = -1
                                          THEN currentFullName
                                          ELSE _suiteFullName || '@' || _scenarioName
                                          END  
    WHERE scenarioId = (SELECT scenarioId FROM "tTestcases" WHERE testcaseId = _testcaseId);
    
    GET DIAGNOSTICS rowsUpdated = ROW_COUNT;
    
END
$func$ LANGUAGE plpgsql;


/* Record the version w/o _draft as complete installation */
UPDATE "tInternal" SET value ='4.0.7' WHERE key = 'version';
