print '#<internal_version_number> INTERNAL VERSION UPGRADE HEADER - START'
GO
INSERT INTO tInternal ([key], value) VALUES ('Upgrade_to_intVer_<internal_version_number>', SYSDATETIME());
GO
print '#<internal_version_number> INTERNAL VERSION UPGRADE HEADER - END'
GO

print 'start <sql_action> procedure <procedure_name>'
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
<sql_action>  PROCEDURE [dbo].[<procedure_name>]

 ...

END

IF @@ERROR <> 0 --error has happened
    ROLLBACK
ELSE
    COMMIT
GO

print 'end <sql_action> procedure <procedure_name>'
GO

print '#<internal_version_number> INTERNAL VERSION UPGRADE FOOTER - START'
GO
IF (@@ERROR != 0)
BEGIN
    RAISERROR(N'Error occurred while performing update to internal version <internal_version_number>', 16, 1)  WITH LOG;
    RETURN;
END
UPDATE tInternal SET [value]='<internal_version_number>' WHERE [key]='internalVersion'
GO
print '#<internal_version_number> INTERNAL VERSION UPGRADE FOOTER - END'
GO
