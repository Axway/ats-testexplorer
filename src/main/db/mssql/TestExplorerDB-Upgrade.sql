-- ***********************************************
-- Script for upgrade from version 4.0.0 to 4.0.2.
-- There is no 4.0.1 DB version
-- ***********************************************

/****** Record the internal version ******/
print '-- update internalVersion in [dbo].[tInternal]'
GO
INSERT INTO tInternal ([key], value) VALUES ('Upgrade_to_intVer_7', SYSDATETIME());
GO

-- updates for internal version 7

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
		IF @performUpdate IS NOT NULL
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
        SET @performUpdate = null
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
@WhereClause varchar(1000)

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
                ' + @WhereClause + '
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
@checkpointNames varchar(1000),
@parentNames varchar(1000)

AS

DECLARE @sql varchar(8000)
SET @sql = '
SELECT
    ch.checkpointId as statsTypeId,
    c.name as queueName,
    ch.name as statsName,
    ch.responseTime as value,
    DATEDIFF(second, CONVERT( datetime, ''' + @fdate + ''', 20), ch.endTime) as statsAxisTimestamp,
    tt.testcaseId
     FROM tCheckpoints ch
     INNER JOIN tCheckpointsSummary chs on (chs.checkpointSummaryId = ch.checkpointSummaryId)
     INNER JOIN tLoadQueues c on (c.loadQueueId = chs.loadQueueId)
     INNER JOIN tTestcases tt on (tt.testcaseId = c.testcaseId)
WHERE tt.testcaseId in ( '+@testcaseIds+' ) AND ch.name in ( '+@checkpointNames+' ) AND  c.name in ( '+@parentNames+' ) AND ch.result = 1 AND ch.endTime IS NOT NULL
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
@WhereClause varchar(1000)

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
        ' + @WhereClause + '
        GROUP BY tt.testcaseId, tt.dateStart, tt.name, c.name, chs.name
        ORDER BY chs.name';

EXEC (@sql)
GO

print 'end alter sp_get_checkpoint_statistic_descriptions '
GO

print 'start alter sp_get_system_statistics '
GO

