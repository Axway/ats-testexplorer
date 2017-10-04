\set ON_ERROR_STOP on

/* Record the internal version */
DO language plpgsql $$
BEGIN
  RAISE WARNING '-- update internalVersion in "tInternal" to 7';
END
$$;

INSERT INTO "tInternal" (key, value) VALUES ('Upgrade_to_intVer_7', NOW());

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

UPDATE "tInternal" SET value='7' WHERE key='internalVersion';
