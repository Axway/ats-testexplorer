-- ***********************************************
-- Script for upgrade from version 4.0.6 to 4.0.7.
-- ***********************************************

print '#17 INTERNAL VERSION UPGRADE HEADER - START'
GO
INSERT INTO tInternal ([key], value) VALUES ('Upgrade_to_intVer_17', SYSDATETIME());
GO
print '#17 INTERNAL VERSION UPGRADE HEADER - END'
GO

print 'start ALTER TABLE tMachines'
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
ALTER TABLE [dbo].[tMachines] ADD CONSTRAINT U_MachineName UNIQUE(machineName)
GO

print 'end ALTER TABLE tMachines'
GO


print '#17 INTERNAL VERSION UPGRADE FOOTER - START'
GO
IF (@@ERROR != 0)
BEGIN
    RAISERROR(N'Error occurred while performing update to internal version 17', 16, 1) WITH LOG;
    RETURN;
END
UPDATE tInternal SET [value]='17' WHERE [key]='internalVersion'
GO
print '#17 INTERNAL VERSION UPGRADE FOOTER - END'
GO


print '#18 INTERNAL VERSION UPGRADE HEADER - START'
GO
INSERT INTO tInternal ([key], value) VALUES ('Upgrade_to_intVer_18', SYSDATETIME());
GO
print '#18 INTERNAL VERSION UPGRADE HEADER - END'
GO



print 'start ALTER PROCEDURE sp_insert_message'
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER        PROCEDURE [dbo].[sp_insert_message]

@testcaseId INT
,@messageTypeId INT
,@message NTEXT
,@escapeHtml INT =1
,@machine VARCHAR(255)
,@threadName VARCHAR(255)
,@timestamp DATETIME

,@RowsInserted INT =0

AS

DECLARE
@timestampActual DATETIME
EXECUTE [dbo].[getAutoDate]    @timestamp ,@timestampActual  OUTPUT

-- --------------------------
DECLARE @Hash                INT
DECLARE @uniqueMessageId    INT
DECLARE @messageChunk        NVARCHAR(3952)

DECLARE     @pos             INT
SELECT     @pos             = 1
DECLARE     @chunkSize        INT
SELECT     @chunkSize          = 3950
SELECT     @RowsInserted     = 0
DECLARE        @parentMessageId    INT = NULL

-- get machine's id
DECLARE @machineId INT
EXECUTE [dbo].[getMachineId] @machine, @machineId OUTPUT

WHILE       @pos*2 <= DATALENGTH(@message)
BEGIN
    SET @messageChunk = SUBSTRING(@message, @pos, @chunkSize)

    EXECUTE [dbo].[getUniqueMessageId] @messageChunk, @uniqueMessageId OUTPUT
    -- insert the message
    IF (DATALENGTH(@message) > @chunkSize*2 AND @pos = 1)   -- NVARCHAR uses 2 bytes per char, so we have to double 'chunkSize' in order to compalre it with DATALENGTH
        BEGIN
            INSERT INTO tMessages
                (testcaseId, messageTypeId, timestamp, escapeHtml, uniqueMessageId, machineId, threadName , parentMessageId)
            VALUES
                (@testcaseId, @messageTypeId, @timestampActual, @escapeHtml, @uniqueMessageId, @machineId, @threadName, IDENT_CURRENT('tMessages'))
            SET    @parentMessageId = IDENT_CURRENT('tMessages')
        END
    ELSE
        INSERT INTO tMessages
            (testcaseId, messageTypeId, timestamp, escapeHtml, uniqueMessageId, machineId, threadName ,parentMessageId)
        VALUES
            (@testcaseId, @messageTypeId, @timestampActual, @escapeHtml, @uniqueMessageId, @machineId, @threadName, @parentMessageId)

    SET @RowsInserted = @RowsInserted + @@ROWCOUNT
    SET @pos = @pos + @chunkSize
END

GO

print 'end ALTER PROCEDURE sp_insert_message'
GO

print 'start ALTER PROCEDURE sp_insert_suite_message'
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER        PROCEDURE [dbo].[sp_insert_suite_message]

@suiteId INT
,@messageTypeId INT
,@message NTEXT
,@escapeHtml INT =1
,@machine VARCHAR(255)
,@threadName VARCHAR(255)
,@timestamp DATETIME