/****** Object:  StoredProcedure [dbo].[sp_get_system_statistics]    Script Date: 06/27/2017 11:22:34 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
ALTER                PROCEDURE [dbo].[sp_get_system_statistics]

@fdate varchar(150),
@testcaseIds varchar(150),
@machineIds varchar(500),
@statsTypeIds varchar(150)

AS

DECLARE @sql varchar(8000)
SET     @sql = 'SELECT  st.name as statsName,
                        st.parentName as statsParent,
                        st.units as statsUnit,
                        ss.value,
                        st.statsTypeId,
                        convert(varchar,ss.timestamp,20) as statsAxis,
                        DATEDIFF(second, CONVERT( datetime, ''' + @fdate + ''', 20), ss.timestamp) as statsAxisTimestamp,
                        ss.machineId,
                        ss.testcaseId

                     FROM       tSystemStats ss
                     LEFT JOIN tStatsTypes st ON (ss.statsTypeId = st.statsTypeId)
                     JOIN    tTestcases tt ON (tt.testcaseId = ss.testcaseId)
             WHERE       ss.testcaseId in ( '+@testcaseIds+' )
                         AND ss.machineId in ( '+@machineIds+' )
                         AND st.statsTypeId IN ('+@statsTypeIds+')
             GROUP BY    st.parentName, st.name, st.units, ss.timestamp, ss.value, st.statsTypeId, ss.machineId, ss.testcaseId
             ORDER BY    ss.timestamp'

EXEC (@sql)
GO

print 'end alter sp_get_system_statistics '
GO

print ' start alter sp_delete_suite'
GO

/****** Object:  StoredProcedure [dbo].[sp_delete_suite]    Script Date: 09/26/2016 16:31:55 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
ALTER      PROCEDURE [dbo].[sp_delete_suite]

@suiteIds VARCHAR(1000)

AS

DECLARE @delimiter VARCHAR(10) =',' -- the used delimiter

BEGIN
    DECLARE
        @idIndex SMALLINT,
        @idToken VARCHAR(100)

    WHILE @suiteIds <> ''
    BEGIN
        SET @idIndex = CHARINDEX(@delimiter, @suiteIds)

        IF @idIndex > 0
            BEGIN
                SET @idToken = LEFT(@suiteIds, @idIndex-1)
                SET @suiteIds = RIGHT(@suiteIds, LEN(@suiteIds)-@idIndex)
            END
        ELSE
            BEGIN
                SET @idToken = @suiteIds
                SET @suiteIds = ''
            END

    DELETE FROM tSuiteMessages WHERE tSuiteMessages.suiteId IN (SELECT suiteId FROM tSuites WHERE tSuites.suiteId=@idToken);
    DELETE FROM tSuites WHERE tSuites.suiteId = @idToken;

    END
END
GO

print ' end alter sp_delete_suite'
GO

print ' start rename all machine names from "HTF Controllers" to "ATS Agents"'
GO

UPDATE tMachines
SET machineName='ATS Agents'
WHERE machineName='HTF Controllers'

print ' end rename all machine names from "HTF Controllers" to "ATS Agents"'
GO

print 'start alter procedure sp_update_suite '
GO

/****** Object:  StoredProcedure [dbo].[sp_update_suite]     ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
ALTER PROCEDURE [dbo].[sp_update_suite]

@suiteId INT,
@suiteName VARCHAR(50),
@userNote VARCHAR(255),

@RowsUpdated INT =0 OUT

AS

DECLARE
@currentSuiteName VARCHAR(50),
@currentUserNote VARCHAR(255)

-- get the current values
SELECT  @currentSuiteName = name,
        @currentUserNote  = userNote
FROM    tSuites
WHERE   suiteId = @suiteId

-- update the Suite info, if the provided value is null, then use the current value
UPDATE tSuites
SET    name = CASE
                       WHEN @suiteName IS NULL THEN @currentSuiteName
                       ELSE @suiteName
                   END,
       userNote =  CASE
                       WHEN @userNote IS NULL THEN @currentUserNote
                       ELSE @userNote
                   END
WHERE    tSuites.suiteId=@suiteId

SET @RowsUpdated = @@ROWCOUNT
GO

print 'end alter procedure sp_update_suite '
GO

print 'start alter sp-get_checkpoint_statistic_description'
GO

/****** Object:  StoredProcedure [dbo].[sp_get_checkpoint_statistic_descriptions]    Script Date: 06/27/2011 10:19:48 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
ALTER                PROCEDURE [dbo].[sp_get_checkpoint_statistic_descriptions]

@fdate varchar(150),
@WhereClause varchar(1000)

AS

DECLARE @sql varchar(8000)
SET  @sql =
        'SELECT  tt.testcaseId, tt.name as testcaseName,
        DATEDIFF(second, CONVERT( datetime, ''' + @fdate + ''', 20), tt.dateStart) as testcaseStarttime,
        c.name as queueName, chs.name as name,
        sum(chs.numPassed + chs.numFailed) as statsNumberMeasurements,
        chs.minResponseTime as statsMinValue,
        chs.maxResponseTime as statsMaxValue,
        chs.avgResponseTime as statsAvgValue
             FROM tCheckpointsSummary chs
             INNER JOIN tLoadQueues c on (c.loadQueueId = chs.loadQueueId)
             INNER JOIN tTestcases tt on (tt.testcaseId = c.testcaseId)
        ' + @WhereClause + '
        GROUP BY tt.testcaseId, tt.dateStart, tt.name, c.name, chs.name, 
        chs.minResponseTime, chs.maxResponseTime, chs.avgResponseTime        
        ORDER BY chs.name';

EXEC (@sql)
GO

print 'end alter sp-get_checkpoint_statistic_description'
GO

IF (@@ERROR != 0)
BEGIN
    RAISERROR(N'Error occured while performing update to internal version 7', 16, 1) WITH LOG;
    RETURN;
END

UPDATE [dbo].[tInternal] SET [value]='7' WHERE [key]='internalVersion'
GO

/****** Record the internal version ******/
print '-- update internalVersion in [dbo].[tInternal]'
GO
INSERT INTO tInternal ([key], value) VALUES ('Upgrade_to_intVer_8', SYSDATETIME());
GO

-- updates for internal version 8

print 'start alter sp_start_testcase'
GO

ALTER PROCEDURE [dbo].[sp_start_testcase]

