\set ON_ERROR_STOP on

DO language plpgsql $$
BEGIN
  RAISE WARNING '#7 INTERNAL VERSION UPGRADE HEADER - START';
END
$$;
INSERT INTO "tInternal" (key, value) VALUES ('Upgrade_to_intVer_7', NOW());
DO language plpgsql $$
BEGIN
  RAISE WARNING '#7 INTERNAL VERSION UPGRADE HEADER - END';
END
$$;

DO language plpgsql $$
BEGIN
  RAISE WARNING 'START ALTER procedure sp_insert_user_activity_statistic_by_ids';
END
$$;

CREATE OR REPLACE FUNCTION sp_insert_user_activity_statistic_by_ids(_testcaseId INTEGER, _machine VARCHAR(255), _statisticIds VARCHAR(1000), 
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

DO language plpgsql $$
BEGIN
  RAISE WARNING 'END ALTER procedure sp_insert_user_activity_statistic_by_ids';
END
$$;

DO language plpgsql $$
BEGIN
  RAISE WARNING '#7 INTERNAL VERSION UPGRADE FOOTER - START';
END
$$;
UPDATE "tInternal" SET value='7' WHERE key='internalVersion';
DO language plpgsql $$
BEGIN
  RAISE WARNING '#7 INTERNAL VERSION UPGRADE FOOTER - END';
END
$$;

DO language plpgsql $$
BEGIN
  RAISE WARNING '#8 INTERNAL VERSION UPGRADE HEADER - START';
END
$$;
INSERT INTO "tInternal" (key, value) VALUES ('Upgrade_to_intVer_8', NOW());
DO language plpgsql $$
BEGIN
  RAISE WARNING '#7 INTERNAL VERSION UPGRADE HEADER - END';
END
$$;

DO language plpgsql $$
BEGIN
  RAISE WARNING 'START CREATE OR REPLACE procedure sp_update_testcase';
END
$$;

CREATE OR REPLACE FUNCTION sp_update_testcase( _testcaseId INTEGER, _suiteFullName VARCHAR(255), _scenarioName VARCHAR(255), 
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

DO language plpgsql $$
BEGIN
  RAISE WARNING 'END CREATE OR REPLACE procedure sp_update_testcase';
END
$$;

DO language plpgsql $$
BEGIN
  RAISE WARNING '#8 INTERNAL VERSION UPGRADE FOOTER - START';
END
$$;
UPDATE "tInternal" SET value='8' WHERE key='internalVersion';
DO language plpgsql $$
BEGIN
  RAISE WARNING '#8 INTERNAL VERSION UPGRADE FOOTER - END';
END
$$;


DO language plpgsql $$
BEGIN
  RAISE WARNING '#9 INTERNAL VERSION UPGRADE HEADER - START';
END
$$;
INSERT INTO "tInternal" (key, value) VALUES ('Upgrade_to_intVer_9', NOW());
DO language plpgsql $$
BEGIN
  RAISE WARNING '#9 INTERNAL VERSION UPGRADE HEADER - END';
END
$$;

DO language plpgsql $$
BEGIN
  RAISE WARNING 'START CREATE OR REPLACE procedure sp_get_navigation_for_testcases';
END
$$;

CREATE OR REPLACE FUNCTION sp_get_navigation_for_testcases(_suiteId varchar(30))
RETURNS TABLE (
    runId INTEGER,
    runName VARCHAR(50),
    scenarioId INTEGER,
    suiteName VARCHAR(50),
    scenarioName VARCHAR(50)
) AS $func$
BEGIN
    RETURN QUERY
    SELECT tRuns.runId, 
           tRuns.runName, 
           tScenarios.scenarioId, 
           tSuites.name AS suiteName, 
           tScenarios.name AS scenarioName
    FROM "tTestcases"
    INNER JOIN "tScenarios" ON (tScenarios.scenarioId = tTestcases.scenarioId)
    INNER JOIN "tSuites"  ON (tSuites.suiteId = tTestcases.suiteId)
    INNER JOIN "tRuns"  ON (tSuites.runId = tRuns.runId)
    WHERE tTestcases.suiteId = CAST(_suiteId AS INTEGER);
END;
$func$ LANGUAGE plpgsql;

DO language plpgsql $$
BEGIN
  RAISE WARNING 'END CREATE OR REPLACE procedure sp_get_navigation_for_testcases';
END
$$;


DO language plpgsql $$
BEGIN
  RAISE WARNING '#10 INTERNAL VERSION UPGRADE HEADER - START';
END
$$;
INSERT INTO "tInternal" (key, value) VALUES ('Upgrade_to_intVer_10', NOW());
DO language plpgsql $$
BEGIN
  RAISE WARNING '#10 INTERNAL VERSION UPGRADE HEADER - END';
END
$$;

DO language plpgsql $$
BEGIN
  RAISE WARNING 'START CREATE OR REPLACE procedure sp_get_number_of_checkpoints_per_queue';
END
$$;


CREATE OR REPLACE FUNCTION sp_get_number_of_checkpoints_per_queue(testcaseIds VARCHAR(100))
RETURNS TABLE (
    name VARCHAR(255),
    queue_number BIGINT
) AS $func$
DECLARE
    _sql VARCHAR(8000);
BEGIN
    _sql := 'SELECT "tLoadQueues".name,
             count("tLoadQueues".name) as queue_number
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
  RAISE WARNING 'END CREATE OR REPLACE procedure sp_get_number_of_checkpoints_per_queue';
END
$$;


DO language plpgsql $$
BEGIN
  RAISE WARNING 'START CREATE OR REPLACE procedure sp_get_system_statistic_descriptions';
END
$$;


CREATE OR REPLACE FUNCTION sp_get_system_statistic_descriptions(_fdate varchar(150), whereClause varchar(1000))
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
                    EXTRACT(EPOCH FROM (ss.timestamp - CAST( ''' || _fdate || ''' AS TIMESTAMP))) as testcaseStarttime,
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
             ' || whereClause || ' AND ss.timestamp in ( SELECT MIN(ss.timestamp)
																	from "tSystemStats" ss
																	' || whereClause || '
																	GROUP BY ss.testcaseId)
             GROUP BY tt.testcaseId, ss.timestamp, tt.name, m.machineId, m.machineName, m.machineAlias, st.name, st.params, st.parentName, st.internalName, ss.statsTypeId, st.units
             ORDER BY st.name';
    RETURN QUERY EXECUTE _sql;
END;
$func$ LANGUAGE plpgsql;


DO language plpgsql $$
BEGIN
  RAISE WARNING 'END CREATE OR REPLACE procedure sp_get_system_statistic_descriptions';
END
$$;


DO language plpgsql $$
BEGIN
  RAISE WARNING 'START CREATE OR REPLACE procedure sp_get_navigation_for_testcases';
END
$$;

CREATE OR REPLACE FUNCTION sp_get_navigation_for_testcases(_suiteId varchar(30))
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

DO language plpgsql $$
BEGIN
  RAISE WARNING 'END CREATE OR REPLACE procedure sp_get_navigation_for_testcases';
END
$$

