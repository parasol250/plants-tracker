-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `mydb` DEFAULT CHARACTER SET utf8 ;
USE `mydb` ;

-- -----------------------------------------------------
-- Table `mydb`.`PlantSpecies`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mydb`.`PlantSpecies` (
  `ID` INT NOT NULL AUTO_INCREMENT,
  `Name` VARCHAR(70) NOT NULL,
  `CareAdvice` MEDIUMTEXT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE INDEX `ID_UNIQUE` (`ID` ASC) VISIBLE,
  UNIQUE INDEX `Name_UNIQUE` (`Name` ASC) VISIBLE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `mydb`.`Plants`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mydb`.`Plants` (
  `ID` INT NOT NULL AUTO_INCREMENT,
  `Name` VARCHAR(60) NOT NULL,
  `PhotoPath` VARCHAR(255) NULL,
  `PlantSpeciesID` INT NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE INDEX `ID_UNIQUE` (`ID` ASC) VISIBLE,
  INDEX `fk_Plants_PlantSpecies1_idx` (`PlantSpeciesID` ASC) VISIBLE,
  CONSTRAINT `fk_Plants_PlantSpecies1`
    FOREIGN KEY (`PlantSpeciesID`)
    REFERENCES `mydb`.`PlantSpecies` (`ID`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `mydb`.`ProcedureType`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mydb`.`ProcedureType` (
  `ID` INT NOT NULL AUTO_INCREMENT,
  `Name` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE INDEX `ID_UNIQUE` (`ID` ASC) VISIBLE,
  UNIQUE INDEX `Name_UNIQUE` (`Name` ASC) VISIBLE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `mydb`.`CareSchedules`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mydb`.`CareSchedules` (
  `ID` INT NOT NULL AUTO_INCREMENT,
  `PlantID` INT NOT NULL,
  `Frequency` INT NOT NULL,
  `NextDate` DATE NOT NULL,
  `ProcedureTypeID` INT NOT NULL,
  PRIMARY KEY (`ID`),
  INDEX `fk_CareSchedules_ProcedureType1_idx` (`ProcedureTypeID` ASC) VISIBLE,
  UNIQUE INDEX `ID_UNIQUE` (`ID` ASC) VISIBLE,
  CONSTRAINT `fk_CareSchedules_ProcedureType1`
    FOREIGN KEY (`ProcedureTypeID`)
    REFERENCES `mydb`.`ProcedureType` (`ID`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `mydb`.`CareHistory`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mydb`.`CareHistory` (
  `ID` INT NOT NULL AUTO_INCREMENT,
  `PlantsID` INT NOT NULL,
  `PerformedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `Note` VARCHAR(255) NULL,
  `ProcedureTypeID` INT NOT NULL,
  PRIMARY KEY (`ID`),
  INDEX `fk_CareHistory_Plants1_idx` (`PlantsID` ASC) VISIBLE,
  INDEX `fk_CareHistory_ProcedureType1_idx` (`ProcedureTypeID` ASC) VISIBLE,
  UNIQUE INDEX `ID_UNIQUE` (`ID` ASC) VISIBLE,
  CONSTRAINT `fk_CareHistory_Plants1`
    FOREIGN KEY (`PlantsID`)
    REFERENCES `mydb`.`Plants` (`ID`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `fk_CareHistory_ProcedureType1`
    FOREIGN KEY (`ProcedureTypeID`)
    REFERENCES `mydb`.`ProcedureType` (`ID`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `mydb`.`Reminders`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mydb`.`Reminders` (
  `ID` INT NOT NULL AUTO_INCREMENT,
  `TimeShow` DATETIME NOT NULL,
  `IsShown` TINYINT NOT NULL DEFAULT 0,
  `CareSchedulesID` INT NOT NULL,
  PRIMARY KEY (`ID`),
  INDEX `fk_Reminders_CareSchedules1_idx` (`CareSchedulesID` ASC) VISIBLE,
  UNIQUE INDEX `ID_UNIQUE` (`ID` ASC) VISIBLE,
  CONSTRAINT `fk_Reminders_CareSchedules1`
    FOREIGN KEY (`CareSchedulesID`)
    REFERENCES `mydb`.`CareSchedules` (`ID`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
