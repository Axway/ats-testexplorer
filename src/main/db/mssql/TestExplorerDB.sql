GO
/****** Object:  User [AtsUser] ******/
CREATE USER [AtsUser] FOR LOGIN [AtsUser] WITH DEFAULT_SCHEMA=[AtsUser]
GO
/****** Object:  Schema [AtsUser] ******/
CREATE SCHEMA [AtsUser] AUTHORIZATION [AtsUser]
GO

/****** Object:  Table [dbo].[tInternal] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[tInternal](
    [id] [int] IDENTITY(1,1) NOT NULL,
    [key] [varchar](50) NOT NULL,
    [value] [varchar](256) NOT NULL,
 CONSTRAINT [PK_tSystem] PRIMARY KEY CLUSTERED
(
    [id] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY],
 CONSTRAINT [IX_tSystem] UNIQUE NONCLUSTERED
(
    [key] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO

/****** Record the version ******/
INSERT INTO tInternal ([key],[value]) VALUES ('version', '4.0.6_draft')
GO

/****** Record the initial version ******/
INSERT INTO tInternal ([key],[value]) VALUES ('initialVersion', '16')
GO

/****** Record the internal version ******/
INSERT INTO tInternal ([key],[value]) VALUES ('internalVersion', '16')
GO

INSERT INTO tInternal ([key], value) VALUES ('Install_of_intVer_16', SYSDATETIME());
GO

