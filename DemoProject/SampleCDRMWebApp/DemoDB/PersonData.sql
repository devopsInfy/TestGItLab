CREATE TABLE [dbo].[PersonData]
(
    [Id] INT NOT NULL PRIMARY KEY, 
    [FirstName] NCHAR(10) NULL, 
    [MiddleName] NCHAR(10) NULL,
    [Age] INT NULL,
   -- [City] NVARCHAR(50) NULL,
    [LastName] NCHAR(10) NULL 
   -- [Description] NVARCHAR(50) NULL
)
