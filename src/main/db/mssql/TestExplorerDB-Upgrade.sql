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