/****** Object:  StoredProcedure [dbo].[stringArrayIntoTable] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE PROCEDURE [dbo].[stringArrayIntoTable]
@inputString VARCHAR(8000),
@delimiter VARCHAR(10),
@tablename SYSNAME

AS

BEGIN
    DECLARE @spot SMALLINT, @str VARCHAR(8000), @sql VARCHAR(8000)

    WHILE @inputString <> ''
    BEGIN
        SET @spot = CHARINDEX(@delimiter, @inputString)
        IF @spot>0
            BEGIN
                SET @str = LEFT(@inputString, @spot-1)
                SET @inputString = RIGHT(@inputString, LEN(@inputString)-@spot)
            END
        ELSE
            BEGIN
                SET @str = @inputString
                SET @inputString = ''
            END
        SET @sql = 'INSERT INTO '+@tablename+' VALUES('''+CONVERT(VARCHAR(100),@str)+''')'
        EXEC(@sql)
    END
END
GO
/****** Object:  Table [dbo].[tRuns]    Script Date: 04/11/2011 20:46:20 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[tRuns](
    [runId] [int] IDENTITY(1,1) NOT NULL,
    [productName] [varchar](50) NOT NULL,
    [versionName] [varchar](50) NOT NULL,
    [buildName] [varchar](50) NOT NULL,
    [runName] [varchar](255) NOT NULL,
    [OS] [varchar](50) NULL,
    [dateStart] [datetime] NOT NULL,
    [dateEnd] [datetime] NULL,
    [userNote] [varchar](255) NULL,
    [hostName] [varchar] (255) NULL,
 CONSTRAINT [PK_tRuns] PRIMARY KEY CLUSTERED
(
    [runId] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Table [dbo].[tSuites]    Script Date: 04/11/2011 20:46:21 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[tSuites](
    [suiteId] [int] IDENTITY(1,1) NOT NULL,
    [runId] [int] NOT NULL,
    [name] [varchar](255) NOT NULL,
    [dateStart] [datetime] NOT NULL,
    [dateEnd] [datetime] NULL,
    [userNote] [varchar](255) NULL,
    [package] [varchar](255) NULL,
 CONSTRAINT [PK_tSuites] PRIMARY KEY CLUSTERED
(
    [suiteId] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX IX_tSuites_ByRun ON [dbo].[tSuites]
(
    [runId]
) WITH( STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
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
CREATE TABLE [dbo].[tScenarios](
    [scenarioId] [int] IDENTITY(1,1) NOT NULL,
    [name] [varchar](255) NOT NULL,
    fullName VARCHAR(1000) NOT NULL UNIQUE,
    [description] [varchar](4000) NULL,
    [userNote] [varchar](255) NULL,
 CONSTRAINT [PK_tPerformanceScenario] PRIMARY KEY CLUSTERED
(
    [scenarioId] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Table [dbo].[tTestcases]    Script Date: 04/11/2011 20:46:21 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[tTestcases](
    [testcaseId] [int] IDENTITY(1,1) NOT NULL,
    [scenarioId] [int] NOT NULL,
    [suiteId] [int] NOT NULL,
    [name] [varchar](255) NOT NULL,
    [dateStart] [datetime] NOT NULL,
    [dateEnd] [datetime] NULL,
    [result] [tinyint] NULL,
    [userNote] [varchar](255) NULL,
 CONSTRAINT [PK_tTestcases] PRIMARY KEY CLUSTERED
(
    [testcaseId] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX IX_tTestcases_BySuite ON [dbo].[tTestcases]
(
    [suiteId]
) WITH( STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
ALTER TABLE [dbo].[tTestcases] WITH CHECK ADD CONSTRAINT [FK_tTestcases_tSuites] FOREIGN KEY([suiteId])
REFERENCES [dbo].[tSuites] ([suiteId])
ON UPDATE CASCADE
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[tTestcases] CHECK CONSTRAINT [FK_tTestcases_tSuites]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Table [dbo].[tMachines]    Script Date: 04/11/2011 20:46:20 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[tMachines](
    [machineId] [int] IDENTITY(1,1) NOT NULL,
    [machineName] [varchar](255) NOT NULL,
    [machineAlias] [varchar](100) NULL,
    [machineInfo] [ntext] NULL,
 CONSTRAINT [PK_tMachines] PRIMARY KEY CLUSTERED
(
    [machineId] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Table [dbo].[tRunMetainfo]    Script Date: 01/07/2016 10:10:10 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tRunMetainfo](
  [metainfoId] [int] IDENTITY(1,1) NOT NULL,
  [runId] [int] NOT NULL,
  [name] [varchar](50) NOT NULL,
  [value] [varchar](512) NOT NULL,
 CONSTRAINT [PK_tRunMetainfo] PRIMARY KEY CLUSTERED
(
    [metainfoId] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX IX_tRunMetainfo_ByRun ON [dbo].[tRuns]
(
    [runId]
) WITH( STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
ALTER TABLE [dbo].[tRunMetainfo]  WITH CHECK ADD  CONSTRAINT [FK_tRunMetainfo_tRuns] FOREIGN KEY([runId])
REFERENCES [dbo].[tRuns] ([runId])
ON UPDATE CASCADE
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[tRunMetainfo] CHECK CONSTRAINT [FK_tRunMetainfo_tRuns]
GO
/****** Object:  Table [dbo].[tScenarioMetainfo]    Script Date: 01/07/2016 10:10:10 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tScenarioMetainfo](
  [metainfoId] [int] IDENTITY(1,1) NOT NULL,
  [scenarioId] [int] NOT NULL,
  [name] [varchar](50) NOT NULL,
  [value] [varchar](512) NOT NULL,
 CONSTRAINT [PK_tScenarioMetainfo] PRIMARY KEY CLUSTERED
(
    [metainfoId] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
CREATE NONCLUSTERED INDEX IX_tScenarioMetainfo_ByScenario ON [dbo].[tScenarioMetainfo]
(
    [scenarioId]
) WITH( STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
ALTER TABLE [dbo].[tScenarioMetainfo]  WITH CHECK ADD  CONSTRAINT [FK_tScenarioMetainfo_tScenarios] FOREIGN KEY([scenarioId])
REFERENCES [dbo].[tScenarios] ([scenarioId])
ON UPDATE CASCADE
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[tScenarioMetainfo] CHECK CONSTRAINT [FK_tScenarioMetainfo_tScenarios]
GO
/****** Object:  Table [dbo].[tTestcaseMetainfo]    Script Date: 30/04/2019 10:50:22 ******/
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
/****** Object:  Table [dbo].[tSytemProperties]    Script Date: 04/11/2011 20:46:21 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[tSytemProperties](
    [id] [int] NOT NULL,
    [runId] [int] NOT NULL,
    [machineId] [int] NOT NULL,
    [key] [varchar](50) NOT NULL,
    [value] [varchar](256) NULL,
 CONSTRAINT [PK_tSytemProperties] PRIMARY KEY CLUSTERED
(
    [id] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Table [dbo].[tMessageTypes]    Script Date: 04/11/2011 20:46:20 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[tMessageTypes](
    [messageTypeId] [int] NOT NULL,
    [name] [varchar](50) NULL,
 CONSTRAINT [PK_tLogTypes] PRIMARY KEY CLUSTERED
(
    [messageTypeId] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Table [dbo].[tUniqueMessages]    Script Date: 04/11/2011 20:46:21 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[tUniqueMessages](
    [uniqueMessageId] [int] IDENTITY(1,1) NOT NULL,
    [hash] [binary](20) NOT NULL,
    [message] [nvarchar](3950) NULL,
 CONSTRAINT [PK_tMessages] PRIMARY KEY CLUSTERED
(
    [uniqueMessageId] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY],
 CONSTRAINT [IX_tMessages] UNIQUE NONCLUSTERED
(
    [hash] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Table [dbo].[tRunMessages] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tRunMessages](
  [runMessageId][int] NOT NULL IDENTITY(1,1) PRIMARY KEY,
  [runId] [int] NOT NULL,
  [messageTypeId] [int] NOT NULL,
  [timestamp] [datetime] NOT NULL,
  [escapeHtml] [tinyint] NOT NULL,
  [uniqueMessageId] [int] NOT NULL,
  [machineId] [int] NOT NULL,
  [threadName] [varchar](255) NOT NULL
) ON [PRIMARY]
GO
ALTER TABLE dbo.tRunMessages WITH CHECK ADD CONSTRAINT FK_tRunMessages_tMessageTypes FOREIGN KEY( messageTypeId) 
REFERENCES tMessageTypes(messageTypeId)
GO
ALTER TABLE dbo.tRunMessages WITH CHECK ADD CONSTRAINT FK_tRunMessages_tUniqueMessages FOREIGN KEY( uniqueMessageId) 
REFERENCES tUniqueMessages(uniqueMessageId)
GO
ALTER TABLE dbo.tRunMessages WITH CHECK ADD CONSTRAINT FK_tRunMessages_tMachines FOREIGN KEY( machineId)
REFERENCES tMachines(machineId)
GO
/****** Object:  Table [dbo].[tSuiteMessages] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tSuiteMessages](
    [suiteMessageId] [int] NOT NULL IDENTITY(1,1) PRIMARY KEY,
  [suiteId] [int] NOT NULL,
  [messageTypeId] [int] NOT NULL,
  [timestamp] [datetime] NOT NULL,
  [escapeHtml] [tinyint] NOT NULL,
  [uniqueMessageId] [int] NOT NULL,
  [machineId] [int] NOT NULL,
  [threadName] [varchar](255) NOT NULL
) ON [PRIMARY]
GO
ALTER TABLE dbo.tSuiteMessages WITH CHECK ADD CONSTRAINT FK_tSuiteMessages_tMessageTypes FOREIGN KEY( messageTypeId) 
REFERENCES tMessageTypes(messageTypeId)
GO
ALTER TABLE dbo.tSuiteMessages WITH CHECK ADD CONSTRAINT FK_tSuiteMessages_tUniqueMessages FOREIGN KEY( uniqueMessageId) 
REFERENCES tUniqueMessages(uniqueMessageId)
GO
ALTER TABLE dbo.tSuiteMessages WITH CHECK ADD CONSTRAINT FK_tSuiteMessages_tMachines FOREIGN KEY( machineId)
REFERENCES tMachines(machineId)
GO
/****** Object:  Table [dbo].[tMessages]    Script Date: 04/11/2011 20:46:20 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[tMessages](
    [messageId] [int] IDENTITY(1,1) NOT NULL,
    [testcaseId] [int] NULL,
    [messageTypeId] [int] NOT NULL,
    [timestamp] [datetime] NOT NULL,
    [escapeHtml] [tinyint] NULL,
    [uniqueMessageId] [int] NOT NULL,
    [machineId] [int] NOT NULL,
    [threadName] [varchar](255) NOT NULL,
    [parentMessageId] [int],
 CONSTRAINT [PK_tPerformanceLogs] PRIMARY KEY CLUSTERED
(
    [messageId] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY],
 CONSTRAINT [IX_tPerformanceLogs] UNIQUE NONCLUSTERED
(
    [testcaseId] ASC,
    [messageId] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX IX_tMessages_parentMessageId ON dbo.tMessages    (
    [parentMessageId]
) WITH( STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Table [dbo].[tStatsTypes]    Script Date: 04/11/2011 20:46:21 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[tStatsTypes](
    [statsTypeId] [int] IDENTITY(1,1) NOT NULL,
    [name] [varchar](255) NOT NULL,
    [units] [varchar](50) NOT NULL,
    [params] [varchar](500) NULL,
    [parentName] [varchar](255) default '' NOT NULL,
    [internalName] [varchar](255) default '' NOT NULL,
 CONSTRAINT [PK_tStatsTypes] PRIMARY KEY CLUSTERED
(
    [statsTypeId] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Table [dbo].[tSystemStats]    Script Date: 04/11/2011 20:46:21 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tSystemStats](
    [systemStatsId] [int] IDENTITY(1,1) NOT NULL,
    [testcaseId] [int] NOT NULL,
    [machineId] [int] NOT NULL,
    [statsTypeId] [int] NOT NULL,
    [timestamp] [datetime] NOT NULL,
    [value] [float] NOT NULL,
 CONSTRAINT [PK_tSystemStats] PRIMARY KEY CLUSTERED
(
    [systemStatsId] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [IX_tSystemStats_testcaseId] ON [dbo].[tSystemStats]
(
    [testcaseId] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tLoadQueues]    Script Date: 04/11/2011 20:46:20 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[tLoadQueues](
    [loadQueueId] [int] IDENTITY(1,1) NOT NULL,
    [testcaseId] [int] NOT NULL,
    [name] [varchar](255) NOT NULL,
    [sequence] [int] NULL,
    [hostsList] [varchar](255) NULL,
    [threadingPattern] [varchar](255) NOT NULL,
    [numberThreads] [int] NOT NULL,
    [machineId] [int] NOT NULL,
    [dateStart] [datetime] NOT NULL,
    [dateEnd] [datetime] NULL,
    [result] [tinyint] NOT NULL,
    [userNote] [varchar](255) NULL,
 CONSTRAINT [PK_tLoadQueues] PRIMARY KEY CLUSTERED
(
    [loadQueueId] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX IX_tLoadQueues_byTestcaseId ON dbo.tLoadQueues
(
    [testcaseId]
) WITH( STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Table [dbo].[tCheckpointsSummary]    Script Date: 04/11/2011 20:46:20 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[tCheckpointsSummary](
    [checkpointSummaryId] [int] IDENTITY(1,1) NOT NULL,
    [loadQueueId] [int] NOT NULL,
    [name] [varchar](150) NOT NULL,
    [numRunning] [int] NULL,
    [numPassed] [int] NULL,
    [numFailed] [int] NULL,
    [minResponseTime] [int] NOT NULL,
    [maxResponseTime] [int] NOT NULL,
    [avgResponseTime] [float] NOT NULL,
    [minTransferRate] [float] NOT NULL,
    [maxTransferRate] [float] NOT NULL,
    [avgTransferRate] [float] NOT NULL,
    [transferRateUnit] [varchar](50) NULL,
 CONSTRAINT [PK_tCheckpoints] PRIMARY KEY CLUSTERED
(
    [checkpointSummaryId] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Table [dbo].[tCheckpoints]    Script Date: 04/11/2011 20:46:20 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[tCheckpoints](
    [checkpointId] [int] IDENTITY(1,1) NOT NULL,
    [checkpointSummaryId] [int] NOT NULL,
    [name] [varchar](150) NOT NULL,
    [responseTime] [int] NOT NULL,
    [transferRate] [float] NOT NULL,
    [transferRateUnit] [varchar](50) NULL,
    [result] [tinyint] NOT NULL,
    [endTime] [datetime] NULL,
 CONSTRAINT [PK_tCheckpointsAll] PRIMARY KEY CLUSTERED
(
    [checkpointId] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX IX_CheckpointSummaryId
ON [dbo].[tCheckpoints] (checkpointSummaryId)
INCLUDE (name, responseTime, endTime, result);
SET ANSI_PADDING ON
GO

/****** Object:  Table [dbo].[tColumnDefinition] ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
CREATE TABLE [dbo].[tColumnDefinition](
    [ID] [int] IDENTITY(1,1) NOT NULL,
    [columnName] [varchar](50) NOT NULL,
    [columnPosition] [smallint] NOT NULL,
    [columnLength] [smallint] NOT NULL,
    [parentTable] [varchar](10) NOT NULL,
    [isVisible] [bit] NOT NULL,
 CONSTRAINT [PK_column_definition] PRIMARY KEY CLUSTERED
(
    [ID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  StoredProcedure [dbo].[sp_get_testcases_count]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************

CREATE       PROCEDURE [dbo].[sp_get_testcases_count]

@WhereClause VARCHAR(1000)

AS

SET ARITHABORT OFF

DECLARE @sql_1 varchar(8000)
SET        @sql_1 = ' SELECT count(*) as testcasesCount FROM tTestcases ' + @WhereClause;
EXEC     (@sql_1)

SET ARITHABORT ON
GO
/****** Object:  StoredProcedure [dbo].[sp_get_testcases]    Script Date: 08/11/2017 17:30:48 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE  PROCEDURE [dbo].[sp_get_testcases]

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
GO
/****** Object:  StoredProcedure [dbo].[sp_get_system_statistics]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE                PROCEDURE [dbo].[sp_get_system_statistics]

@fdate varchar(150),
@testcaseIds varchar(150),
@machineIds varchar(150),
@statsTypeIds varchar(150)

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
                     LEFT JOIN tStatsTypes st ON (ss.statsTypeId = st.statsTypeId)
                     JOIN    tTestcases tt ON (tt.testcaseId = ss.testcaseId)
             WHERE       ss.testcaseId in ( '+@testcaseIds+' )
                         AND ss.machineId in ( '+@machineIds+' )
                         AND st.statsTypeId IN ('+@statsTypeIds+')
             GROUP BY    st.parentName, st.name, st.units, ss.timestamp, ss.value, st.statsTypeId, ss.machineId, ss.testcaseId
             ORDER BY    ss.timestamp'

EXEC (@sql)
GO
/****** Object:  StoredProcedure [dbo].[sp_get_system_statistic_descriptions]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE       PROCEDURE [dbo].[sp_get_system_statistic_descriptions]


@fdate varchar(150),
@WhereClause varchar(1000)

AS

DECLARE @sql varchar(8000)

SET     @sql = 'SELECT  tt.testcaseId, tt.name as testcaseName,
						DATEDIFF(second, CONVERT( datetime, ''' + @fdate + ''', 20), MIN(ss.timestamp)) as testcaseStarttime,
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
                GROUP BY tt.testcaseId, tt.name, m.machineId, m.machineName, m.machineAlias, st.name, st.params, st.parentName, st.internalName, ss.statsTypeId, st.units
                ORDER BY st.name';
                
EXEC (@sql)
GO

/****** Object:  StoredProcedure [dbo].[sp_get_number_of_checkpoints_per_queue]    Script Date: 10/30/2017 11:19:46 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE   PROCEDURE [dbo].[sp_get_number_of_checkpoints_per_queue]

@testcaseIds varchar(150)

AS

DECLARE @sql varchar(8000)
-- this procedure returns map with queue name as a key and number of checkpoints as a value
SET @sql = 
'SELECT
	 tLoadQueues.name,
     COUNT(tLoadQueues.name) as numberOfQueue
     FROM tCheckpoints
     INNER JOIN tCheckpointsSummary on (tCheckpointsSummary.checkpointSummaryId = tCheckpoints.checkpointSummaryId)
     INNER JOIN tLoadQueues on (tLoadQueues.loadQueueId = tCheckpointsSummary.loadQueueId)
WHERE tLoadQueues.testcaseId in ( '+@testcaseIds+' )
group by tLoadQueues.testcaseId, tLoadQueues.name';
EXEC (@sql)
GO

/****** Object:  StoredProcedure [dbo].[sp_get_suites_count]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************

CREATE       PROCEDURE [dbo].[sp_get_suites_count]

@WhereClause VARCHAR(1000)

AS

SET ARITHABORT OFF

DECLARE @sql_1 varchar(8000)
SET        @sql_1 = ' SELECT count(*) as suitesCount FROM tSuites ' + @WhereClause;
EXEC     (@sql_1)

SET ARITHABORT ON
GO
/****** Object:  StoredProcedure [dbo].[sp_get_suites]    Script Date: 26/06/2012 10:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE        PROCEDURE [dbo].[sp_get_suites]

@StartRecord VARCHAR(100)
, @RecordsCount VARCHAR(100)
, @WhereClause VARCHAR(1000)
, @SortCol VARCHAR(100)
, @SortType VARCHAR(100)

AS

DECLARE @tmpSuites TABLE
(
    [suiteId] [int] NOT NULL,
    [runId] [int] NOT NULL,
    [name] [varchar](255) NOT NULL,
    [testcasesTotal] [int] NOT NULL,
    [testcasesFailed] [int] NOT NULL,
    [testcasesPassedPercent] [float] NOT NULL,
    [testcaseIsRunning] [bit] NOT NULL,
    [scenariosTotal] [int] NOT NULL,
    [scenariosFailed] [int] NOT NULL,
    [scenariosSkipped] [int] NOT NULL,
    [dateStart] [datetime] NOT NULL,
    [dateEnd] [datetime] NULL,
    [duration] [int] NOT NULL,
    [userNote] [varchar](255) NULL,
    [package] [varchar](255) NULL
)

DECLARE @tmpTestcases TABLE
(
    [testcaseId] [int] NOT NULL,
    [scenarioId] [int] NOT NULL,
    [result] [int] NOT NULL
)

DECLARE @testcasesTotal INT
DECLARE @testcasesFailed INT
DECLARE @testcasesSkipped INT
DECLARE @testcaseIsRunning INT

DECLARE @scenariosTotal INT
DECLARE @scenariosFailed INT
DECLARE @scenariosSkipped INT

DECLARE @suiteId INT
DECLARE @fetchStatus INT = 0

EXEC ('DECLARE suitesCursor CURSOR FOR
            SELECT tr.suiteId FROM (
                SELECT    suiteId, ROW_NUMBER() OVER (ORDER BY ' + @SortCol + ' ' + @SortType + ') AS Row
                FROM tSuites '
                + @WhereClause + ' ) as tr
            WHERE tr.Row >= ' + @StartRecord + ' AND tr.Row <= ' + @RecordsCount)
OPEN suitesCursor

WHILE 0 = @fetchStatus
    BEGIN
        FETCH NEXT FROM suitesCursor INTO @suiteId
        SET @fetchStatus = @@FETCH_STATUS
        IF 0 = @fetchStatus
            BEGIN
        -- cache the testcases
        INSERT INTO @tmpTestcases SELECT testcaseId, scenarioId, result FROM tTestcases WHERE suiteId IN (SELECT suiteId=@suiteId)

        -- data about all testcases in this suite
                SET @testcasesTotal     = (SELECT COUNT(testcaseId) FROM @tmpTestcases )
                SET @testcasesFailed    = (SELECT COUNT(testcaseId) FROM @tmpTestcases WHERE result=0)
                SET @testcasesSkipped   = (SELECT COUNT(testcaseId) FROM @tmpTestcases WHERE result=2)
                SET @testcaseIsRunning  = (SELECT COUNT(testcaseId) FROM @tmpTestcases WHERE result=4)

        -- data about all distinct scenarios in this run
        SET @scenariosTotal   = (select COUNT(DISTINCT(scenarioId)) from @tmpTestcases)
        SET @scenariosFailed  = (select COUNT(DISTINCT(scenarioId)) from @tmpTestcases where result=0)
        SET @scenariosSkipped = (select COUNT(DISTINCT(scenarioId)) from @tmpTestcases where result=2)

                INSERT INTO @tmpSuites
                SELECT  tSuites.suiteId,
                		tSuites.runId,
                        tSuites.name,

                        @testcasesTotal,
                        @testcasesFailed,
                        (@testcasesTotal - @testcasesFailed - @testcasesSkipped) * 100.00 / CASE WHEN @testcasesTotal=0 THEN 1 ELSE @testcasesTotal END  AS testcasesPassedPercent,
                        @testcaseIsRunning AS testcaseIsRunning,

                        @scenariosTotal,
                        @scenariosFailed,
                        @scenariosSkipped,

                        tSuites.dateStart,
                        tSuites.dateEnd,

                        CASE WHEN tSuites.dateEnd IS NULL
                            THEN datediff(second, tSuites.dateStart, GETDATE() )
                            ELSE datediff(second, tSuites.dateStart, tSuites.dateEnd )
                        END AS duration,

                        tSuites.userNote,
                        tSuites.package
                FROM    tSuites
                WHERE    suiteId=@suiteId

                DELETE FROM @tmpTestcases -- cleanup the temp testcases table
            END
    END
CLOSE suitesCursor
DEALLOCATE suitesCursor

select * from @tmpSuites
GO

/****** Object:  StoredProcedure [dbo].[sp_get_scenarios_count]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************

CREATE       PROCEDURE [dbo].[sp_get_scenarios_count]

@WhereClause VARCHAR(1000)

AS

SET ARITHABORT OFF

EXEC     ('SELECT COUNT(DISTINCT(scenarioId)) AS scenariosCount FROM tTestcases ' + @WhereClause)

SET ARITHABORT ON
GO
/****** Object:  StoredProcedure [dbo].[sp_get_scenarios]    Script Date: 08/10/2017 17:34:57 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE        PROCEDURE [dbo].[sp_get_scenarios]

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
/****** Object:  StoredProcedure [dbo].[sp_get_runs_count]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************

CREATE       PROCEDURE [dbo].[sp_get_runs_count]

@WhereClause VARCHAR(1000)

AS

SET ARITHABORT OFF

DECLARE @sql_1 varchar(8000)
SET        @sql_1 = ' SELECT count(*) as runsCount FROM tRuns ' + @WhereClause;
EXEC     (@sql_1)

SET ARITHABORT ON
GO
/****** Object:  StoredProcedure [dbo].[sp_get_runs]    Script Date: 26/06/2012 10:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE       PROCEDURE [dbo].[sp_get_runs]

@StartRecord VARCHAR(100)
, @RecordsCount VARCHAR(100)
, @WhereClause VARCHAR(1000)
, @SortCol VARCHAR(100)
, @SortType VARCHAR(100)

AS

SET ARITHABORT OFF

DECLARE @tmpRuns TABLE
(
    [runId] [int] NOT NULL,
    [productName] [varchar](50) NOT NULL,
    [versionName] [varchar](50) NOT NULL,
    [buildName] [varchar](50) NOT NULL,
    [runName] [varchar](255) NOT NULL,
    [OS] [varchar](50) NULL,
    [hostName] [varchar](255) NULL,
    [testcasesTotal] [int] NOT NULL,
    [testcasesFailed] [int] NOT NULL,
    [testcasesPassedPercent] [float] NOT NULL,
    [testcaseIsRunning] [bit] NOT NULL,
    [scenariosTotal] [int] NOT NULL,
    [scenariosFailed] [int] NOT NULL,
    [scenariosSkipped] [int] NOT NULL,
    [dateStart] [datetime] NOT NULL,
    [dateEnd] [datetime] NULL,
    [duration] [int] NOT NULL,
    [userNote] [varchar](255) NULL
)

DECLARE @tmpTestcases TABLE
(
    [testcaseId] [int] NOT NULL,
    [scenarioId] [int] NOT NULL,
    [result] [int] NOT NULL
)

DECLARE @testcasesTotal INT
DECLARE @testcasesFailed INT
DECLARE @testcasesSkipped INT
DECLARE @testcaseIsRunning INT

DECLARE @scenariosTotal INT
DECLARE @scenariosFailed INT
DECLARE @scenariosSkipped INT

DECLARE @runId INT
DECLARE @fetchStatus INT = 0

EXEC ('DECLARE runsCursor CURSOR FOR
            SELECT tr.runId FROM (
                SELECT    runId, ROW_NUMBER() OVER (ORDER BY ' + @SortCol + ' ' + @SortType + ') AS Row
                FROM tRuns '
                + @WhereClause + ' ) as tr
            WHERE tr.Row >= ' + @StartRecord + ' AND tr.Row <= ' + @RecordsCount)
OPEN runsCursor

WHILE 0 = @fetchStatus
    BEGIN
        FETCH NEXT FROM runsCursor INTO @runId
        SET @fetchStatus = @@FETCH_STATUS
        IF 0 = @fetchStatus
            BEGIN
        -- cache the testcases
        INSERT INTO @tmpTestcases SELECT testcaseId, scenarioId, result FROM tTestcases WHERE suiteId IN (SELECT suiteId FROM tSuites WHERE runId=@runId)

        -- data about all testcases in this run
                SET @testcasesTotal       = (SELECT COUNT(testcaseId) FROM @tmpTestcases )
                SET @testcasesFailed	  = (SELECT COUNT(testcaseId) FROM @tmpTestcases WHERE result=0)
                SET @testcasesSkipped     = (SELECT COUNT(testcaseId) FROM @tmpTestcases WHERE result=2)
                SET @testcaseIsRunning    = (SELECT COUNT(testcaseId) FROM @tmpTestcases WHERE result=4)

        -- data about all disctinct scenarios in this run
        SET @scenariosTotal   = (select COUNT(DISTINCT(scenarioId)) from @tmpTestcases)
        SET @scenariosFailed  = (select COUNT(DISTINCT(scenarioId)) from @tmpTestcases where result=0)
        SET @scenariosSkipped = (select COUNT(DISTINCT(scenarioId)) from @tmpTestcases where result=2)

                INSERT INTO @tmpRuns
                SELECT  tRuns.runId,
                        tRuns.productName,
                        tRuns.versionName,
                        tRuns.buildName,
                        tRuns.runName,
                        tRuns.OS,
                        tRuns.hostName,

                        @testcasesTotal,
                        @testcasesFailed,
                        (@testcasesTotal - @testcasesFailed - @testcasesSkipped) * 100.00 / CASE WHEN @testcasesTotal=0 THEN 1 ELSE @testcasesTotal END  AS testcasesPassedPercent,
                        @testcaseIsRunning,

                        @scenariosTotal,
                        @scenariosFailed,
                        @scenariosSkipped,

                        tRuns.dateStart,
                        tRuns.dateEnd,

                        CASE WHEN tRuns.dateEnd IS NULL
                            THEN datediff(second, tRuns.dateStart, GETDATE() )
                            ELSE datediff(second, tRuns.dateStart, tRuns.dateEnd )
                        END AS duration,

                        tRuns.userNote

                FROM    tRuns
                WHERE   runId=@runId

                DELETE FROM @tmpTestcases -- cleanup the temp testcases table
            END
    END
CLOSE runsCursor
DEALLOCATE runsCursor

select * from @tmpRuns

SET ARITHABORT ON
GO
/****** Object:  StoredProcedure [dbo].[sp_get_navigation_for_testcases]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE                 PROCEDURE [dbo].[sp_get_navigation_for_testcases]

@suiteId varchar(30)

AS

EXEC (	'select  tRuns.runId, tRuns.runName, tScenarios.scenarioId, tSuites.name as suiteName, tScenarios.name as scenarioName
			from tTestcases
			inner join tScenarios on (tScenarios.scenarioId = tTestcases.scenarioId)
			inner join tSuites on (tSuites.suiteId = tTestcases.suiteId)
			inner join tRuns on (tSuites.runId = tRuns.runId)
			where tTestcases.suiteId = ' + @suiteId)
GO
/****** Object:  StoredProcedure [dbo].[sp_get_navigation_for_testcase]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE                 PROCEDURE [dbo].[sp_get_navigation_for_testcase]

@testcaseId varchar(30)

AS

EXEC('	select	tr.runId,
        tr.runName,
        tt.suiteId,
        ts.name as suiteName,
        tss.scenarioId,
        tss.name as scenarioName,
        tt.name as testcaseName,
        tt.dateStart,
        tt.dateEnd
    from tTestcases tt
    inner join tScenarios tss on (tt.scenarioId = tss.scenarioId)
    inner join tSuites ts on (ts.suiteId = tt.suiteId)
    inner join tRuns tr on (ts.runId = tr.runId)
    where tt.testcaseId = ' + @testcaseId);
GO
/****** Object:  StoredProcedure [dbo].[sp_get_navigation_for_suites]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE                 PROCEDURE [dbo].[sp_get_navigation_for_suites]

@runId varchar(30)

AS

DECLARE @sql varchar(200)
SET     @sql = 'select runName from tRuns where runId = ' + @runId;

EXEC (@sql)
GO
/****** Object:  StoredProcedure [dbo].[sp_get_navigation_for_scenarios]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE                 PROCEDURE [dbo].[sp_get_navigation_for_scenarios]

@suiteId varchar(30)

AS

DECLARE @sql varchar(8000)
SET     @sql = 'select tr.runId, tr.runName, ts.name as suiteName from tSuites ts
    inner join tRuns tr on (ts.runId = tr.runId) where ts.suiteId = ' + @suiteId;

EXEC (@sql)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE                 PROCEDURE [dbo].[sp_get_specific_testcase_id]

@currentTestcaseId VARCHAR(30)
, @runName VARCHAR(300)
, @suiteName VARCHAR(300)
, @scenarioName VARCHAR(300)
, @testName VARCHAR(500)
, @mode INT

AS

DECLARE @gtOrLt varchar(5)
DECLARE @ascOrDesc varchar(5)

/*
 mode = 1  ->  next testcase id
 mode = 2  ->  previous testcase id
 mode = 3  ->  last testcase id
*/

