-- ***********************************************
-- Script for upgrade from version 4.0.6 to 4.1.0.
-- ***********************************************

print '#16 INTERNAL VERSION UPGRADE HEADER - START'
GO
INSERT INTO tInternal ([key], value) VALUES ('Upgrade_to_intVer_16', SYSDATETIME());
GO
print '#16 INTERNAL VERSION UPGRADE HEADER - END'
GO

print 'start CREATE procedure sp_insert_checkpoints'
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE PROCEDURE [dbo].[sp_insert_checkpoints]
@query varchar(MAX)
AS
BEGIN
	BEGIN TRAN InsertCheckpointsTransaction
		DECLARE @get_app_lock_res INT
		EXEC @get_app_lock_res = sp_getapplock @Resource = 'InsertCheckpointsTransaction Lock ID', @LockMode = 'Exclusive';
		IF @get_app_lock_res < 0
		BEGIN
			-- error getting lock
			-- client will see there was an error as @RowsInserted stays 0
			RETURN;
		END
		BEGIN
			EXEC(@query)
		END
		IF @@ERROR <> 0 --error has happened
    	BEGIN
			ROLLBACK
		END
		ELSE
		BEGIN
    		COMMIT
    	END
END
GO

IF @@ERROR <> 0 --error has happened
    ROLLBACK
ELSE
    COMMIT
GO

print 'end CREATE procedure sp_insert_checkpoints'
GO

print '#16 INTERNAL VERSION UPGRADE FOOTER - START'
GO
IF (@@ERROR != 0)
BEGIN
    RAISERROR(N'Error occurred while performing update to internal version 16', 16, 1)  WITH LOG;
    RETURN;
END
UPDATE tInternal SET [value]='16' WHERE [key]='internalVersion'
GO
print '#16 INTERNAL VERSION UPGRADE FOOTER - END'
GO