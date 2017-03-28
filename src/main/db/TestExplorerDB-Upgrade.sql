-- ***********************************************
-- Script for upgrade from version 3.11.0 to 4.0.0
-- ***********************************************

DROP PROCEDURE dbo.sp_get_controllers;  
GO
DROP PROCEDURE dbo.getUniqueMessageId;  
GO
DROP PROCEDURE dbo.sp_end_testcase;  
GO
DROP PROCEDURE dbo.sp_get_checkpoint_aggregated_statistics;  
GO
DROP PROCEDURE dbo.sp_start_controller;  
GO
DROP PROCEDURE dbo.sp_end_controller;  
GO
DROP PROCEDURE dbo.sp_start_checkpoint;  
GO
DROP PROCEDURE dbo.sp_insert_checkpoint;  
GO
DROP PROCEDURE dbo.sp_get_checkpoint_statistics;  
GO
DROP PROCEDURE dbo.sp_get_checkpoint_statistic_descriptions;  
GO
ALTER TABLE [dbo].[tCheckpointsSummary]	DROP CONSTRAINT [FK_tCheckpointsSummary_tControllers];
GO
ALTER TABLE [dbo].[tControllers] DROP CONSTRAINT [FK_tControllers_tTestcases];
GO

print 'Starting modifications in tControllers table'
GO
-- drop primary key constraint
ALTER TABLE [dbo].[tControllers]	DROP CONSTRAINT [PK_tControllers];
-- drop index
DROP INDEX IX_tControllers_byTestcaseId  ON [dbo].[tControllers];  
GO 
-- rename table
exec sp_rename 'tControllers', 'tLoadQueues' 
GO
-- rename column
EXEC sp_rename 'tLoadQueues.controllerId', 'loadQueueId', 'COLUMN';  
GO  
-- recreate index
CREATE NONCLUSTERED INDEX IX_tLoadQueues_byTestcaseId ON dbo.tLoadQueues
(
    [testcaseId]
) WITH( STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
-- recreate primary key constraint
ALTER TABLE [dbo].[tLoadQueues]
ADD CONSTRAINT PK_tLoadQueues PRIMARY KEY CLUSTERED ( [loadQueueId] ASC ) 
WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
GO
print 'Done modifications in tControllers table'
GO

EXEC sp_rename 'tCheckpointsSummary.controllerId', 'loadQueueId', 'COLUMN';  
GO  

/****** Object:  ForeignKey [FK_tCheckpointsSummary_tLoadQueues]    Script Date: 18/11/2016 20:46:20 ******/
ALTER TABLE [dbo].[tCheckpointsSummary]  WITH CHECK ADD  CONSTRAINT [FK_tCheckpointsSummary_tLoadQueues] FOREIGN KEY([loadQueueId])
REFERENCES [dbo].[tLoadQueues] ([loadQueueId])
ON UPDATE CASCADE
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[tCheckpointsSummary] CHECK CONSTRAINT [FK_tCheckpointsSummary_tLoadQueues]
GO
/****** Object:  ForeignKey [FK_tLoadQueues_tTestcases]    Script Date: 18/11/2016 20:46:20 ******/
ALTER TABLE [dbo].[tLoadQueues]  WITH CHECK ADD  CONSTRAINT [FK_tLoadQueues_tTestcases] FOREIGN KEY([testcaseId])
REFERENCES [dbo].[tTestcases] ([testcaseId])
ON UPDATE CASCADE
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[tLoadQueues] CHECK CONSTRAINT [FK_tLoadQueues_tTestcases]
GO

/****** Object:  StoredProcedure [dbo].[sp_get_loadqueues]    Script Date: 18/11/2016 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE  PROCEDURE [dbo].[sp_get_loadqueues]

@WhereClause VARCHAR(1000)
, @SortCol VARCHAR(100)
, @SortType VARCHAR(100)

AS

DECLARE @sql_1 varchar(8000)
SET        @sql_1 =  'SELECT         loadQueueId,
                                name,
                                sequence,
                                hostsList,
                                threadingPattern,
                                numberThreads,
                                machineId,
                                dateStart,
                                dateEnd,
                                CASE WHEN dateEnd IS NULL
                                    THEN datediff(second, dateStart, GETDATE() )
                                    ELSE datediff(second, dateStart, dateEnd )
                                END AS duration,

                                result,
                                userNote

                    FROM tLoadQueues ' + @WhereClause + ' ORDER BY ' + @SortCol + ' ' + @SortType

EXEC (@sql_1)
GO

/****** Object:  StoredProcedure [dbo].[getUniqueMessageId]    Script Date: 18/11/2016 16:09:30 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE PROCEDURE [dbo].[getUniqueMessageId]
@Message nvarchar(3950),
@MessageId int OUT
AS

BEGIN

DECLARE @Hash binary(20)
DECLARE @NUMBER_TRIES INT = 0

-- when more than 1 load agent try to insert at the same time the same unique message
-- they all see there is no an entry for this unique message, so they all try to insert
-- the new message into tUniqueMessages, but only the first one succeed as there is
-- unique constraint on this table

-- so each thread tries the first time and if fail, tries once again, if fail again,
-- the OUT parameter @MessageId will remain NULL, and this will cause failure in the
-- procedure which calls this one
DOIT:
    BEGIN TRY
        IF (@NUMBER_TRIES < 2)
        BEGIN
            SET @NUMBER_TRIES = @NUMBER_TRIES + 1
            SET @Hash        = HashBytes('SHA1', @Message);
            SET @MessageId   = ( SELECT    um.uniqueMessageId FROM    tUniqueMessages um WHERE    um.hash = @Hash)
            -- insert the message if not already there
            IF @MessageId IS NULL
                BEGIN
                    INSERT INTO tUniqueMessages(hash, message) VALUES (@Hash, @Message)
                    SET @MessageId = SCOPE_IDENTITY()
                END
        END
    END TRY
    BEGIN CATCH
           GOTO DOIT;
    END CATCH;

END
GO

/****** Object:  StoredProcedure [dbo].[sp_end_testcase]    Script Date: 18/11/2016 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE      PROCEDURE [dbo].[sp_end_testcase]

@testcaseId INT
,@result INT
,@dateEnd DATETIME

,@RowsUpdated INT =0 OUT

AS

DECLARE
@dateEndActual DATETIME
EXECUTE [dbo].[getAutoDate] @dateEnd, @dateEndActual OUTPUT

DECLARE @numberFailedQueues INT
SELECT @numberFailedQueues = COUNT(loadQueueId) FROM tLoadQueues WHERE testcaseId = @testcaseId AND result = 0
IF(@numberFailedQueues > 0) SET @result = 0


-- end the testcase
UPDATE    tTestcases
SET        result = @result, dateEnd = @dateEndActual
WHERE    testcaseId = @testcaseId

SET     @RowsUpdated = @@ROWCOUNT
GO

/****** Object:  StoredProcedure [dbo].[sp_get_checkpoint_aggregated_statistics]    Script Date: 18/11/2016 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE   PROCEDURE [dbo].[sp_get_checkpoint_aggregated_statistics]

@fdate varchar(150),
@testcaseIds varchar(150),
@checkpointNames varchar(1000),
@interval int,
@mode int

AS

DECLARE @firstTT int
DECLARE @sql nvarchar(4000)
DECLARE @ParmDefinition nvarchar(50);

SET @sql = N'
SELECT @firstTTOut = MIN( DATEDIFF(second, CONVERT( datetime, ''' + @fdate + ''', 20), ch.endTime))
     FROM tCheckpoints ch
     INNER JOIN tCheckpointsSummary chs on (chs.checkpointSummaryId = ch.checkpointSummaryId)
     INNER JOIN tLoadQueues c on (c.loadQueueId = chs.loadQueueId)
     INNER JOIN tTestcases tt on (tt.testcaseId = c.testcaseId)
WHERE tt.testcaseId in ( '+@testcaseIds+' ) AND ch.name in ( '+@checkpointNames+' ) AND ch.result = 1 AND ch.endTime IS NOT NULL';
SET @ParmDefinition = N'@firstTTOut int OUTPUT';

EXECUTE sp_executesql @sql, @ParmDefinition, @firstTTOut=@firstTT OUTPUT;

DECLARE @firstTTChar varchar(20)
DECLARE @intervalChar varchar(20)
SET  @firstTTChar = CAST(@firstTT as varchar(20))
SET  @intervalChar = CAST(@interval as varchar(20))

-- Modes:
-- AVG    - 0001
-- SUM    - 0010
-- TOTALS - 0100
-- COUNT  - 1000
DECLARE @sumSelectString varchar(150)
DECLARE @avgSelectString varchar(150)
DECLARE @countSelectString varchar(150)
SET @avgSelectString = '-1 as avgValue'
SET @sumSelectString = '-1 as sumValue'
SET @countSelectString = '-1 as countValue'

-- In (@firstTTChar + 1), '+1' is used to add the upper bound timestamp (from the current range) in the AVG or SUM calculations
IF (@mode & 0x01 > 0) -- average values
    SET @avgSelectString = 'AVG(tt.value) OVER ( PARTITION BY (tt.statsAxisTimestamp - (' + @firstTTChar + '+1)) / ' + @intervalChar + ', tt.statsName ) as avgValue'
IF ( (@mode & 0x02 > 0) OR (@mode & 0x04 > 0) ) -- sum values or total values(these are calculate later in the java layer by using the sum values)
    SET @sumSelectString = 'SUM(tt.value) OVER ( PARTITION BY (tt.statsAxisTimestamp - (' + @firstTTChar + '+1)) / ' + @intervalChar + ', tt.statsName ) as sumValue'
IF (@mode & 0x08 > 0)
    SET @countSelectString = 'COUNT(tt.value) OVER ( PARTITION BY (tt.statsAxisTimestamp - (' + @firstTTChar + '+1)) / ' + @intervalChar + ', tt.statsName ) as countValue'

SET @sql = N'
SELECT
    avgValue, sumValue, countValue, statsName, queueName, testcaseId,
    CASE WHEN mpl=0 THEN ' + @firstTTChar + '+' + @intervalChar + ' ELSE (mpl*' + @intervalChar + ')+' + @firstTTChar + '+' + @intervalChar + ' END as timestamp
FROM (
    SELECT
        MIN(rr.avgValue) OVER ( PARTITION BY rr.mpl, rr.statsName ) as avgValue,
        MIN(rr.sumValue) OVER ( PARTITION BY rr.mpl, rr.statsName ) as sumValue,
        MIN(rr.countValue) OVER ( PARTITION BY rr.mpl, rr.statsName ) as countValue,
        rr.statsName, rr.queueName, rr.testcaseId, rr.mpl
    FROM (
        SELECT
            ' + @avgSelectString + ', ' + @sumSelectString + ', ' + @countSelectString + ',
            tt.statsAxisTimestamp, (tt.statsAxisTimestamp - (' + @firstTTChar + '+1)) / ' + @intervalChar + ' as mpl,
            tt.statsName, tt.queueName, tt.testcaseId
        FROM (
            SELECT ch.name as statsName, c.name as queueName, ch.responseTime as value, tt.testcaseId,
                    DATEDIFF(second, CONVERT( datetime, ''' + @fdate + ''', 20), ch.endTime) as statsAxisTimestamp
                 FROM tCheckpoints ch
                 INNER JOIN tCheckpointsSummary chs on (chs.checkpointSummaryId = ch.checkpointSummaryId)
                 INNER JOIN tLoadQueues c on (c.loadQueueId = chs.loadQueueId)
                 INNER JOIN tTestcases tt on (tt.testcaseId = c.testcaseId)
            WHERE tt.testcaseId in ( '+@testcaseIds+' ) AND ch.name in ( '+@checkpointNames+' ) AND ch.result = 1 AND ch.endTime IS NOT NULL
        ) tt
    ) rr
) bb
GROUP BY  bb.avgValue, bb.sumValue, bb.countValue, bb.queueName, bb.statsName, bb.testcaseId, bb.mpl
ORDER BY  bb.mpl ASC';

EXEC (@sql)
GO

/****** Object:  StoredProcedure [dbo].[sp_start_loadqueue]    Script Date: 18/11/2016 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE          PROCEDURE [dbo].[sp_start_loadqueue]

@testcaseId INT
,@name VARCHAR(255)
,@sequence INT
,@hostsList VARCHAR(255)
,@threadingPattern VARCHAR(255)
,@numberThreads INT
,@machine VARCHAR(255)
,@dateStart DATETIME
,@RowsInserted INT =0 OUT
,@loadQueueId INT =0 OUT

AS

DECLARE
@dateStartActual DATETIME
EXECUTE [dbo].[getAutoDate]    @dateStart ,@dateStartActual  OUTPUT

-- POSSIBLE LOAD QUEUE RESULT VALUES
-- 0 FAILED
-- 1 PASSED
-- 2 CANCELLED
-- 4 RUNNING

-- The load queue is initially RUNNING
DECLARE @result INT
SET @result = 4

-- get machine's id
DECLARE @machineId INT
EXECUTE [dbo].[getMachineId] @machine, @machineId OUTPUT

-- When the test steps are run from more than 1 agent loader, we need to synchronize them here as we want to have
-- just 1 loader for 1 action queue. This way all checkpoints info from agent instances is merged in a single place

BEGIN TRAN StartLoadQueueTransaction
DECLARE @get_app_lock_res INT
EXEC @get_app_lock_res = sp_getapplock @Resource = 'StartLoadQueueTransaction Lock ID', @LockMode = 'Exclusive';

IF @get_app_lock_res < 0
    -- error getting lock
    -- client will see there was an error as @RowsInserted stays 0
    RETURN;

BEGIN

    -- start the load queue only if not already there - necessary for remote execution
    SET @RowsInserted = 1
    SET @loadQueueId = (SELECT loadQueueId FROM tLoadQueues WHERE name = @name AND sequence = @sequence AND testcaseId = @testcaseId)
    IF (@loadQueueId IS NULL)
        -- start a new load queue
        BEGIN
            INSERT INTO  tLoadQueues
                    (testcaseId, name, sequence, hostsList, threadingPattern, numberThreads, machineId, dateStart, result)
                VALUES
                    (@testcaseId, @name, @sequence, @hostsList, @threadingPattern, @numberThreads, @machineId, @dateStartActual, @result)
            SET @loadQueueId = @@IDENTITY
            SET @RowsInserted = @@ROWCOUNT
        END
    ELSE
        -- this load queue is started again
        -- update the number of threads value, reset its result and end date as it is running again
        BEGIN
            UPDATE    tLoadQueues
            SET       numberThreads = numberThreads + @numberThreads,
                      dateEnd = NULL,
                      result = @result
            WHERE name = @name AND testcaseId = @testcaseId AND sequence = @sequence
        END
END

IF @@ERROR <> 0 --error has happened
    ROLLBACK
ELSE
    COMMIT
GO

/****** Object:  StoredProcedure [dbo].[sp_end_loadqueue]    Script Date: 18/11/2016 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE       PROCEDURE [dbo].[sp_end_loadqueue]

@loadQueueId INT
,@result INT
,@dateEnd DATETIME

,@RowsUpdated INT =0 OUT

AS

DECLARE
@dateEndActual DATETIME
EXECUTE [dbo].[getAutoDate] @dateEnd, @dateEndActual OUTPUT

-- end the loadqueue
UPDATE    tLoadQueues
SET        result = @result, dateEnd = @dateEndActual
WHERE      loadQueueId = @loadQueueId

SET     @RowsUpdated = @@ROWCOUNT
GO

/****** Object:  StoredProcedure [dbo].[sp_start_checkpoint]    Script Date: 18/11/2016 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE   PROCEDURE [dbo].[sp_start_checkpoint]

@loadQueueId INT
,@threadName VARCHAR(255)
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
    IF (@checkpointSummaryId IS NULL)
        -- new entry
        BEGIN
            INSERT INTO tCheckpointsSummary
                    ( loadQueueId,  name, numRunning, numPassed, numFailed, minResponseTime, maxResponseTime, avgResponseTime, minTransferRate, maxTransferRate, avgTransferRate, transferRateUnit)
                VALUES
                    -- insert the max possible int value for minResponseTime, so on the first End Checkpoint event we will get a real min value
                    -- insert the same value for maxTransferRate which is float
                    (@loadQueueId, @name, 1, 0, 0, 2147483647, 0, 0, 2147483647, 0, 0, @transferRateUnit)

            SET @checkpointSummaryId = @@IDENTITY
        END
    ELSE
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

/****** Object:  StoredProcedure [dbo].[sp_insert_checkpoint]    Script Date: 18/11/2016 11:18:42 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE  PROCEDURE [dbo].[sp_insert_checkpoint]

@loadQueueId INT
,@threadName VARCHAR(255)
,@name VARCHAR(150)
,@responseTime INT
,@endTime DATETIME
,@transferSize BIGINT
,@transferRateUnit VARCHAR(50)
,@result INT
,@mode INT

AS

DECLARE @checkpointSummaryId INT=0
DECLARE @checkpointId INT=0

-- As this procedure is usually called from more more than one thread (from 1 or more ATS agents)
-- it happens that more than 1 thread enters this stored procedure at the same time and they all ask for the checkpoint summary id,
-- they all see the needed summary checkpoint is not present and they all create one. This is wrong!
-- The fix is to make sure that only 1 thread at a time executes this stored procedure and all other threads are blocked.
-- This is done by using a lock with exlusive mode. The lock is automatically released at the end of the transaction.

BEGIN TRAN InsertCheckpointTransaction
DECLARE @get_app_lock_res INT
EXEC @get_app_lock_res = sp_getapplock @Resource = 'InsertCheckpointTransaction Lock ID', @LockMode = 'Exclusive';

IF @get_app_lock_res < 0
    -- error getting lock
    -- client will see there was an error as @RowsInserted stays 0
    RETURN;

BEGIN

    IF (@result = 0)
        BEGIN
            SET @responseTime = 0
            SET @transferSize = 0
        END

    DECLARE @transferRate FLOAT
    IF @responseTime > 0
        -- in order to transform transferSize into float we multiply with 1000.0 instead of 1000
        SET @transferRate = @transferSize*1000.0/@responseTime
    ELSE SET @transferRate = 0

    -- get an ID if already present
    SET @checkpointSummaryId =( SELECT  checkpointSummaryId
                                FROM    tCheckpointsSummary
                                WHERE   loadQueueId = @loadQueueId AND name = @name   )

    -- SUMMARY table - it keeps 1 row for ALL values of a checkpoint
    IF (@checkpointSummaryId IS NULL)
        -- new entry
        BEGIN
            INSERT INTO tCheckpointsSummary
                    ( loadQueueId,  name,
                        numRunning, numPassed, numFailed,
                        minResponseTime, maxResponseTime, avgResponseTime,
                        minTransferRate, maxTransferRate, avgTransferRate, transferRateUnit)
            VALUES
                    -- insert the max possible int value for minResponseTime, so on the first End Checkpoint event we will get a real min value
                    -- insert the same value for maxTransferRate which is float
                    (@loadQueueId, @name,
                        0, @result, CASE WHEN @result = 0 THEN 1 ELSE 0 END,
                        @responseTime, @responseTime, @responseTime,
                        @transferRate, @transferRate, @transferRate, @transferRateUnit)

            SET @checkpointSummaryId = @@IDENTITY
        END
    ELSE
        -- update existing entry
        IF (@result = 0)
            -- checkpoint failed
            BEGIN
                UPDATE  tCheckpointsSummary
                SET     numFailed = numFailed + 1
                WHERE   checkpointSummaryId = @checkpointSummaryId
            END
        ELSE
            -- checkpoint passed
            BEGIN
                UPDATE tCheckpointsSummary
                SET     numPassed = numPassed + 1,
                        minResponseTime = CASE WHEN @responseTime < minResponseTime THEN @responseTime ELSE minResponseTime END,
                        maxResponseTime = CASE WHEN @responseTime > maxResponseTime THEN @responseTime ELSE maxResponseTime END,
                        avgResponseTime = (avgResponseTime * numPassed + @responseTime)/(numPassed + 1),
                        minTransferRate = CASE WHEN @transferRate < minTransferRate THEN @transferRate ELSE minTransferRate END,
                        maxTransferRate = CASE WHEN @transferRate > maxTransferRate THEN @transferRate ELSE maxTransferRate END,
                        avgTransferRate = (avgTransferRate * numPassed + @transferRate)/(numPassed+ 1)
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
            (@checkpointSummaryId, @name, @responseTime, @transferRate, @transferRateUnit, @result, @endTime)

        SET @checkpointId = @@IDENTITY
    END
END

IF @@ERROR <> 0 --error has happened
    ROLLBACK
ELSE
    COMMIT
GO

/****** Object:  StoredProcedure [dbo].[sp_get_checkpoint_statistics]    Script Date: 18/11/2016 16:05:37 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE   PROCEDURE [dbo].[sp_get_checkpoint_statistics]

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

/****** Object:  StoredProcedure [dbo].[sp_get_checkpoint_statistic_descriptions]    Script Date: 18/11/2016 10:19:48 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE                PROCEDURE [dbo].[sp_get_checkpoint_statistic_descriptions]

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



print '-- UNCOMMENT BEFORE RELEASE update internalVersion in [dbo].[tInternal]'
-- GO

-- UPDATE [dbo].[tInternal] SET [value]='5' WHERE [key]='internalVersion'
-- GO


print 'start alter procedure sp_delete_scenario '
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
ALTER     PROCEDURE [dbo].[sp_delete_scenario]

@scenarioIds VARCHAR(1000),
@suiteId VARCHAR(1000)

AS

DECLARE @delimiter VARCHAR(10) =',' -- the used delimiter

BEGIN
    DECLARE
        @idIndex SMALLINT,
        @idToken VARCHAR(100)

    WHILE @scenarioIds <> ''
    BEGIN
        SET @idIndex = CHARINDEX(@delimiter, @scenarioIds)

        IF @idIndex > 0
            BEGIN
                SET @idToken = LEFT(@scenarioIds, @idIndex-1)
                SET @scenarioIds = RIGHT(@scenarioIds, LEN(@scenarioIds)-@idIndex)
            END
        ELSE
            BEGIN
                SET @idToken = @scenarioIds
                SET @scenarioIds = ''
            END

        DELETE FROM tTestcases WHERE tTestcases.scenarioId=@idToken and tTestcases.suiteId=@suiteId

    END
END

print 'end alter procedure sp_delete_scenario '
GO



print 'start alter procedure sp_delete_testcase '
GO


/****** Object:  StoredProcedure [dbo].[sp_delete_testcase]    Script Date: 03/19/2012 17:05:20 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
ALTER       PROCEDURE [dbo].[sp_delete_testcase]

@testcaseIds VARCHAR(5000)

AS

DECLARE @delimiter VARCHAR(10) =',' -- the used delimiter

BEGIN
    DECLARE
        @idIndex SMALLINT,
        @idToken VARCHAR(100)

    WHILE @testcaseIds <> ''
    BEGIN
        SET @idIndex = CHARINDEX(@delimiter, @testcaseIds)

        IF @idIndex > 0
            BEGIN
                SET @idToken = LEFT(@testcaseIds, @idIndex-1)
                SET @testcaseIds = RIGHT(@testcaseIds, LEN(@testcaseIds)-@idIndex)
            END
        ELSE
            BEGIN
                SET @idToken = @testcaseIds
                SET @testcaseIds = ''
            END

        DELETE FROM tTestcases WHERE tTestcases.testcaseId=@idToken
    END
END
GO

print 'end alter procedure sp_delete_testcase '
GO


print 'start alter procedure sp_db_cleanup '
GO

/****** Object:  StoredProcedure [dbo].[sp_db_cleanup]     ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
ALTER PROCEDURE [dbo].[sp_db_cleanup]

AS
DECLARE @start_time datetime;
DECLARE @end_time datetime;
DECLARE @length int;
BEGIN

 SET @start_time = (SELECT SYSDATETIME());
  PRINT 'START DELETING RUN MESSAGES: ' + cast(@start_time as varchar(20));
  DELETE FROM tRunMessages WHERE runId NOT IN (SELECT runId FROM tRuns);
  SET @end_time = (SELECT SYSDATETIME());
  PRINT 'END DELETING RUN MESSAGES: ' + cast(@end_time as varchar(20));
  PRINT 'EXECUTED FOR TIME IN MILISECONDS:' + cast(DATEDIFF(millisecond,@start_time ,@end_time ) as varchar(100));

  SET @start_time = (SELECT SYSDATETIME());
  PRINT 'START DELETING SUITE MESSAGES: ' + cast(@start_time as varchar(20));
  DELETE FROM tSuiteMessages WHERE suiteId NOT IN (SELECT suiteId FROM tSuites);
  SET @end_time = (SELECT SYSDATETIME());
  PRINT 'END DELETING SUITE MESSAGES: ' + cast(@end_time as varchar(20));
  PRINT 'EXECUTED FOR TIME IN MILISECONDS:' + cast(DATEDIFF(millisecond,@start_time ,@end_time ) as varchar(100));

  -- When deleting tRunMessages, tSuiteMessages and tMessages we cannot not use AUTO DELETE for tUniqueMessages,
  -- because it is possible to have same message(identified by its uniqueMessageId) in more than one place in 
  -- one or more of these parent tables
  SET @start_time = (SELECT SYSDATETIME());
  PRINT 'START DELETING UNIQUE MESSAGES: ' + cast(@start_time as varchar(20));
  DELETE FROM tUniqueMessages WHERE tUniqueMessages.uniqueMessageId NOT IN 
    (
            SELECT uniqueMessageId FROM tMessages 
      UNION SELECT uniqueMessageId FROM tRunMessages
      UNION SELECT uniqueMessageId FROM tSuiteMessages
    );
  SET @end_time = (SELECT SYSDATETIME());
  PRINT 'END DELETING UNIQUE MESSAGES: ' + cast(@end_time as varchar(20));
  PRINT 'EXECUTED FOR TIME IN MILISECONDS:' + cast(DATEDIFF(millisecond,@start_time ,@end_time )as varchar(100));
  
  SET @start_time = (SELECT SYSDATETIME());
  PRINT 'START DELETING SCENARIOS: ' + cast(@start_time as varchar(20));
  DELETE FROM tScenarios WHERE scenarioId NOT IN (SELECT scenarioId FROM tTestcases);
  SET @end_time = (SELECT SYSDATETIME());
  PRINT 'END DELETING SCENARIOS: ' + cast(@end_time as varchar(20));
  PRINT 'EXECUTED FOR TIME IN MILISECONDS:' + cast(DATEDIFF(millisecond,@start_time ,@end_time ) as varchar(100));

END
GO

print 'end alter procedure sp_db_cleanup '
GO