IF @mode = 1
    BEGIN
        SET @gtOrLt = '>'
        SET @ascOrDesc = 'asc'
    END
ELSE IF @mode = 2
    BEGIN
        SET @gtOrLt = '<'
        SET @ascOrDesc = 'desc'
    END
ELSE IF @mode = 3
    BEGIN
        SET @gtOrLt = '>'
        SET @ascOrDesc = 'desc'
    END

DECLARE @sql nvarchar(4000)
SET     @sql = 'select top 1 tt.testcaseId from tTestcases tt
    inner join tScenarios tss on (tt.scenarioId = tss.scenarioId)
    inner join tSuites ts on (ts.suiteId = tt.suiteId)
    inner join tRuns tr on (ts.runId = tr.runId)
    where tr.runName = ''' + @runName + ''' and ts.name = '''+ @suiteName + ''' and tss.name = ''' + @scenarioName
             + ''' and tt.name = ''' + @testName + ''' and tt.testcaseId ' + @gtOrLt + ' ' + @currentTestcaseId + '
    order by tt.testcaseId ' + @ascOrDesc;

EXEC (@sql)
GO
/****** Object:  StoredProcedure [dbo].[sp_get_messages_count]    Script Date: 01/13/2015 14:18:57 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************

CREATE       PROCEDURE [dbo].[sp_get_messages_count]

@WhereClause VARCHAR(7000)

AS

SET ARITHABORT OFF

DECLARE @sql_1 varchar(8000)
SET @sql_1 = 'SELECT COUNT (DISTINCT (CASE WHEN m.parentMessageId IS NULL THEN m.messageId ELSE m.parentMessageId END)) as messagesCount FROM tMessages m
                                LEFT JOIN tMessageTypes mt ON m.messageTypeId = mt.messageTypeId
                                JOIN tUniqueMessages umsg ON m.uniqueMessageId = umsg.uniqueMessageId
                                JOIN tMachines mach ON m.machineId = mach.machineId ' + @WhereClause;
EXEC (@sql_1)

SET ARITHABORT ON
GO
/****** Object:  StoredProcedure [dbo].[sp_get_messages]    Script Date: 01/13/2015 14:18:57 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE  PROCEDURE [dbo].[sp_get_messages]

@StartRecord VARCHAR(100)
, @RecordsCount VARCHAR(100)
, @WhereClause VARCHAR(7000)
, @SortCol VARCHAR(100)
, @SortType VARCHAR(100)

AS

DECLARE @tmpMessages TABLE
(
[messageId]            [INT],
[timestamp]            [DATETIME],
[threadName]        [NVARCHAR](250),
[machineName]        [NVARCHAR](250),
[message]            [NVARCHAR](3950),
[typeName]            [VARCHAR](10),
[parentMessageId]    [INT],
[row]                [INT]
)

DECLARE @sql varchar(8000) =
    'SELECT * FROM
        ( SELECT  m.messageId,
                        m.timestamp,
                        m.threadName,
                        CASE
                            WHEN mach.machineAlias is NULL OR DATALENGTH(mach.machineAlias) = 0 THEN mach.machineName
                            ELSE mach.machineAlias
                        END as machineName,
                        CONVERT(NTEXT, umsg.message) as message,
                        mt.name AS typeName,
                        m.parentMessageId,
                        ROW_NUMBER() OVER (ORDER BY ' + @SortCol + ' ' + @SortType + ') AS row
                        FROM tMessages m
                            LEFT JOIN tMessageTypes mt ON m.messageTypeId = mt.messageTypeId
                            JOIN tUniqueMessages umsg ON m.uniqueMessageId = umsg.uniqueMessageId
                            JOIN tMachines mach ON m.machineId = mach.machineId
                        WHERE m.messageId in (SELECT DISTINCT (CASE WHEN m.parentMessageId IS NULL THEN m.messageId ELSE m.parentMessageId END) FROM tMessages m
                                LEFT JOIN tMessageTypes mt ON m.messageTypeId = mt.messageTypeId
                                JOIN tUniqueMessages umsg ON m.uniqueMessageId = umsg.uniqueMessageId
                                JOIN tMachines mach ON m.machineId = mach.machineId
                        ' + @WhereClause + ')) as r
            WHERE r.row >= ' + @StartRecord + ' AND r.row <= ' + @RecordsCount;

INSERT INTO @tmpMessages
EXEC (@sql)

INSERT INTO @tmpMessages
SELECT
  m.messageId,
        m.timestamp,
        m.threadName,
        CASE
            WHEN mach.machineAlias is NULL OR DATALENGTH(mach.machineAlias) = 0 THEN mach.machineName
            ELSE mach.machineAlias
        END AS machineName,
        CONVERT(NTEXT, umsg.message) AS message,
        mt.name AS typeName,
        m.parentMessageId,
        -1 AS row
 FROM tMessages m
  LEFT JOIN tMessageTypes mt ON m.messageTypeId = mt.messageTypeId
  JOIN tUniqueMessages umsg ON m.uniqueMessageId = umsg.uniqueMessageId
  JOIN tMachines mach ON m.machineId = mach.machineId
 WHERE (m.parentMessageId > 0
          AND m.parentMessageId IN (SELECT tm.parentMessageId FROM @tmpMessages tm)
          AND m.messageId NOT IN (SELECT tm.messageId FROM @tmpMessages tm))

-- Java code relies on that order
SELECT * FROM @tmpMessages
    ORDER BY
        CASE WHEN @SortCol = 'timestamp' AND @SortType = 'ASC' THEN timestamp END,
        CASE WHEN @SortCol = 'timestamp' AND @SortType = 'DESC' THEN timestamp END DESC,
        CASE WHEN @SortCol = 'threadName' AND @SortType = 'ASC' THEN threadName END,
        CASE WHEN @SortCol = 'threadName' AND @SortType = 'DESC' THEN threadName END DESC,
        CASE WHEN @SortCol = 'machineName' AND @SortType = 'ASC' THEN machineName END,
        CASE WHEN @SortCol = 'machineName' AND @SortType = 'DESC' THEN machineName END DESC,
        CASE WHEN @SortCol = 'message' AND @SortType = 'ASC' THEN message END,
        CASE WHEN @SortCol = 'message' AND @SortType = 'DESC' THEN message END DESC,
        messageId;
GO
/****** Object:  StoredProcedure [dbo].[sp_get_loadqueues]    Script Date: 04/11/2011 20:46:19 ******/
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
/****** Object:  StoredProcedure [dbo].[sp_get_checkpoints_summary]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE  PROCEDURE [dbo].[sp_get_checkpoints_summary]

@WhereClause VARCHAR(500)
, @SortCol VARCHAR(100)
, @SortType VARCHAR(100)

AS

DECLARE @sql_1 varchar(8000)
SET        @sql_1 =  'SELECT * FROM tCheckpointsSummary ' + @WhereClause + ' ORDER BY ' + @SortCol + ' ' + @SortType

EXEC     (@sql_1)
GO
/****** Object:  StoredProcedure [dbo].[sp_get_checkpoints]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE  PROCEDURE [dbo].[sp_get_checkpoints]

@PerPage VARCHAR(100)
, @RowsCount VARCHAR(100)
, @WhereClause VARCHAR(1000)
, @SortCol VARCHAR(100)
, @SortType VARCHAR(100)

AS

DECLARE @SortTypeRevert    VARCHAR(10)
SET @SortTypeRevert        = CASE WHEN @SortType = 'asc' THEN 'desc' ELSE 'asc' END

IF OBJECT_ID('tempdb..#TmpCheckpoints') IS NOT NULL DROP TABLE #TmpCheckpoints

DECLARE @sql1 varchar(8000)
SET        @sql1 =  'SELECT    *
                    INTO     #TmpCheckpoints
                    FROM     tCheckpoints cp
                    WHERE     ' + @WhereClause

DECLARE @sql2 varchar(8000)
SET        @sql2 =  'SELECT      *
                    FROM       #TmpCheckpoints
                    WHERE      checkpointId IN (
                                    SELECT  TOP ' + @PerPage + '  cp.checkpointId
                                    FROM    (
                                            SELECT TOP ' + @RowsCount + ' *
                                            FROM #TmpCheckpoints
                                            ORDER BY ' + @SortCol + ' ' + @SortType + '
                                            ) cp
                                    ORDER BY ' + @SortCol + ' ' + @SortTypeRevert + '
                                    )
                    ORDER BY ' + @SortCol + ' ' + @SortType

EXEC     (@sql1 + @sql2)
GO
/****** Object:  StoredProcedure [dbo].[getAutoDate]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE  PROCEDURE [dbo].[getAutoDate]
@indate DATETIME,
@outdate DATETIME OUT

AS
BEGIN

IF (@indate IS NULL)
BEGIN
SET @outdate = GETDATE()
END
ELSE
SET @outdate = CONVERT(DATETIME,@indate)

END
GO
/****** Object:  StoredProcedure [dbo].[sp_delete_run]    Script Date: 09/26/2016 16:19:57 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE       PROCEDURE [dbo].[sp_delete_run]

@runIds VARCHAR(1000)

AS

DECLARE @delimiter VARCHAR(10) =',' -- the used delimiter

BEGIN
    DECLARE
        @idIndex SMALLINT,
        @idToken VARCHAR(100)

    WHILE @runIds <> ''
    BEGIN
        SET @idIndex = CHARINDEX(@delimiter, @runIds)

        IF @idIndex > 0
            BEGIN
                SET @idToken = LEFT(@runIds, @idIndex-1)
                SET @runIds = RIGHT(@runIds, LEN(@runIds)-@idIndex)
            END
        ELSE
            BEGIN
                SET @idToken = @runIds
                SET @runIds = ''
            END

       DELETE FROM tRunMessages WHERE tRunMessages.runId = @idToken
       DELETE FROM tSuiteMessages WHERE tSuiteMessages.suiteId IN (SELECT suiteId FROM tSuites WHERE tSuites.runId=@idToken);

      DELETE FROM tRuns WHERE runId = @idToken;

    -- Delete any possible orphan messages in tUniqueMessages. This is used as some kind of cleanup
    -- DELETE FROM tUniqueMessages WHERE tUniqueMessages.uniqueMessageId NOT IN ( SELECT tMessages.uniqueMessageId FROM tMessages );

    END

END
GO

/****** Object:  StoredProcedure [dbo].[sp_delete_suite]    Script Date: 09/26/2016 16:31:55 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE      PROCEDURE [dbo].[sp_delete_suite]

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

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE     PROCEDURE [dbo].[sp_delete_scenario]

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
GO
/****** Object:  StoredProcedure [dbo].[getUniqueMessageId]    Script Date: 10/28/2011 16:09:30 ******/
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
/****** Object:  StoredProcedure [dbo].[getStatsTypeId]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE  PROCEDURE [dbo].[getStatsTypeId]

@parentName VARCHAR(255),
@internalName VARCHAR(255),
@statsName VARCHAR(255),
@statsUnit VARCHAR(50),
@statsParams VARCHAR(500),

@statsTypeId INT OUT

AS

BEGIN

SET @statsTypeId    = (SELECT statsTypeId FROM tStatsTypes WHERE parentName = @parentName AND internalName = @internalName AND name = @statsName AND units = @statsUnit AND params = @statsParams)

-- insert the stats type if not already there
IF (@statsTypeId IS NULL)
    BEGIN
        INSERT INTO tStatsTypes (parentName, internalName, name, units, params) VALUES  (@parentName, @internalName, @statsName, @statsUnit, @statsParams)
         SET @statsTypeId = SCOPE_IDENTITY()
    END
END
GO
/****** Object:  StoredProcedure [dbo].[getMachineId]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE  PROCEDURE [dbo].[getMachineId]
@machine VARCHAR(255)
,@machineId INT OUT

AS

BEGIN

SET @machineId    = (SELECT machineId FROM tMachines WHERE machineName = @machine)

-- insert the machine if not already there
IF (@machineId IS NULL)
    BEGIN
        INSERT INTO tMachines (machineName) VALUES  (@machine)
         SET @machineId = SCOPE_IDENTITY()
    END
END
GO
/****** Object:  StoredProcedure [dbo].[sp_end_run]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE       PROCEDURE [dbo].[sp_end_run]

@runId INT
,@dateEnd DATETIME

,@RowsUpdated INT =0 OUT

AS

DECLARE
@dateEndActual DATETIME
EXECUTE [dbo].[getAutoDate] @dateEnd, @dateEndActual OUTPUT

UPDATE  tRuns SET dateEnd = @dateEndActual WHERE  runId = @runId
SET @RowsUpdated = @@ROWCOUNT
GO
/****** Object:  StoredProcedure [dbo].[sp_update_run]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE       PROCEDURE [dbo].[sp_update_run]

@runId INT,
@productName VARCHAR(50),
@versionName VARCHAR(50),
@buildName VARCHAR(50),
@runName VARCHAR(255),
@osName VARCHAR(50),
@userNote VARCHAR(255),
@hostName VARCHAR(255),

@RowsUpdated INT =0 OUT

AS

DECLARE
@currentProductName VARCHAR(50),
@currentVersionName VARCHAR(50),
@currentBuildName VARCHAR(50),
@currentRunName VARCHAR(255),
@currentOsName VARCHAR(50),
@currentUserNote VARCHAR(255),
@currentHostName VARCHAR(255)

-- get the current values
SELECT    @currentProductName = productName,
        @currentVersionName = versionName,
        @currentBuildName    = buildName,
        @currentRunName        = runName,
        @currentOsName        = OS,
        @currentUserNote    = userNote,
        @currentHostName    = hostName
FROM    tRuns
WHERE    runId = @runId

-- update the Run info, if the provided value is null, then use the current value
UPDATE tRuns
SET        productName =    CASE
                            WHEN @productName IS NULL THEN @currentProductName
                            ELSE @productName
                        END,
        versionName =    CASE
                            WHEN @versionName IS NULL  THEN @currentVersionName
                            ELSE @versionName
                        END,
        buildName =        CASE
                            WHEN @buildName IS NULL THEN @currentBuildName
                            ELSE @buildName
                        END,
        runName =        CASE
                            WHEN @runName IS NULL THEN @currentRunName
                            ELSE @runName
                        END,
        OS =            CASE
                            WHEN @osName IS NULL THEN @currentOsName
                            ELSE @osName
                        END,
        userNote =        CASE
                            WHEN @userNote IS NULL THEN @currentUserNote
                            ELSE @userNote
                        END,
        hostName =        CASE
                            WHEN @hostName IS NULL THEN @currentHostName
                            ELSE @hostName
                        END
WHERE tRuns.runId=@runId

SET @RowsUpdated = @@ROWCOUNT
GO
/****** Object:  StoredProcedure [dbo].[sp_start_run]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE       PROCEDURE [dbo].[sp_start_run]

@productName VARCHAR(50)
,@versionName VARCHAR(50)
,@buildName VARCHAR(50)
,@runName VARCHAR(255)
,@OSName VARCHAR(50)
,@dateStart DATETIME
,@hostName VARCHAR(255)

,@RowsInserted INT =0 OUT
,@RunID INT    =0 OUT


AS

DECLARE
@dateStartActual DATETIME

EXECUTE [dbo].[getAutoDate] @dateStart,@dateStartActual  OUTPUT

INSERT INTO tRuns (productName, versionName, buildName, runName, OS, dateStart , hostName)
VALUES (@productName, @versionName, @buildName, @runName, @OSName, @dateStartActual , @hostName)
SET @RunID = @@IDENTITY
SET @RowsInserted = @@ROWCOUNT
GO
/****** Object:  StoredProcedure [dbo].[sp_add_run_metainfo]    Script Date: 01/07/2011 10:10:10 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE          PROCEDURE [dbo].[sp_add_run_metainfo]

@runId INT
,@metaKey VARCHAR(50)
,@metaValue VARCHAR(50)

,@RowsInserted INT =0 OUT

AS
  INSERT INTO tRunMetainfo VALUES(@runId, @metaKey, @metaValue)
SET @RowsInserted = @@ROWCOUNT
GO
/****** Object:  StoredProcedure [dbo].[sp_update_suite]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE       PROCEDURE [dbo].[sp_update_suite]

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
/****** Object:  StoredProcedure [dbo].[sp_populate_system_statistic_definition]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE            PROCEDURE [dbo].[sp_populate_system_statistic_definition]

@parentName VARCHAR(255),
@internalName VARCHAR(255),
@name VARCHAR(255),
@unit VARCHAR(50),
@params VARCHAR(500),

@statisticId INT  OUT

AS

BEGIN

EXECUTE [dbo].[getStatsTypeId] @parentName, @internalName, @name, @unit, @params, @statisticId OUTPUT

END
GO
/****** Object:  StoredProcedure [dbo].[sp_start_suite]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE          PROCEDURE [dbo].[sp_start_suite]

@suiteName VARCHAR(255)
,@runId INT
,@package VARCHAR(255)
,@dateStart DATETIME


,@RowsInserted INT  OUT
,@suiteId INT     OUT



AS

DECLARE
@dateStartActual DATETIME
EXECUTE [dbo].[getAutoDate]    @dateStart ,@dateStartActual  OUTPUT

INSERT INTO  tSuites (name, runId, dateStart, package)
VALUES (@suiteName, @runId, @dateStartActual, @package)

SET @suiteId = @@IDENTITY
SET @RowsInserted = @@ROWCOUNT
GO
/****** Object:  StoredProcedure [dbo].[sp_insert_system_statistic_by_ids]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE            PROCEDURE [dbo].[sp_insert_system_statistic_by_ids]

@testcaseId INT,
@machine VARCHAR(255),
@statisticIds VARCHAR(1000),
@statisticValues VARCHAR(8000),
@timestamp DATETIME


AS

-- get machine's id
DECLARE @machineId INT
EXECUTE [dbo].[getMachineId] @machine, @machineId OUTPUT

-- prepare some constants
DECLARE @delimiter VARCHAR(10) ='_', -- the used delimiter
@testcaseIdString VARCHAR(10) = CONVERT(VARCHAR(10),@testcaseId), -- testcase id to a string
@machineIdString VARCHAR(10) = CONVERT(VARCHAR(10),@machineId), -- machine id to a string
@timestampString VARCHAR(30) = CONVERT(VARCHAR(30),@timestamp,20) -- timestamp to a string

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

        SET @sql =    'INSERT INTO tSystemStats VALUES (' +
                        @testcaseIdString + ',' +
                        @machineIdString + ',' +
                        @idToken + ', ' +
                        'CONVERT(DATETIME,''' + @timestampString + ''',20), ' +
                        @valueToken +
                        ')'
        EXEC (@sql)
    END
    -- TODO we should return the number of inserted statistics and check this number in the calling java code
END
GO
/****** Object:  StoredProcedure [dbo].[sp_insert_user_activity_statistic_by_ids]  ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE            PROCEDURE [dbo].[sp_insert_user_activity_statistic_by_ids]

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
    -- TODO we should return the number of inserted statistics and check this number in the calling java code
END

IF @@ERROR <> 0 --error has happened
    ROLLBACK
ELSE
    COMMIT
GO
/****** Object:  StoredProcedure [dbo].[sp_end_suite]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
/****** Object:  StoredProcedure [dbo].[sp_end_suite]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE        PROCEDURE [dbo].[sp_end_suite]

@suiteId INT
,@dateEnd DATETIME

,@RowsUpdated INT =0 OUT

AS

DECLARE
@dateEndActual DATETIME
EXECUTE [dbo].[getAutoDate] @dateEnd, @dateEndActual OUTPUT

UPDATE  tSuites SET dateEnd = @dateEndActual WHERE  suiteId = @suiteId
SET @RowsUpdated = @@ROWCOUNT
GO
/****** Object:  StoredProcedure [dbo].[sp_clear_scenario_metainfo]    Script Date: 07/14/2016 15:51:37 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

--*********************************************************
CREATE          PROCEDURE [dbo].[sp_clear_scenario_metainfo]

@testcaseId INT

,@RowsDeleted INT =0 OUT

AS

DECLARE @scenarioId INT
SET @scenarioId = (SELECT scenarioId FROM tTestcases WHERE testcaseId=@testcaseId)
IF (@scenarioId > 0)
    BEGIN
    DELETE FROM tScenarioMetainfo WHERE scenarioId=@scenarioId
    SET @RowsDeleted = @@ROWCOUNT
    END
GO
/****** Object:  StoredProcedure [dbo].[sp_add_scenario_metainfo]    Script Date: 07/27/2016 17:37:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

--*********************************************************
CREATE          PROCEDURE [dbo].[sp_add_scenario_metainfo]

@testcaseId INT
,@metaKey VARCHAR(50)
,@metaValue VARCHAR(50)

,@RowsInserted INT =0 OUT

AS

DECLARE @scenarioId INT = (SELECT scenarioId FROM tTestcases WHERE testcaseId=@testcaseId)

INSERT INTO tScenarioMetainfo VALUES(@scenarioId, @metaKey, @metaValue)

SET @RowsInserted = @@ROWCOUNT
GO

/****** Object:  StoredProcedure [dbo].[sp_update_scenario]    Script Date: 04/11/2011 20:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE       PROCEDURE [dbo].[sp_update_scenario]

@scenarioId INT,
@userNote VARCHAR(255),

@RowsUpdated INT =0 OUT

AS

UPDATE    tScenarios
SET        userNote=@userNote
WHERE    tScenarios.scenarioId=@scenarioId

SET @RowsUpdated = @@ROWCOUNT
GO
/****** Object:  StoredProcedure [dbo].[sp_update_testcase]    Script Date: 10/16/2017 13:26:39 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE       PROCEDURE [dbo].[sp_update_testcase]

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
/****** Object:  StoredProcedure [dbo].[sp_update_machine_info]    Script Date: 12/15/2011 14:19:50 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- *********************************************************
CREATE        PROCEDURE [dbo].[sp_update_machine_info]

@machine VARCHAR(255),
@machineInfo NTEXT,

@RowsUpdated INT =0 OUT

AS

-- get machine's id
DECLARE @machineId INT
EXECUTE [dbo].[getMachineId] @machine, @machineId OUTPUT

-- update the machine description
UPDATE tMachines SET machineInfo=@machineInfo WHERE machineId=@machineId

SET @RowsUpdated = @@ROWCOUNT
GO
/****** Object:  StoredProcedure [dbo].[sp_end_testcase]    Script Date: 04/11/2011 20:46:19 ******/
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
/****** Object:  StoredProcedure [dbo].[sp_start_testcase]    Script Date: 26/06/2012 10:46:19 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE          PROCEDURE [dbo].[sp_start_testcase]

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

/****** Object:  StoredProcedure [dbo].[sp_delete_testcase]    Script Date: 03/19/2012 17:05:20 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE       PROCEDURE [dbo].[sp_delete_testcase]

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

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE       PROCEDURE [dbo].[sp_change_testcase_state]

@scenarioIds VARCHAR(255),
@testcaseIds VARCHAR(500),
@result int

AS

DECLARE @sql nvarchar(4000)
IF @testcaseIds IS null
    BEGIN
        SET @sql =  N'UPDATE tTestcases SET result = ' + cast(@result as varchar) + ' WHERE testcaseId IN ( SELECT tt.testcaseId
                        FROM tTestcases tt WHERE tt.scenarioId in ( ' + @scenarioIds + ' ) );'
        EXEC  (@sql)
    END
ELSE
    BEGIN
        SET @sql =  N'UPDATE tTestcases SET result = ' + cast(@result as varchar) + ' WHERE testcaseId IN ( ' + @testcaseIds + ' );'
        EXEC  (@sql)

        DECLARE @scenarioId int
        DECLARE @ParmDefinition nvarchar(50);
        SET @ParmDefinition = N'@scenarioIdOut int OUTPUT';
        SET @sql = N'SELECT @scenarioIdOut = scenarioId FROM tTestcases WHERE testcaseId in ( ' + @testcaseIds + ' )'

        EXECUTE sp_executesql @sql, @ParmDefinition, @scenarioIdOut=@scenarioId OUTPUT
    END
GO
/****** Object:  StoredProcedure [dbo].[sp_add_testcase_metainfo]    Script Date: 30/04/2019 10:53:35 ******/
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
/****** Object:  StoredProcedure [dbo].[sp_insert_message]    Script Date: 10/13/2014 17:54:12 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- *********************************************************
CREATE        PROCEDURE [dbo].[sp_insert_message]

@testcaseId INT
,@messageTypeId INT
,@message NTEXT
,@escapeHtml INT =1
,@machine VARCHAR(255)
,@threadName VARCHAR(255)
,@timestamp DATETIME

,@RowsInserted INT =0 OUT

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

/****** Object:  StoredProcedure [dbo].[sp_insert_run_message]    Script Date: 09/26/2016 18:46:28 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- *********************************************************
CREATE        PROCEDURE [dbo].[sp_insert_run_message]

@runId INT
,@messageTypeId INT
,@message NTEXT
,@escapeHtml INT =1
,@machine VARCHAR(255)
,@threadName VARCHAR(255)
,@timestamp DATETIME

,@RowsInserted INT =0 OUT

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
/****** Object:  StoredProcedure [dbo].[sp_insert_suite_message]    Script Date: 09/26/2016 18:47:48 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- *********************************************************
CREATE        PROCEDURE [dbo].[sp_insert_suite_message]

@suiteId INT
,@messageTypeId INT
,@message NTEXT
,@escapeHtml INT =1
,@machine VARCHAR(255)
,@threadName VARCHAR(255)
,@timestamp DATETIME

,@RowsInserted INT =0 OUT

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

/****** Object:  StoredProcedure [dbo].[sp_get_run_messages]    Script Date: 09/26/2016 18:02:47 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE  PROCEDURE [dbo].[sp_get_run_messages]

@StartRecord VARCHAR(100)
, @RecordsCount VARCHAR(100)
, @WhereClause VARCHAR(7000)
, @SortCol VARCHAR(100)
, @SortType VARCHAR(100)

AS

DECLARE @sql varchar(8000)
SET @sql = 'SELECT * from (
                SELECT  m.runmessageId,
                        m.timestamp,
                        m.threadName,
                        CASE
                            WHEN mach.machineAlias is NULL OR DATALENGTH(mach.machineAlias) = 0 THEN mach.machineName
                            ELSE mach.machineAlias
                        END as machineName,
                        CONVERT(NTEXT, umsg.message) as message,
                        mt.name AS typeName,
                        ROW_NUMBER() OVER (ORDER BY ' + @SortCol + ' ' + @SortType + ') AS row
                        FROM tRunMessages m
                            LEFT JOIN tMessageTypes mt ON m.messageTypeId = mt.messageTypeId
                            JOIN tUniqueMessages umsg ON m.uniqueMessageId = umsg.uniqueMessageId
                            JOIN tMachines mach ON m.machineId = mach.machineId
                         WHERE runMessageId in(
                                SELECT runMessageId FROM tRunMessages
                                ' + @WhereClause + ')) as r
            WHERE r.row >= ' + @StartRecord + ' AND r.row <= ' + @RecordsCount;
            print @sql

EXEC (@sql)
GO

/****** Object:  StoredProcedure [dbo].[sp_get_run_messages_count]    Script Date: 09/26/2016 18:04:10 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************

CREATE       PROCEDURE [dbo].[sp_get_run_messages_count]

@WhereClause VARCHAR(7000)

AS

SET ARITHABORT OFF

DECLARE @sql_1 varchar(8000)
SET        @sql_1 = 'SELECT count(*) as messagesCount FROM tRunMessages m
                        LEFT JOIN tMessageTypes mt  ON m.messageTypeId = mt.messageTypeId
                        JOIN    tUniqueMessages umsg   ON m.uniqueMessageId = umsg.uniqueMessageId
                        JOIN    tMachines mach  ON m.machineId = mach.machineId
                       WHERE runMessageId in( SELECT runMessageId FROM tRunMessages  '+ @WhereClause +' )';
EXEC     (@sql_1)

SET ARITHABORT ON
GO


/****** Object:  StoredProcedure [dbo].[sp_get_suite_messages]    Script Date: 09/26/2016 18:37:11 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE  PROCEDURE [dbo].[sp_get_suite_messages]

@StartRecord VARCHAR(100)
, @RecordsCount VARCHAR(100)
, @WhereClause VARCHAR(7000)
, @SortCol VARCHAR(100)
, @SortType VARCHAR(100)

AS

DECLARE @sql varchar(8000)
SET @sql = 'SELECT * from (
                SELECT  m.suitemessageId,
                        m.timestamp,
                        m.threadName,
                        CASE
                            WHEN mach.machineAlias is NULL OR DATALENGTH(mach.machineAlias) = 0 THEN mach.machineName
                            ELSE mach.machineAlias
                        END as machineName,
                        CONVERT(NTEXT, umsg.message) as message,
                        mt.name AS typeName,
                        ROW_NUMBER() OVER (ORDER BY ' + @SortCol + ' ' + @SortType + ') AS row
                        FROM tSuiteMessages m
                            LEFT JOIN tMessageTypes mt ON m.messageTypeId = mt.messageTypeId
                            JOIN tUniqueMessages umsg ON m.uniqueMessageId = umsg.uniqueMessageId
                            JOIN tMachines mach ON m.machineId = mach.machineId
                         WHERE suiteMessageId in(
                                SELECT suiteMessageId FROM tSuiteMessages
                                ' + @WhereClause + ')) as r
            WHERE r.row >= ' + @StartRecord + ' AND r.row <= ' + @RecordsCount;
EXEC (@sql)
GO


/****** Object:  StoredProcedure [dbo].[sp_get_suite_messages_count]    Script Date: 09/26/2016 18:38:46 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************

CREATE       PROCEDURE [dbo].[sp_get_suite_messages_count]

@WhereClause VARCHAR(7000)

AS

SET ARITHABORT OFF

DECLARE @sql_1 varchar(8000)
SET        @sql_1 = 'SELECT count(*) as messagesCount FROM tSuiteMessages m
                        LEFT JOIN tMessageTypes mt  ON m.messageTypeId = mt.messageTypeId
                        JOIN    tUniqueMessages umsg   ON m.uniqueMessageId = umsg.uniqueMessageId
                        JOIN    tMachines mach  ON m.machineId = mach.machineId
                       WHERE suiteMessageId in( SELECT suiteMessageId FROM tSuiteMessages ' + @WhereClause +' )';
EXEC     (@sql_1)

SET ARITHABORT ON
GO


/****** Object:  StoredProcedure [dbo].[sp_end_loadqueue]    Script Date: 04/11/2011 20:46:19 ******/
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
GO
/****** Object:  StoredProcedure [dbo].[sp_populate_checkpoint_summary]    Script Date: 08/24/2017 15:09:22 ******/
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
--*********************************************************
CREATE   PROCEDURE [dbo].[sp_start_checkpoint]

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
--*********************************************************
CREATE   PROCEDURE [dbo].[sp_end_checkpoint]

