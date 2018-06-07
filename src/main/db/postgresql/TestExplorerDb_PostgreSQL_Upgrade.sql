-- ***********************************************
-- Script for upgrade from version 4.0.5 to 4.1.0.
-- ***********************************************

\set ON_ERROR_STOP on

DO language plpgsql $$
BEGIN
  RAISE WARNING '#14 INTERNAL VERSION UPGRADE HEADER - START';
END
$$;
INSERT INTO "tInternal" (key, value) VALUES ('Upgrade_to_intVer_14', NOW());
DO language plpgsql $$
BEGIN
  RAISE WARNING '#14 INTERNAL VERSION UPGRADE HEADER - END';
END
$$;

DO language plpgsql $$
BEGIN
  RAISE WARNING 'start CREATE OR REPLACE function sp_get_testcases';
END
$$;

CREATE OR REPLACE FUNCTION sp_get_testcases(startRecord VARCHAR(100), recordsCount VARCHAR(100), whereClause VARCHAR(1000), sortCol VARCHAR(100), sortType VARCHAR(100))
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

DO language plpgsql $$
BEGIN
  RAISE WARNING 'end CREATE OR REPLACE function sp_get_testcases';
END
$$;


DO language plpgsql $$
BEGIN
  RAISE WARNING 'start CREATE OR REPLACE function sp_get_number_of_checkpoints_per_queue';
END
$$;


CREATE OR REPLACE FUNCTION sp_get_number_of_checkpoints_per_queue(testcaseIds VARCHAR(100))
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



DO language plpgsql $$
BEGIN
  RAISE WARNING 'end CREATE OR REPLACE function sp_get_number_of_checkpoints_per_queue';
END
$$;


DO language plpgsql $$
BEGIN
  RAISE WARNING '#14 INTERNAL VERSION UPGRADE FOOTER - START';
END
$$;
UPDATE "tInternal" SET value='14' WHERE key='internalVersion';
DO language plpgsql $$
BEGIN
  RAISE WARNING '#14 INTERNAL VERSION UPGRADE FOOTER - END';
END
$$;


DO language plpgsql $$
BEGIN
  RAISE WARNING '#15 INTERNAL VERSION UPGRADE HEADER - START';
END
$$;
INSERT INTO "tInternal" (key, value) VALUES ('Upgrade_to_intVer_15', NOW());
DO language plpgsql $$
BEGIN
  RAISE WARNING '#15 INTERNAL VERSION UPGRADE HEADER - END';
END
$$;


DO language plpgsql $$
BEGIN
  RAISE WARNING 'start CREATE OR REPLACE function sp_insert_message';
END
$$;


CREATE OR REPLACE FUNCTION sp_insert_message( _testcaseId INTEGER, _messageTypeId INTEGER , _message TEXT , 
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


DO language plpgsql $$
BEGIN
  RAISE WARNING 'end CREATE OR REPLACE function sp_insert_message';
END
$$;


DO language plpgsql $$
BEGIN
  RAISE WARNING '#15 INTERNAL VERSION UPGRADE FOOTER - START';
END
$$;
UPDATE "tInternal" SET value='15' WHERE key='internalVersion';
DO language plpgsql $$
BEGIN
  RAISE WARNING '#15 INTERNAL VERSION UPGRADE FOOTER - END';
END
$$;