@suiteId INT
,@suiteFullName VARCHAR(255)
,@scenarioName VARCHAR(255)
,@scenarioDescription VARCHAR(4000)
,@testcaseName VARCHAR(255)
,@dateStart DATETIME

,@RowsInserted INT =0 OUT
,@tcId INT =0 OUT

AS

DECLARE
@dateStartActual DATETIME
EXECUTE [dbo].[getAutoDate]    @dateStart ,@dateStartActual  OUTPUT

DECLARE @scenarioId INT
DECLARE @result INT

-- POSSIBLE TEST CASE RESULT VALUES
-- 0 FAILED
-- 1 PASSED
-- 2 SKIPPED
-- 4 RUNNING
-- The test is RUNNING IN THE BEGGINNING
SET @result = 4

-- empty end time for the current Suite
UPDATE  tSuites SET dateEnd = null WHERE  suiteId = @suiteId

-- Construct full name. The format is <java package>.<java class>.<name>
-- in case of data driven tests, the <name> contains java method name and values of input arguments: <java method name>(arg1, arg2)
DECLARE @scenarioFullName VARCHAR(4000)
SET @scenarioFullName =	@suiteFullName + '@' + @scenarioName

-- Check if this scenario already exists. Insert a new one if needed
DECLARE @existingScenarioId INT =0
SET @existingScenarioId = (SELECT tScenarios.scenarioId FROM tScenarios WHERE tScenarios.fullName=@scenarioFullName)
IF (@existingScenarioId > 0)
    BEGIN
        -- this is an already existing scenario
        SET @scenarioId = @existingScenarioId
        -- update scenario's description
        UPDATE tScenarios SET [description] = @scenarioDescription WHERE scenarioId = @existingScenarioId
    END
ELSE
    BEGIN
        -- this is a new scenario
        INSERT INTO  tScenarios (fullName, name, description)
      VALUES				(@scenarioFullName, @scenarioName, @scenarioDescription)
        SET @scenarioId = @@IDENTITY
    END

-- Insert a new testcase
INSERT INTO  tTestcases	(suiteId, scenarioId, name, dateStart, result)
    VALUES	        (@suiteId, @scenarioId, @testcaseName, @dateStartActual, @result)

SET @tcId = @@IDENTITY
SET @RowsInserted = @@ROWCOUNT
GO

print 'end alter sp_start_testcase'
GO