,@RowsInserted INT =0

AS

DECLARE
@timestampActual DATETIME
EXECUTE [dbo].[getAutoDate]    @timestamp ,@timestampActual  OUTPUT

-- --------------------------
DECLARE @Hash                INT
DECLARE @uniqueMessageId    INT
DECLARE @messageChunk        NVARCHAR(3952)

DECLARE     @pos             INT
SELECT     @pos             = 1
DECLARE     @chunkSize        INT
SELECT     @chunkSize     = 3950
SELECT     @RowsInserted     = 0

-- get machine's id
DECLARE @machineId INT
EXECUTE [dbo].[getMachineId] @machine, @machineId OUTPUT

WHILE       @pos*2 <= DATALENGTH(@message)
BEGIN
    SET @messageChunk = SUBSTRING(@message, @pos, @chunkSize)

    EXECUTE [dbo].[getUniqueMessageId] @messageChunk, @uniqueMessageId OUTPUT

    -- insert the message
    INSERT INTO tSuiteMessages
        ( suiteId, messageTypeId, timestamp, escapeHtml, uniqueMessageId, machineId, threadName)
    VALUES
        ( @suiteId, @messageTypeId, @timestampActual, @escapeHtml, @uniqueMessageId, @machineId, @threadName)

    SET @RowsInserted = @RowsInserted + @@ROWCOUNT
        SET @pos = @pos + @chunkSize
END
GO

print 'end ALTER PROCEDURE sp_insert_suite_message'
GO


print 'start ALTER PROCEDURE sp_insert_run_message'
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER        PROCEDURE [dbo].[sp_insert_run_message]

@runId INT
,@messageTypeId INT
,@message NTEXT
,@escapeHtml INT =1
,@machine VARCHAR(255)
,@threadName VARCHAR(255)
,@timestamp DATETIME

,@RowsInserted INT =0

AS

DECLARE
@timestampActual DATETIME
EXECUTE [dbo].[getAutoDate]    @timestamp ,@timestampActual  OUTPUT

-- --------------------------
DECLARE @Hash                INT
DECLARE @uniqueMessageId    INT
DECLARE @messageChunk        NVARCHAR(3952)

DECLARE     @pos             INT
SELECT     @pos             = 1
DECLARE     @chunkSize        INT
SELECT     @chunkSize     = 3950
SELECT     @RowsInserted     = 0

-- get machine's id
DECLARE @machineId INT
EXECUTE [dbo].[getMachineId] @machine, @machineId OUTPUT

WHILE       @pos*2 <= DATALENGTH(@message)
BEGIN
    SET @messageChunk = SUBSTRING(@message, @pos, @chunkSize)

    EXECUTE [dbo].[getUniqueMessageId] @messageChunk, @uniqueMessageId OUTPUT

    -- insert the message
    INSERT INTO tRunMessages
        ( runId, messageTypeId, timestamp, escapeHtml, uniqueMessageId, machineId, threadName)
    VALUES
        ( @runId, @messageTypeId, @timestampActual, @escapeHtml, @uniqueMessageId, @machineId, @threadName)

    SET @RowsInserted = @RowsInserted + @@ROWCOUNT
        SET @pos = @pos + @chunkSize
END
GO


print 'end ALTER PROCEDURE sp_insert_run_message'
GO


print 'start CREATE PROCEDURE sp_update_checkpoint_summary'
GO


