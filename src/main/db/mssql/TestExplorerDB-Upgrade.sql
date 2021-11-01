-- ***********************************************
-- Script for upgrade from version 4.0.9 to 4.0.10
-- ***********************************************

print '#19 INTERNAL VERSION UPGRADE HEADER - START'
GO
INSERT INTO tInternal ([key], value) VALUES ('Upgrade_to_intVer_19', SYSDATETIME());
GO
print '#19 INTERNAL VERSION UPGRADE HEADER - END'
GO

-- No changes since last release. Keep internal version

print '#19 INTERNAL VERSION UPGRADE FOOTER - START'
GO
IF (@@ERROR != 0)
BEGIN
    RAISERROR(N'Error occurred while performing update to internal version 19', 16, 1) WITH LOG;
    RETURN;
END
UPDATE tInternal SET [value]='19' WHERE [key]='internalVersion';
GO
print '#19 INTERNAL VERSION UPGRADE FOOTER - END'
GO