@checkpointSummaryId INT
,@checkpointId INT
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

/****** Object:  StoredProcedure [dbo].[sp_insert_checkpoint]    Script Date: 05/31/2012 11:18:42 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE  PROCEDURE [dbo].[sp_insert_checkpoint]

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

/****** Object:  StoredProcedure [dbo].[sp_get_checkpoint_statistics]    Script Date: 06/28/2011 16:05:37 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE   PROCEDURE [dbo].[sp_get_checkpoint_statistics]

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
    c.name as queueName,
    tt.testcaseId
     FROM tCheckpoints ch
     INNER JOIN tCheckpointsSummary chs on (chs.checkpointSummaryId = ch.checkpointSummaryId)
     INNER JOIN tLoadQueues c on (c.loadQueueId = chs.loadQueueId)
     INNER JOIN tTestcases tt on (tt.testcaseId = c.testcaseId)
WHERE tt.testcaseId in ( '+@testcaseIds+' ) AND ch.name in ( '+@checkpointNames+' ) AND  c.name in ( '+@parentNames+' ) AND ch.result = 1 AND ch.endTime IS NOT NULL
ORDER BY ch.endTime';

EXEC (@sql)
GO
/****** Object:  StoredProcedure [dbo].[sp_get_checkpoint_statistic_descriptions]    Script Date: 06/27/2011 10:19:48 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
--*********************************************************
CREATE                PROCEDURE [dbo].[sp_get_checkpoint_statistic_descriptions]

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

/****** Object:  StoredProcedure [dbo].[sp_db_cleanup]     ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE PROCEDURE [dbo].[sp_db_cleanup]

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

/****** Object:  Default [DF_tSystemStats_machineId]    Script Date: 04/11/2011 20:46:21 ******/
ALTER TABLE [dbo].[tSystemStats] ADD  CONSTRAINT [DF_tSystemStats_machineId]  DEFAULT ((0)) FOR [machineId]
GO
/****** Object:  ForeignKey [FK_tCheckpoints_tCheckpointsSummary]    Script Date: 04/11/2011 20:46:20 ******/
ALTER TABLE [dbo].[tCheckpoints]  WITH CHECK ADD  CONSTRAINT [FK_tCheckpoints_tCheckpointsSummary] FOREIGN KEY([checkpointSummaryId])
REFERENCES [dbo].[tCheckpointsSummary] ([checkpointSummaryId])
ON UPDATE CASCADE
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[tCheckpoints] CHECK CONSTRAINT [FK_tCheckpoints_tCheckpointsSummary]
GO
/****** Object:  ForeignKey [FK_tCheckpointsSummary_tLoadQueues]    Script Date: 04/11/2011 20:46:20 ******/
ALTER TABLE [dbo].[tCheckpointsSummary]  WITH CHECK ADD  CONSTRAINT [FK_tCheckpointsSummary_tLoadQueues] FOREIGN KEY([loadQueueId])
REFERENCES [dbo].[tLoadQueues] ([loadQueueId])
ON UPDATE CASCADE
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[tCheckpointsSummary] CHECK CONSTRAINT [FK_tCheckpointsSummary_tLoadQueues]
GO
/****** Object:  ForeignKey [FK_tLoadQueues_tTestcases]    Script Date: 04/11/2011 20:46:20 ******/
ALTER TABLE [dbo].[tLoadQueues]  WITH CHECK ADD  CONSTRAINT [FK_tLoadQueues_tTestcases] FOREIGN KEY([testcaseId])
REFERENCES [dbo].[tTestcases] ([testcaseId])
ON UPDATE CASCADE
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[tLoadQueues] CHECK CONSTRAINT [FK_tLoadQueues_tTestcases]
GO
/****** Object:  ForeignKey [FK_tMessages_tMachines]    Script Date: 04/11/2011 20:46:20 ******/
ALTER TABLE [dbo].[tMessages]  WITH CHECK ADD  CONSTRAINT [FK_tMessages_tMachines] FOREIGN KEY([machineId])
REFERENCES [dbo].[tMachines] ([machineId])
GO
ALTER TABLE [dbo].[tMessages] CHECK CONSTRAINT [FK_tMessages_tMachines]
GO
/****** Object:  ForeignKey [FK_tMessages_tMessageTypes]    Script Date: 04/11/2011 20:46:20 ******/
ALTER TABLE [dbo].[tMessages]  WITH CHECK ADD  CONSTRAINT [FK_tMessages_tMessageTypes] FOREIGN KEY([messageTypeId])
REFERENCES [dbo].[tMessageTypes] ([messageTypeId])
GO
ALTER TABLE [dbo].[tMessages] CHECK CONSTRAINT [FK_tMessages_tMessageTypes]
GO
/****** Object:  ForeignKey [FK_tMessages_tTestcases]    Script Date: 04/11/2011 20:46:20 ******/
ALTER TABLE [dbo].[tMessages]  WITH CHECK ADD  CONSTRAINT [FK_tMessages_tTestcases] FOREIGN KEY([testcaseId])
REFERENCES [dbo].[tTestcases] ([testcaseId])
ON UPDATE CASCADE
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[tMessages] CHECK CONSTRAINT [FK_tMessages_tTestcases]
GO
/****** Object:  ForeignKey [FK_tMessages_tUniqueMessages1]    Script Date: 04/11/2011 20:46:20 ******/
ALTER TABLE [dbo].[tMessages]  WITH CHECK ADD  CONSTRAINT [FK_tMessages_tUniqueMessages1] FOREIGN KEY([uniqueMessageId])
REFERENCES [dbo].[tUniqueMessages] ([uniqueMessageId])
GO
ALTER TABLE [dbo].[tMessages] CHECK CONSTRAINT [FK_tMessages_tUniqueMessages1]
GO
/****** Object:  ForeignKey [FK_tSuites_tRuns]    Script Date: 04/11/2011 20:46:21 ******/
ALTER TABLE [dbo].[tSuites]  WITH CHECK ADD  CONSTRAINT [FK_tSuites_tRuns] FOREIGN KEY([runId])
REFERENCES [dbo].[tRuns] ([runId])
ON UPDATE CASCADE
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[tSuites] CHECK CONSTRAINT [FK_tSuites_tRuns]
GO
/****** Object:  ForeignKey [FK_tSystemStats_tMachines]    Script Date: 04/11/2011 20:46:21 ******/
ALTER TABLE [dbo].[tSystemStats]  WITH CHECK ADD  CONSTRAINT [FK_tSystemStats_tMachines] FOREIGN KEY([machineId])
REFERENCES [dbo].[tMachines] ([machineId])
GO
ALTER TABLE [dbo].[tSystemStats] CHECK CONSTRAINT [FK_tSystemStats_tMachines]
GO
/****** Object:  ForeignKey [FK_tSystemStats_tStatsTypes]    Script Date: 04/11/2011 20:46:21 ******/
ALTER TABLE [dbo].[tSystemStats]  WITH CHECK ADD  CONSTRAINT [FK_tSystemStats_tStatsTypes] FOREIGN KEY([statsTypeId])
REFERENCES [dbo].[tStatsTypes] ([statsTypeId])
ON UPDATE CASCADE
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[tSystemStats] CHECK CONSTRAINT [FK_tSystemStats_tStatsTypes]
GO
/****** Object:  ForeignKey [FK_tSystemStats_tTestcases]    Script Date: 04/11/2011 20:46:21 ******/
ALTER TABLE [dbo].[tSystemStats]  WITH CHECK ADD  CONSTRAINT [FK_tSystemStats_tTestcases] FOREIGN KEY([testcaseId])
REFERENCES [dbo].[tTestcases] ([testcaseId])
ON UPDATE CASCADE
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[tSystemStats] CHECK CONSTRAINT [FK_tSystemStats_tTestcases]
GO
/****** Object:  ForeignKey [FK_tSytemProperties_tMachines]    Script Date: 04/11/2011 20:46:21 ******/
ALTER TABLE [dbo].[tSytemProperties]  WITH CHECK ADD  CONSTRAINT [FK_tSytemProperties_tMachines] FOREIGN KEY([machineId])
REFERENCES [dbo].[tMachines] ([machineId])
GO
ALTER TABLE [dbo].[tSytemProperties] CHECK CONSTRAINT [FK_tSytemProperties_tMachines]
GO
/****** Object:  ForeignKey [FK_tSytemProperties_tRuns]    Script Date: 04/11/2011 20:46:21 ******/
ALTER TABLE [dbo].[tSytemProperties]  WITH CHECK ADD  CONSTRAINT [FK_tSytemProperties_tRuns] FOREIGN KEY([runId])
REFERENCES [dbo].[tRuns] ([runId])
ON UPDATE CASCADE
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[tSytemProperties] CHECK CONSTRAINT [FK_tSytemProperties_tRuns]
GO
/****** Object:  ForeignKey [FK_tTestcases_tScenarios]    Script Date: 04/11/2011 20:46:21 ******/
ALTER TABLE [dbo].[tTestcases]  WITH CHECK ADD  CONSTRAINT [FK_tTestcases_tScenarios] FOREIGN KEY([scenarioId])
REFERENCES [dbo].[tScenarios] ([scenarioId])
ON UPDATE CASCADE
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[tTestcases] CHECK CONSTRAINT [FK_tTestcases_tScenarios]
GO