print 'start alter sp_get_testcases'
GO
/****** Object:  StoredProcedure [dbo].[sp_get_testcases]    Script Date: 08/11/2017 17:30:48 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
ALTER  PROCEDURE [dbo].[sp_get_testcases]

@StartRecord VARCHAR(100)
, @RecordsCount VARCHAR(100)
, @WhereClause VARCHAR(1000)
, @SortCol VARCHAR(100)
, @SortType VARCHAR(100)

AS

CREATE TABLE #tmpTestcases
(
    [testcaseId] [int] NOT NULL,
	[scenarioId] [int] NOT NULL,
    [suiteId] [int] NOT NULL,
    [name] [varchar](255) NOT NULL,
    [dateStart] [datetime] NOT NULL,
    [dateEnd] [datetime] NULL,
    [duration] [int] NOT NULL,
	[result] [int] NOT NULL,
    [userNote] [varchar](255) NULL
)

DECLARE @testcaseId INT
DECLARE @fetchStatus INT = 0

EXEC ('DECLARE testcaseCursor CURSOR FOR
            SELECT tr.testcaseId FROM (
                SELECT testcaseId, ROW_NUMBER() OVER (ORDER BY testcaseId ) AS Row
                FROM tTestcases ' + @WhereClause + ' ) as tr
            WHERE tr.Row >= ' + @StartRecord + ' AND tr.Row <= ' + @RecordsCount)
            
OPEN testcaseCursor

WHILE 0 = @fetchStatus
    BEGIN
        FETCH NEXT FROM testcaseCursor INTO @testcaseId
        SET @fetchStatus = @@FETCH_STATUS
        IF 0 = @fetchStatus
            BEGIN

	INSERT INTO #tmpTestcases
		SELECT	tTestcases.testcaseId,
				tTestcases.scenarioId,
				tTestcases.suiteId,
				tTestcases.name,
				tTestcases.dateStart,
				tTestcases.dateEnd,
				
				CASE WHEN tTestcases.dateEnd IS NULL
					THEN datediff(second, tTestcases.dateStart, GETDATE() )
					ELSE datediff(second, tTestcases.dateStart, tTestcases.dateEnd )
				END AS duration,
	            
				tTestcases.result,
				tTestcases.userNote
			FROM tTestcases 
			WHERE testcaseId = @testcaseId
			
		END
    END
CLOSE testcaseCursor
DEALLOCATE testcaseCursor

EXEC('SELECT * FROM #tmpTestcases ORDER BY ' + @SortCol + ' ' + @SortType )
drop table #tmpTestcases

print 'end alter sp_get_testcases'
GO

print 'start alter sp_get_scenarios'
GO
/****** Object:  StoredProcedure [dbo].[sp_get_scenarios]    Script Date: 08/10/2017 17:34:57 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
ALTER        PROCEDURE [dbo].[sp_get_scenarios]

@StartRecord VARCHAR(100)
, @RecordsCount VARCHAR(100)
, @WhereClause VARCHAR(1000)
, @SortCol VARCHAR(100)
, @SortType VARCHAR(100)

AS

CREATE TABLE #tmpScenarios 
(
    [scenarioId] [int] NOT NULL,
    [suiteId] [int] NOT NULL,
    [name] [varchar](255) NOT NULL,
    [description] [varchar](4000) NULL,
    [result] [int] NOT NULL,
    [testcasesTotal] [int] NOT NULL,
    [testcasesFailed] [int] NOT NULL,
    [testcasesPassedPercent] [float] NOT NULL,
    [testcaseIsRunning] [bit] NOT NULL,
    [dateStart] [datetime] NOT NULL,
    [dateEnd] [datetime] NULL,
    [duration] [int] NOT NULL,
    [userNote] [varchar](255) NULL
)

CREATE TABLE #tmpTestcases(
  [testcaseId] [int] NOT NULL,
    [scenarioId] [int] NOT NULL,
    [suiteId] [int] NOT NULL,
    [dateStart] [datetime] NOT NULL,
  [dateEnd] [datetime] NULL,
    [result] [int] NOT NULL
)

DECLARE @testcasesTotal INT
DECLARE @testcasesFailed INT
DECLARE @testcasesSkipped INT
DECLARE @testcaseIsRunning INT

DECLARE @scenarioResult INT
DECLARE @scenarioDateStart datetime
DECLARE @scenarioDateEnd datetime
DECLARE @scenarioDuration INT

DECLARE @scenarioId INT

EXEC('INSERT INTO #tmpTestcases SELECT testcaseId, scenarioId, suiteId, dateStart, dateEnd, result FROM tTestcases '+@WhereClause )

EXEC ('DECLARE scenariosCursor CURSOR FOR
            SELECT  distinct tr.scenarioId FROM (
                SELECT scenarioId, rank() OVER (ORDER BY scenarioId ) AS Row
                FROM #tmpTestcases GROUP BY scenarioId ) as tr
            WHERE tr.Row >= ' + @StartRecord + ' AND tr.Row <= ' + @RecordsCount )
            
OPEN scenariosCursor
FETCH NEXT FROM scenariosCursor INTO @scenarioId
WHILE @@FETCH_STATUS = 0
    BEGIN   
    -- calculate testcase info
            SET @testcasesTotal     = (SELECT COUNT(testcaseId) FROM #tmpTestcases WHERE scenarioId=@scenarioId)
            SET @testcasesFailed    = (SELECT COUNT(testcaseId) FROM #tmpTestcases WHERE scenarioId=@scenarioId AND result=0)
            SET @testcasesSkipped   = (SELECT COUNT(testcaseId) FROM #tmpTestcases WHERE scenarioId=@scenarioId AND result=2)
            SET @testcaseIsRunning  = (SELECT COUNT(testcaseId) FROM #tmpTestcases WHERE scenarioId=@scenarioId AND result=4)

    -- calculate scenario result
    SET @scenarioResult = 1;
    IF(@testcaseIsRunning > 0)
      SET @scenarioResult = 4;
    ELSE IF(@testcasesFailed > 0)
      SET @scenarioResult = 0;
    ELSE IF(@testcasesSkipped > 0)
      SET @scenarioResult = 2;

    -- calculate scenario dates
    SET @scenarioDateStart	= (SELECT TOP 1 dateStart FROM #tmpTestcases WHERE scenarioId=@scenarioId order by testcaseId ASC)
    SET @scenarioDateEnd	= (SELECT TOP 1 dateEnd   FROM #tmpTestcases WHERE scenarioId=@scenarioId order by testcaseId DESC)
    
    DECLARE @currentScenarioDuration int = 0
    DECLARE @testDuration int
    DECLARE @dateEnd DATETIME
    DECLARE @dateStart DATETIME

	EXEC('DECLARE dateCursor CURSOR FOR SELECT dateStart, dateEnd FROM #tmpTestcases WHERE scenarioId=' + @scenarioId )  
	OPEN dateCursor;  
	FETCH NEXT FROM dateCursor INTO @dateStart, @dateEnd;
	WHILE @@FETCH_STATUS = 0  
	   BEGIN  	  
		  IF @dateEnd IS NULL
				SET @testDuration = datediff(second, @dateStart, GETDATE() );
          ELSE 
				SET @testDuration = datediff(second, @dateStart, @dateEnd );

		  SET @currentScenarioDuration = @currentScenarioDuration + @testDuration

		  FETCH NEXT FROM dateCursor INTO @dateStart, @dateEnd;
	   END;  
	CLOSE dateCursor;  
	DEALLOCATE dateCursor;

            INSERT INTO #tmpScenarios
            SELECT  @scenarioId,
        (SELECT TOP 1 suiteId from #tmpTestcases WHERE scenarioId=@scenarioId),
                    tScenarios.name as name,
                    tScenarios.description,
                    @scenarioResult,

                    @testcasesTotal,
                    @testcasesFailed,
                    (@testcasesTotal - @testcasesFailed - @testcasesSkipped) * 100.00 / CASE WHEN @testcasesTotal=0 THEN 1 ELSE @testcasesTotal END AS testcasesPassedPercent,
                    @testcaseIsRunning,

                    @scenarioDateStart,
                    @scenarioDateEnd,

                    @currentScenarioDuration AS duration,

                    tScenarios.userNote
            FROM    tScenarios
            WHERE   scenarioId=@scenarioId
      FETCH NEXT FROM scenariosCursor INTO @scenarioId;
END;
CLOSE scenariosCursor;
DEALLOCATE scenariosCursor;

drop table #tmpTestcases

EXEC('SELECT * FROM #tmpScenarios ORDER BY ' + @SortCol + ' ' + @SortType)
drop table #tmpScenarios
GO

print 'end alter sp_get_scenarios '
GO

IF (@@ERROR != 0)
BEGIN
    RAISERROR(N'Error occured while performing update to internal version 8', 16, 1) WITH LOG;
    RETURN;
END


UPDATE [dbo].[tInternal] SET [value]='8' WHERE [key]='internalVersion'
GO

/****** Record the internal version ******/
print '-- update internalVersion in [dbo].[tInternal]'
GO
INSERT INTO tInternal ([key], value) VALUES ('Upgrade_to_intVer_9', SYSDATETIME());
GO

