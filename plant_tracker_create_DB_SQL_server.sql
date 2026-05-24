IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = N'mydb')
BEGIN
    CREATE DATABASE [mydb];
END;
GO

USE [mydb];
GO

IF OBJECT_ID(N'dbo.PlantSpecies', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[PlantSpecies] (
        [ID] INT IDENTITY(1,1) NOT NULL,
        [Name] NVARCHAR(70) NOT NULL,
        [CareAdvice] NVARCHAR(MAX) NULL,
        CONSTRAINT [PK_PlantSpecies] PRIMARY KEY CLUSTERED ([ID] ASC),
        CONSTRAINT [UQ_PlantSpecies_Name] UNIQUE ([Name] ASC)
    );
END;
GO

IF OBJECT_ID(N'dbo.Plants', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Plants] (
        [ID] INT IDENTITY(1,1) NOT NULL,
        [Name] NVARCHAR(60) NOT NULL,
        [PhotoPath] NVARCHAR(255) NULL,
        [PlantSpeciesID] INT NOT NULL,
        CONSTRAINT [PK_Plants] PRIMARY KEY CLUSTERED ([ID] ASC),
        CONSTRAINT [FK_Plants_PlantSpecies] FOREIGN KEY ([PlantSpeciesID])
            REFERENCES [dbo].[PlantSpecies] ([ID])
            ON DELETE NO ACTION
            ON UPDATE NO ACTION
    );
END;
GO

IF OBJECT_ID(N'dbo.ProcedureType', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[ProcedureType] (
        [ID] INT IDENTITY(1,1) NOT NULL,
        [Name] NVARCHAR(50) NOT NULL,
        CONSTRAINT [PK_ProcedureType] PRIMARY KEY CLUSTERED ([ID] ASC),
        CONSTRAINT [UQ_ProcedureType_Name] UNIQUE ([Name] ASC)
    );
END;
GO

IF OBJECT_ID(N'dbo.CareSchedules', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[CareSchedules] (
        [ID] INT IDENTITY(1,1) NOT NULL,
        [PlantID] INT NOT NULL,
        [Frequency] INT NOT NULL,
        [NextDate] DATE NOT NULL,
        [ProcedureTypeID] INT NOT NULL,
        CONSTRAINT [PK_CareSchedules] PRIMARY KEY CLUSTERED ([ID] ASC),
        CONSTRAINT [FK_CareSchedules_ProcedureType] FOREIGN KEY ([ProcedureTypeID])
            REFERENCES [dbo].[ProcedureType] ([ID])
            ON DELETE NO ACTION
            ON UPDATE NO ACTION,
        CONSTRAINT [FK_CareSchedules_Plants] FOREIGN KEY ([PlantID])
            REFERENCES [dbo].[Plants] ([ID])
            ON DELETE CASCADE
            ON UPDATE NO ACTION
    );
END;
GO

IF OBJECT_ID(N'dbo.CareHistory', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[CareHistory] (
        [ID] INT IDENTITY(1,1) NOT NULL,
        [PlantsID] INT NOT NULL,
        [PerformedAt] DATETIME2 NOT NULL CONSTRAINT [DF_CareHistory_PerformedAt] DEFAULT GETDATE(),
        [Note] NVARCHAR(255) NULL,
        [ProcedureTypeID] INT NOT NULL,
        CONSTRAINT [PK_CareHistory] PRIMARY KEY CLUSTERED ([ID] ASC),
        CONSTRAINT [FK_CareHistory_Plants] FOREIGN KEY ([PlantsID])
            REFERENCES [dbo].[Plants] ([ID])
            ON DELETE CASCADE
            ON UPDATE NO ACTION,
        CONSTRAINT [FK_CareHistory_ProcedureType] FOREIGN KEY ([ProcedureTypeID])
            REFERENCES [dbo].[ProcedureType] ([ID])
            ON DELETE NO ACTION
            ON UPDATE NO ACTION
    );
END;
GO

IF OBJECT_ID(N'dbo.Reminders', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[Reminders] (
        [ID] INT IDENTITY(1,1) NOT NULL,
        [TimeShow] DATETIME2 NOT NULL,
        [IsShown] BIT NOT NULL CONSTRAINT [DF_Reminders_IsShown] DEFAULT 0,
        [CareSchedulesID] INT NOT NULL,
        CONSTRAINT [PK_Reminders] PRIMARY KEY CLUSTERED ([ID] ASC),
        CONSTRAINT [FK_Reminders_CareSchedules] FOREIGN KEY ([CareSchedulesID])
            REFERENCES [dbo].[CareSchedules] ([ID])
            ON DELETE CASCADE
            ON UPDATE NO ACTION
    );
END;
GO