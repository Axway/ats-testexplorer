-- ***********************************************
-- Script for upgrade from version 4.0.6 to 4.0.7.
-- ***********************************************

\set ON_ERROR_STOP on

DO language plpgsql $$
BEGIN
  RAISE WARNING '#19 INTERNAL VERSION UPGRADE HEADER - START';
END
$$;
INSERT INTO "tInternal" (key, value) VALUES ('Upgrade_to_intVer_19', NOW());
DO language plpgsql $$
BEGIN
  RAISE WARNING '#19 INTERNAL VERSION UPGRADE HEADER - END';
END
$$;

DO language plpgsql $$
BEGIN
  RAISE WARNING 'start ALTER TABLE "tCheckpoints"';
END
$$;
-- TODO
ALTER TABLE "tCheckpoints"
	ALTER COLUMN checkpointid SET DATA TYPE bigint;
ALTER SEQUENCE "tCheckpoints_checkpointid_seq" as bigint;
DO language plpgsql $$
BEGIN
  RAISE WARNING 'end ALTER TABLE "tCheckpoints"';
END
$$;

DO language plpgsql $$
BEGIN
  RAISE WARNING 'start CREATE OR REPLACE FUNCTION sp_start_checkpoint';
END
$$;

DROP FUNCTION IF EXISTS sp_start_checkpoint(integer,character varying,integer,character varying);

CREATE OR REPLACE FUNCTION sp_start_checkpoint(_loadQueueId INTEGER, _name VARCHAR(255), _mode INTEGER, _transferRateUnit VARCHAR(50), OUT _checkpointSummaryId INTEGER, OUT _checkpointId BIGINT)
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
DO language plpgsql $$
BEGIN
  RAISE WARNING 'end CREATE OR REPLACE FUNCTION sp_start_checkpoint';
END
$$;

DO language plpgsql $$
BEGIN
  RAISE WARNING 'start CREATE OR REPLACE FUNCTION sp_insert_checkpoint';
END
$$;
CREATE OR REPLACE FUNCTION sp_insert_checkpoint(_loadQueueId INTEGER, _name VARCHAR(150), _responseTime BIGINT, _endTime TIMESTAMP,
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
DO language plpgsql $$
BEGIN
  RAISE WARNING 'end CREATE OR REPLACE FUNCTION sp_insert_checkpoint';
END
$$;

DO language plpgsql $$
BEGIN
  RAISE WARNING 'start CREATE OR REPLACE FUNCTION sp_end_checkpoint';
END
$$;
CREATE OR REPLACE FUNCTION sp_end_checkpoint(_checkpointSummaryId INTEGER, _checkpointId BIGINT, _responseTime INTEGER, _transferSize BIGINT, _result INTEGER, _mode INTEGER, 
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

DO language plpgsql $$
BEGIN
  RAISE WARNING 'end CREATE OR REPLACE FUNCTION sp_end_checkpoint';
END
$$;


DO language plpgsql $$
BEGIN
  RAISE WARNING 'start CREATE OR REPLACE FUNCTION sp_get_system_statistics';
END
$$;

CREATE OR REPLACE FUNCTION sp_get_system_statistics(_fdate varchar(150), _testcaseIds varchar(150), _machineIds varchar(150), _statsTypeIds varchar(150), _whereClause text)
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


DO language plpgsql $$
BEGIN
  RAISE WARNING 'end CREATE OR REPLACE FUNCTION sp_get_system_statistics';
END
$$;


DO language plpgsql $$
BEGIN
  RAISE WARNING '#19 INTERNAL VERSION UPGRADE FOOTER - START';
END
$$;

UPDATE "tInternal" SET value='19' WHERE key='internalVersion';
DO language plpgsql $$
BEGIN
  RAISE WARNING '#19 INTERNAL VERSION UPGRADE FOOTER - END';
END
$$;