-- updates for internal version 9

print 'start alter sp_populate_checkpoint_summary'
GO
/****** Object:  StoredProcedure [dbo].[sp_get_scenarios]    Script Date: 08/10/2017 17:34:57 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE PROCEDURE [dbo].[sp_populate_checkpoint_summary]

@loadQueueId INT,
@name VARCHAR(255),
@transferRateUnit VARCHAR(50),
@checkpointSummaryId INT =0 OUT

AS
BEGIN

	INSERT INTO tCheckpointsSummary
                    ( loadQueueId,  name, numRunning, numPassed, numFailed, minResponseTime, maxResponseTime, avgResponseTime, minTransferRate, maxTransferRate, avgTransferRate, transferRateUnit)
    VALUES
                    -- insert the max possible int value for minResponseTime, so on the first End Checkpoint event we will get a real min value
                    -- insert the same value for maxTransferRate which is float
                    (@loadQueueId, @name, 0, 0, 0, 2147483647, 0, 0, 2147483647, 0, 0, @transferRateUnit)

    SET @checkpointSummaryId = @@IDENTITY
END
GO
/****** Object:  StoredProcedure [dbo].[sp_start_checkpoint]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

print 'end alter sp_populate_checkpoint_summary'
GO

print 'start alter sp_start_checkpoint'
GO
/****** Object:  StoredProcedure [dbo].[sp_start_checkpoint]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
ALTER   PROCEDURE [dbo].[sp_start_checkpoint]

@loadQueueId INT
,@name VARCHAR(255)
,@mode INT
,@transferRateUnit VARCHAR(50)

,@checkpointSummaryId INT =0 OUT
,@checkpointId INT =0 OUT

AS

-- 0 - FAILED
-- 1 - PASSED
-- 4 - RUNNING
DECLARE @result INT =4

-- As this procedure is usually called from more more than one thread (from 1 or more ATS agents)
-- it happens that more than 1 thread enters this stored procedure at the same time and they all ask for the checkpoint summary id,
-- they all see the needed summary checkpoint is not present and they all create one. This is wrong!
-- The fix is to make sure that only 1 thread at a time executes this stored procedure and all other threads are blocked.
-- This is done by using a lock with exclusive mode. The lock is automatically released at the end of the transaction.

BEGIN TRAN StartCheckpointTransaction
DECLARE @get_app_lock_res INT
EXEC @get_app_lock_res = sp_getapplock @Resource = 'StartCheckpointTransaction Lock ID', @LockMode = 'Exclusive';

IF @get_app_lock_res < 0
    -- error getting lock
    -- client will see there was an error as @RowsInserted stays 0
    RETURN;

BEGIN

    -- get an ID if already present
    SET @checkpointSummaryId =( SELECT  checkpointSummaryId
                                FROM    tCheckpointsSummary
                                WHERE   loadQueueId = @loadQueueId AND name = @name   )

    -- SUMMARY table - it keeps 1 row for ALL values of a checkpoint
    IF (@checkpointSummaryId IS NOT NULL)
        -- update existing entry
        BEGIN
            UPDATE tCheckpointsSummary
            SET     numRunning = numRunning + 1
            WHERE checkpointSummaryId = @checkpointSummaryId
        END

    -- insert in DETAILS table when running FULL mode - it keeps 1 row for EACH value of a checkpoint
    --   @mode == 0 -> SHORT mode
    --   @mode != 0 -> FULL mode
    IF @mode != 0
    BEGIN
        INSERT INTO tCheckpoints
            (checkpointSummaryId, name, responseTime, transferRate, transferRateUnit, result, endTime)
        VALUES
            (@checkpointSummaryId, @name, 0, 0, @transferRateUnit, @result, null)

        SET @checkpointId = @@IDENTITY
    END
END

IF @@ERROR <> 0 --error has happened
    ROLLBACK
ELSE
    COMMIT
GO
/****** Object:  StoredProcedure [dbo].[sp_end_checkpoint]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

print 'end alter sp_start_checkpoint'
GO

IF (@@ERROR != 0)
BEGIN
    RAISERROR(N'Error occured while performing update to internal version 9', 16, 1)  WITH LOG;
    RETURN;
END

UPDATE [dbo].[tInternal] SET [value]='9' WHERE [key]='internalVersion'
GO

/****** Record the internal version ******/
print '-- update internalVersion in [dbo].[tInternal]'
GO
INSERT INTO tInternal ([key], value) VALUES ('Upgrade_to_intVer_10', SYSDATETIME());
GO

