-- ***********************************************
-- Script for upgrade from version 4.0.5 to 4.0.6.
-- ***********************************************

print '#16 INTERNAL VERSION UPGRADE HEADER - START'
GO
INSERT INTO tInternal ([key], value) VALUES ('Upgrade_to_intVer_16', SYSDATETIME());
GO
print '#16 INTERNAL VERSION UPGRADE HEADER - END'
GO

print 'start CREATE TABLE tTestcaseMetainfo'
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE TABLE [dbo].[tTestcaseMetainfo](
  [metainfoId] [int] IDENTITY(1,1) NOT NULL,
  [testcaseId] [int] NOT NULL,
  [name] [varchar](50) NOT NULL,
  [value] [varchar](512) NOT NULL,
 CONSTRAINT [PK_tTestcaseMetainfo] PRIMARY KEY CLUSTERED
(
    [metainfoId] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
CREATE NONCLUSTERED INDEX IX_tTestcaseMetainfo_ByTestcase ON [dbo].[tTestcaseMetainfo]
(
    [testcaseId]
) WITH( STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
ALTER TABLE [dbo].[tTestcaseMetainfo]  WITH CHECK ADD  CONSTRAINT [FK_tTestcaseMetainfo_tTestcases] FOREIGN KEY([testcaseId])
REFERENCES [dbo].[tTestcases] ([testcaseId])
ON UPDATE CASCADE
ON DELETE CASCADE
GO
print 'end CREATE TABLE tTestcaseMetainfo'
GO

print 'start CREATE PROCEDURE  sp_add_testcase_metainfo'
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE          PROCEDURE [dbo].[sp_add_testcase_metainfo]

@testcaseId INT
,@metaKey VARCHAR(50)
,@metaValue VARCHAR(50)

,@RowsInserted INT =0 OUT

AS

INSERT INTO tTestcaseMetainfo VALUES(@testcaseId, @metaKey, @metaValue)

SET @RowsInserted = @@ROWCOUNT
GO

print 'end CREATE PROCEDURE  sp_add_testcase_metainfo'
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