/****** Object:  StoredProcedure [dbo].[sp_update_checkpoint_summary]    Script Date: 12/2/2019 2:56:32 PM ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE  PROCEDURE [dbo].[sp_update_checkpoint_summary] 

@checkpointSummaryId INT,
@numPassed INT,
@numFailed INT,
@numRunning INT,
@minResponseTime INT,
@maxResponseTime INT,
@avgResponseTime FLOAT,
@minTransferRate FLOAT,
@maxTransferRate FLOAT,
@avgTransferRate FLOAT

AS

-- As this procedure is usually called from more more than one thread (from 1 or more ATS agents)
-- it happens that more than 1 thread enters this stored procedure at the same time and they all ask for the checkpoint summary id,
-- they all see the needed summary checkpoint is not present and they all create one. This is wrong!
-- The fix is to make sure that only 1 thread at a time executes this stored procedure and all other threads are blocked.
-- This is done by using a lock with exlusive mode. The lock is automatically released at the end of the transaction.

BEGIN TRAN UpdateCheckpointSummary
DECLARE @get_app_lock_res INT
EXEC @get_app_lock_res = sp_getapplock @Resource = 'UpdateCheckpointSummary Lock ID', @LockMode = 'Exclusive'; 

IF @get_app_lock_res < 0
    -- error getting lock
    -- client will see there was an error as @RowsInserted stays 0
    RETURN;

BEGIN

IF @numPassed = 0
    -- no passed checkpoints for the current procerude invocation
    UPDATE tCheckpointsSummary
	SET    numPassed = numPassed+@numPassed,
           numFailed = numFailed+@numFailed,
           numRunning = numRunning+@numRunning
	WHERE  checkpointSummaryId = @checkpointSummaryId;
ELSE
	UPDATE tCheckpointsSummary
	SET    numPassed = numPassed+@numPassed,
           numFailed = numFailed+@numFailed,
           numRunning = numRunning+@numRunning,
           minResponseTime = CASE WHEN @minResponseTime < minResponseTime THEN @minResponseTime ELSE minResponseTime END,
           maxResponseTime = CASE WHEN @maxResponseTime > maxResponseTime THEN @maxResponseTime ELSE maxResponseTime END,

           minTransferRate = CASE WHEN @minTransferRate < minTransferRate THEN @minTransferRate ELSE minTransferRate END,
           maxTransferRate = CASE WHEN @maxTransferRate > maxTransferRate THEN @maxTransferRate ELSE maxTransferRate END,

           avgResponseTime = ((avgResponseTime * numPassed) + (@avgResponseTime * @numPassed)) / (numPassed + @numPassed),
           avgTransferRate = ((avgTransferRate * numPassed) + (@avgTransferRate * @numPassed)) / (numPassed  +@numPassed)
	WHERE checkpointSummaryId = @checkpointSummaryId;
END 

IF @@ERROR <> 0 --error has happened
    ROLLBACK
ELSE
    COMMIT
GO

print 'end CREATE PROCEDURE sp_update_checkpoint_summary'
GO


print '#18 INTERNAL VERSION UPGRADE FOOTER - START'
GO
IF (@@ERROR != 0)
BEGIN
    RAISERROR(N'Error occurred while performing update to internal version 18', 16, 1) WITH LOG;
    RETURN;
END
UPDATE tInternal SET [value]='18' WHERE [key]='internalVersion'
GO
print '#18 INTERNAL VERSION UPGRADE FOOTER - END'
GO

print '#19 INTERNAL VERSION UPGRADE HEADER - START'
GO
INSERT INTO tInternal ([key], value) VALUES ('Upgrade_to_intVer_19', SYSDATETIME());
GO
print '#19 INTERNAL VERSION UPGRADE HEADER - END'
GO
print 'start ALTER TABLE tCheckpoints'
GO
-- BEGIN
ALTER TABLE [dbo].[tCheckpoints]
	DROP CONSTRAINT [PK_tCheckpointsAll]
GO
ALTER TABLE [dbo].[tCheckpoints]
	ALTER COLUMN [checkpointId] [bigint] NOT NULL;
GO
ALTER TABLE [dbo].[tCheckpoints]
	ADD CONSTRAINT [PK_tCheckpointsAll] PRIMARY KEY CLUSTERED (checkpointId);
GO
-- END
print 'end ALTER TABLE tCheckpoints'
GO
print 'start ALTER PROCEDURE sp_start_checkpoint'
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER   PROCEDURE [dbo].[sp_start_checkpoint]

@loadQueueId INT
,@name VARCHAR(255)
,@mode INT
,@transferRateUnit VARCHAR(50)

,@checkpointSummaryId INT =0 OUT
,@checkpointId BIGINT =0 OUT

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
print 'end ALTER PROCEDURE sp_start_checkpoint'
GO
print 'start ALTER PROCEDURE sp_end_checkpoint'
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
ALTER   PROCEDURE [dbo].[sp_end_checkpoint]

@checkpointSummaryId INT
,@checkpointId BIGINT
,@responseTime INT
,@transferSize BIGINT
,@result INT
,@mode INT
,@endTime DATETIME

,@RowsInserted INT =0 OUT

AS

DECLARE @transferRate FLOAT
IF @responseTime > 0
    -- in order to transform transferSize into float we multiply with 1000.0 instead of 1000
    SET @transferRate = @transferSize*1000.0/@responseTime
ELSE SET @transferRate = 0


-- update DETAILS table when running FULL mode - it keeps 1 row for EACH value of a checkpoint
--  @mode == 0 -> SHORT mode
--  @mode != 0 -> FULL mode
IF @mode != 0
    BEGIN
        UPDATE tCheckpoints
        SET responseTime=@responseTime, transferRate=@transferRate, result=@result, endTime=@endTime
        WHERE checkpointId = @checkpointId
    END


-- update SUMMARY table - it keeps 1 row for ALL values of a checkpoint
IF (@result = 0)
    -- checkpoint failed
    BEGIN
        UPDATE  tCheckpointsSummary
        SET     numRunning = numRunning -1,
                numFailed = numFailed + 1
        WHERE   checkpointSummaryId = @checkpointSummaryId
    END
ELSE
    -- checkpoint passed
    BEGIN
        UPDATE tCheckpointsSummary
        SET     numRunning = numRunning -1,
                numPassed = numPassed + 1,
                minResponseTime = CASE WHEN @responseTime < minResponseTime THEN @responseTime ELSE minResponseTime END,
                maxResponseTime = CASE WHEN @responseTime > maxResponseTime THEN @responseTime ELSE maxResponseTime END,
                avgResponseTime = (avgResponseTime * numPassed + @responseTime)/(numPassed + 1),
                minTransferRate = CASE WHEN @transferRate < minTransferRate THEN @transferRate ELSE minTransferRate END,
                maxTransferRate = CASE WHEN @transferRate > maxTransferRate THEN @transferRate ELSE maxTransferRate END,
                avgTransferRate = (avgTransferRate * numPassed + @transferRate)/(numPassed+ 1)
        WHERE checkpointSummaryId = @checkpointSummaryId
    END

SET @RowsInserted = @@ROWCOUNT
GO
print 'end ALTER PROCEDURE sp_end_checkpoint'
GO
print 'start ALTER PROCEDURE sp_insert_checkpoint'
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
ALTER  PROCEDURE [dbo].[sp_insert_checkpoint]

@loadQueueId INT
,@name VARCHAR(150)
,@responseTime INT
,@endTime DATETIME
,@transferSize BIGINT
,@transferRateUnit VARCHAR(50)
,@result INT
,@mode INT

AS

DECLARE @checkpointSummaryId INT=0
DECLARE @checkpointId BIGINT=0

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
    IF (@checkpointSummaryId IS NOT NULL)
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
print 'end ALTER PROCEDURE sp_insert_checkpoint'
GO

print 'start ALTER PROCEDURE sp_get_system_statistics'
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
ALTER                PROCEDURE [dbo].[sp_get_system_statistics]

@fdate varchar(150),
@testcaseIds varchar(150),
@machineIds varchar(150),
@statsTypeIds varchar(150),
@whereClause varchar(MAX)

AS

DECLARE @sql varchar(8000)
-- timestamp conversion note: 20 means yyyy-mm-dd hh:mi:ss
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
                     LEFT JOIN  tStatsTypes st ON (ss.statsTypeId = st.statsTypeId)
                     JOIN       tTestcases tt ON (tt.testcaseId = ss.testcaseId)
                     WHERE      ss.testcaseId in ( '+@testcaseIds+' )
                     AND        ss.machineId in ( '+@machineIds+' )
                     AND        st.statsTypeId IN ('+@statsTypeIds+')
                     AND '+@whereClause+' 
					 GROUP BY st.parentName, st.name, st.units, ss.timestamp, ss.value, st.statsTypeId, ss.machineId, ss.testcaseId 
					 ORDER BY ss.timestamp'


EXEC (@sql)
GO
print 'end ALTER PROCEDURE sp_get_system_statistics'
GO

print '#19 INTERNAL VERSION UPGRADE FOOTER - START'
GO
IF (@@ERROR != 0)
BEGIN
    RAISERROR(N'Error occurred while performing update to internal version 19', 16, 1) WITH LOG;
    RETURN;
END
UPDATE tInternal SET [value]='19' WHERE [key]='internalVersion'
GO
print '#19 INTERNAL VERSION UPGRADE FOOTER - END'
GO
