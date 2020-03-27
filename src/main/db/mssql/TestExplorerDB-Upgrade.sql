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