/****** THE FOLLOWING DATA MUST BE ADDED AFTER THE SCHEMA IS READY ******/

/****** Fill in the message type levels we support ******/
insert into tMessageTypes (messageTypeId, name) values (1, 'fatal')
insert into tMessageTypes (messageTypeId, name) values (2, 'error')
insert into tMessageTypes (messageTypeId, name) values (3, 'warning')
insert into tMessageTypes (messageTypeId, name) values (4, 'info')
insert into tMessageTypes (messageTypeId, name) values (5, 'debug')
insert into tMessageTypes (messageTypeId, name) values (6, 'trace')
insert into tMessageTypes (messageTypeId, name) values (7, 'system')
GO


/****** Object:  Insert Into Table [dbo].[tColumnDefinition]    Script Date: 04/07/2014 16:46:20 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
SET ANSI_PADDING ON
GO
  INSERT INTO  [dbo].[tColumnDefinition]
     VALUES('Run',1,200,'tRuns',1),('Product',2,80,'tRuns',1),('Version',3,55,'tRuns',1),('Build',4,55,'tRuns',1),('OS',5,110,'tRuns',1),
     ('Total',6,65,'tRuns',1),('Failed',7,65,'tRuns',1),('Skipped',8,40,'tRuns',1),('Passed',9,40,'tRuns',1),('Running',10,80,'tRuns',0),
     ('Start',11,130,'tRuns',1),('End',12,130,'tRuns',1),('Duration',13,145,'tRuns',1),('User Note',14,135,'tRuns',1),

     ('Suite',1,300,'tSuite',1),('Total',2,65,'tSuite',1),('Failed',3,65,'tSuite',1),('Skipped',4,40,'tSuite',1),('Passed',5,40,'tSuite',1),
     ('Running',6,50,'tSuite',0),('Start',7,130,'tSuite',1),('End',8,130,'tSuite',1),('Duration',9,145,'tSuite',1),('User Note',10,310,'tSuite',1),
     ('Package',11,100,'tSuite',0),

     ('Scenario',1,245,'tScenario',1),('Description',2,130,'tScenario',1),('State',3,110,'tScenario',1),('TestcasesTotal',4,65,'tScenario',1),
     ('TestcasesFailed',5,65,'tScenario',1),('Passed',6,40,'tScenario',1),('Running',7,50,'tScenario',0),
     ('Start',8,130,'tScenario',1),('End',9,130,'tScenario',1),('Duration',10,145,'tScenario',1),('User Note',11,163,'tScenario',1),

     ('Testcase',1,350,'tTestcases',1),('State',2,110,'tTestcases',1),('Start',3,80,'tTestcases',1),('End',4,80,'tTestcases',1),
     ('Duration',5,145,'tTestcases',1),('User Note',6,360,'tTestcases',1),

     ('Date',1,60,'tTestcase',0),('Time',2,110,'tTestcase',1),('Thread',3,100,'tTestcase',1),('Machine',4,100,'tTestcase',1),
     ('Level',5,80,'tTestcase',1),('Message',6,840,'tTestcase',1);
GO

/****** Record the version w/o _draft as complete installation ******/
UPDATE tInternal SET [value]='4.0.6' WHERE [key] = 'version';
GO