-- updates for internal version 10

print 'start alter procedure sp_update_testcase'
GO
/****** Object:  StoredProcedure [dbo].[sp_update_testcase]    Script Date: 04/17/2017 14:13:29 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
ALTER       PROCEDURE [dbo].[sp_update_testcase]

@testcaseId INT,
@suiteFullName VARCHAR(255),
@scenarioName VARCHAR(255),
@scenarioDescription VARCHAR(4000),
@testcaseName VARCHAR(255),
@userNote VARCHAR(255),
@testcaseResult INT,
@timestamp DATETIME,
@RowsUpdated INT =0 OUT

AS

DECLARE 
@currentDateEnd DATETIME = (SELECT dateEnd FROM  tTestcases WHERE testcaseId = @testcaseId)
,@currentTestcaseName VARCHAR(255) = (SELECT name FROM  tTestcases WHERE testcaseId = @testcaseId)
,@currentTestcaseResult INT = (SELECT result FROM tTestcases WHERE testcaseId = @testcaseId)
,@currentScenarioName VARCHAR(255) = (SELECT name FROM tScenarios WHERE scenarioId = (SELECT scenarioId FROM tTestcases WHERE testcaseId = @testcaseId))
,@currentFullName VARCHAR(255) = (SELECT fullName FROM tScenarios WHERE scenarioId = (SELECT scenarioId FROM tTestcases WHERE testcaseId = @testcaseId))
,@currentScenarioDescription VARCHAR(255) = (SELECT description FROM tScenarios WHERE scenarioId = (SELECT scenarioId FROM tTestcases WHERE testcaseId = @testcaseId))
,@scenariosWithThisFullNameCount INT = (SELECT COUNT(*) FROM tScenarios WHERE fullName = (@suiteFullName + '@' + @scenarioName))
-- the id of the scenario, that is already inserted and has the same full name as the one, provided as a fuction argument
,@alreadyInsertedScenarioId INT =-1
-- save the current testcase' scenario ID
,@currentTestcaseScenarioId INT

IF (@scenariosWithThisFullNameCount > 0)
    BEGIN
    -- Since there already has scenario with fullName that is equal to the one, that this function will set,
    -- simply change the current testcase's scenarioId to the already existing one and delete the unnecessary scenario (indicated by the currrent testcase ID)
    SET @currentTestcaseScenarioId = (SELECT scenarioId FROM tTestcases WHERE testcaseId = @testcaseId)
	SET @currentTestcaseScenarioId = (SELECT scenarioId FROM tTestcases WHERE testcaseId = @testcaseId)
    SET @alreadyInsertedScenarioId = (SELECT scenarioId FROM tScenarios WHERE fullName = @suiteFullName + '@' + @scenarioName)
    UPDATE tTestcases
    SET scenarioId = (SELECT scenarioId FROM tScenarios WHERE fullName = @suiteFullName + '@' + @scenarioName) 
    WHERE testcaseId = @testcaseId
    DELETE FROM tScenarios WHERE scenarioId = @currentTestcaseScenarioId
    END


UPDATE tTestcases SET name = CASE
                                 WHEN @testcaseName IS NULL THEN @currentTestcaseName
                                 ELSE @testcaseName
                             END,
                      dateEnd = CASE
                                    WHEN @timestamp IS NULL THEN @currentDateEnd
                                    ELSE @timestamp
                                END,
                      result = CASE
                                    WHEN @testcaseResult = -1 THEN @currentTestcaseResult
                                    ELSE @testcaseResult
                               END,
                      userNote = @userNote
WHERE testcaseId = @testcaseId;


UPDATE tScenarios SET name = CASE
                                   WHEN @scenarioName IS NULL THEN @currentScenarioName
                                   ELSE @scenarioName
                               END, 
                        description = CASE
                                          WHEN @scenarioDescription IS NULL THEN @currentScenarioDescription
                                          ELSE @scenarioDescription
                                      END,
                        fullName = CASE 
                                       WHEN (@suiteFullName IS NULL OR @scenarioName IS NULL) AND @alreadyInsertedScenarioId = -1
                                       THEN @currentFullName
                                       ELSE @suiteFullName + '@' + @scenarioName
                                       END  
WHERE scenarioId = (SELECT scenarioId FROM tTestcases WHERE testcaseId = @testcaseId);

SET @RowsUpdated = @@ROWCOUNT
GO

print 'end alter procedure sp_update_testcase'
GO

print 'start alter table tScenarios '
GO

SET ANSI_PADDING ON
GO
/****** Object:  Table [dbo].[tScenarios]    Script Date: 04/11/2011 20:46:20 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
ALTER TABLE [dbo].[tScenarios]
ADD CONSTRAINT U_FullName UNIQUE(fullName);
GO

print 'end alter table tScenarios '
GO

IF (@@ERROR != 0)
BEGIN
    RAISERROR(N'Error occured while performing update to internal version 10', 16, 1)  WITH LOG;
    RETURN;
END

UPDATE [dbo].[tInternal] SET [value]='10' WHERE [key]='internalVersion'
GO

