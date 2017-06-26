-- ***********************************************
-- Script for upgrade from version 4.0.1 to 4.0.2
-- ***********************************************

print '-- UNCOMMENT BEFORE RELEASE update internalVersion in [dbo].[tInternal]'
-- GO

-- UPDATE [dbo].[tInternal] SET [value]='7' WHERE [key]='internalVersion'
-- GO
print 'start alter sp_insert_user_activity_statistic_by_ids '
GO
/****** Object:  StoredProcedure [dbo].[sp_insert_user_activity_statistic_by_ids]  ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE    PROCEDURE [dbo].[sp_insert_user_activity_statistic_by_ids]

@testcaseId INT,
@machine VARCHAR(255),
@statisticIds VARCHAR(1000),
@statisticValues VARCHAR(8000),
@timestamp DATETIME


AS

-- get machine's id
DECLARE 
@machineId INT,
-- we make a SELECT COUNT(*) and save the result in @performUpdate
-- if we already have a result for reading with name 'ATS AGENTS' and the same testcase and timestamp
-- this means that a test with multiple agents is logging user activity and one of the agents,
-- already logged its user activity data
-- in that case, all other agents must NOT insert new data, but update the already existing one
-- in SELECT COUNT(*) returns NULL (no data), we insert the user activity data
@performUpdate INT
EXECUTE [dbo].[getMachineId] @machine, @machineId OUTPUT

-- prepare some constants
DECLARE @delimiter VARCHAR(10) ='_', -- the used delimiter
@testcaseIdString VARCHAR(10) = CONVERT(VARCHAR(10),@testcaseId), -- testcase id to a string
@machineIdString VARCHAR(10) = CONVERT(VARCHAR(10),@machineId), -- machine id to a string
@timestampString VARCHAR(30) = CONVERT(VARCHAR(30),@timestamp,20) -- timestamp to a string

-- When the test steps are run from more than 1 agent loader, we need to synchronize them here as we want to have
-- just 1 loader for 1 action queue. This way all checkpoints info from agent instances is merged in a single place

BEGIN TRAN StartInsrtUsrActvtSttTransaction
DECLARE @get_app_lock_res INT
EXEC @get_app_lock_res = sp_getapplock @Resource = 'StartInsrtUsrActvtSttTransaction Lock ID', @LockMode = 'Exclusive';

IF @get_app_lock_res < 0
    -- error getting lock
    -- client will see there was an error as @RowsInserted stays 0
    RETURN;

-- statisticIds is a string with the statistics ids in the tStatsTypes table, delimited with '_'
-- statisticValues is a string with the statistics values, delimited with '_'

-- we iterate all the statistic ids and values and add them to the tSystemStats table
-- we know that @statisticIds and @statisticValues have the same number of subelements
BEGIN
    DECLARE
        @idIndex SMALLINT,
        @idToken VARCHAR(100),

        @valueIndex SMALLINT,
        @valueToken VARCHAR(100),

        @sql VARCHAR(8000)

    WHILE @statisticIds <> ''  AND @statisticValues <> ''
    BEGIN
        SET @idIndex = CHARINDEX(@delimiter, @statisticIds)
        SET @valueIndex = CHARINDEX(@delimiter, @statisticValues)

        IF @idIndex>0
            BEGIN
                SET @idToken = LEFT(@statisticIds, @idIndex-1)
                SET @statisticIds = RIGHT(@statisticIds, LEN(@statisticIds)-@idIndex)

                SET @valueToken = LEFT(@statisticValues, @valueIndex-1)
                SET @statisticValues = RIGHT(@statisticValues, LEN(@statisticValues)-@valueIndex)
            END
        ELSE
            BEGIN
                SET @idToken = @statisticIds
                SET @statisticIds = ''

                SET @valueToken = @statisticValues
                SET @statisticValues = ''
            END
        -- see if there are already logged user activity statistics for the current testcase and timestamp
        SELECT @performUpdate = value 
        FROM tSystemStats 
        WHERE testcaseId = @testcaseId
        AND statsTypeId = @idToken
        AND timestamp = @timestampString
		IF @performUpdate > 0
		    BEGIN
			    SET @sql = 'UPDATE tSystemStats SET value = value + ' + @valueToken +
					       'WHERE statsTypeId = ' + @idToken +
				 	       ' AND timestamp = ' + '''' + @timestampString + '''' + ''
				EXEC(@sql)
			END
		ELSE
			BEGIN
				SET @sql = 'INSERT INTO tSystemStats VALUES (' +
						   @testcaseIdString + ',' +
						   @machineIdString + ',' +
						   @idToken + ', ' +
                           'CONVERT(DATETIME,''' + @timestampString + ''',20), ' +
                           @valueToken +
                           ')'
				EXEC(@sql)
            END
    END
END

IF @@ERROR <> 0 --error has happened
    ROLLBACK
ELSE
    COMMIT
GO

print 'end alter sp_insert_user_activity_statistic_by_ids '
GO

print 'start alter sp_get_system_statistic_descriptions '
GO

/****** Object:  StoredProcedure [dbo].[sp_get_system_statistic_descriptions]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
ALTER                 PROCEDURE [dbo].[sp_get_system_statistic_descriptions]

@fdate varchar(150),
@testcaseIds varchar(100)

AS

DECLARE @sql varchar(8000)

SET     @sql = 'SELECT  tt.testcaseId, tt.name as testcaseName,
                        DATEDIFF(second, CONVERT( datetime, ''' + @fdate + ''', 20), tt.dateStart) as testcaseStarttime,
                        m.machineId,
                        CASE
                            WHEN m.machineAlias is NULL OR DATALENGTH(m.machineAlias) = 0 THEN m.machineName
                            ELSE m.machineAlias
                        END as machineName,
                        ss.statsTypeId, st.name, st.params, st.parentName, st.internalName, st.units,
                        COUNT(ss.value) as statsNumberMeasurements,
                        CAST( MIN(ss.value) AS Decimal(20,2) ) as statsMinValue,
                        CAST( AVG(ss.value) AS Decimal(20,2) ) as statsAvgValue,
                        CAST( MAX(ss.value) AS Decimal(20,2) ) as statsMaxValue
                     FROM tSystemStats ss
                     INNER JOIN tStatsTypes st on (ss.statsTypeId = st.statsTypeId)
                     INNER JOIN tMachines m on (ss.machineId = m.machineId)
                     INNER JOIN tTestcases tt on (ss.testcaseId = tt.testcaseId)
                WHERE ss.testcaseId in (' + @testcaseIds + ')
                GROUP BY tt.testcaseId, tt.dateStart, tt.name, m.machineId, m.machineName, m.machineAlias, st.name, st.params, st.parentName, st.internalName, ss.statsTypeId, st.units
                ORDER BY st.name';

EXEC (@sql)
GO

print 'end alter sp_get_system_statistic_descriptions '
GO

print 'start alter sp_get_checkpoint_statistics '
GO

/****** Object:  StoredProcedure [dbo].[sp_get_checkpoint_statistics]    Script Date: 06/28/2011 16:05:37 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
ALTER   PROCEDURE [dbo].[sp_get_checkpoint_statistics]

@fdate varchar(150),
@testcaseIds varchar(150),
@checkpointNames varchar(1000)

AS

DECLARE @sql varchar(8000)
SET @sql = '
SELECT
    ch.checkpointId as statsTypeId,
    c.name as queueName,
    ch.name as statsName,
    ch.responseTime as value,
    DATEDIFF(second, CONVERT( datetime, ''' + @fdate + ''', 20), ch.endTime) as statsAxisTimestamp,
    c.name as queueName,
    tt.testcaseId
     FROM tCheckpoints ch
     INNER JOIN tCheckpointsSummary chs on (chs.checkpointSummaryId = ch.checkpointSummaryId)
     INNER JOIN tLoadQueues c on (c.loadQueueId = chs.loadQueueId)
     INNER JOIN tTestcases tt on (tt.testcaseId = c.testcaseId)
WHERE tt.testcaseId in ( '+@testcaseIds+' ) AND ch.name in ( '+@checkpointNames+' ) AND ch.result = 1 AND ch.endTime IS NOT NULL
ORDER BY ch.endTime';

EXEC (@sql)
GO

print 'end alter sp_get_checkpoint_statistics '
GO

print 'start alter sp_get_checkpoint_statistic_descriptions '
GO

/****** Object:  StoredProcedure [dbo].[sp_get_checkpoint_statistic_descriptions]    Script Date: 06/27/2011 10:19:48 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
ALTER                PROCEDURE [dbo].[sp_get_checkpoint_statistic_descriptions]

@fdate varchar(150),
@testcaseIds varchar(100)

AS

DECLARE @sql varchar(8000)
SET  @sql =
        'SELECT  tt.testcaseId, tt.name as testcaseName,
        DATEDIFF(second, CONVERT( datetime, ''' + @fdate + ''', 20), tt.dateStart) as testcaseStarttime,
        c.name as queueName, chs.name as name,
        sum(chs.numPassed + chs.numFailed) as statsNumberMeasurements
             FROM tCheckpointsSummary chs
             INNER JOIN tLoadQueues c on (c.loadQueueId = chs.loadQueueId)
             INNER JOIN tTestcases tt on (tt.testcaseId = c.testcaseId)
        WHERE tt.testcaseId in (' + @testcaseIds + ')
        GROUP BY tt.testcaseId, tt.dateStart, tt.name, c.name, chs.name
        ORDER BY chs.name';

EXEC (@sql)
GO

print 'end alter sp_get_checkpoint_statistic_descriptions '
GO

