CREATE TABLE IF NOT EXISTS `hospital`.`admitted_patients` (
  `admit_id` VARCHAR(45) NOT NULL,
  `patient_id` VARCHAR(45) NULL,
  `initial_diagnosis` VARCHAR(45) NULL,
  `arrival_date` DATETIME NULL,
  `discharge_date` DATETIME NULL,
  `treatment_id` VARCHAR(45) NULL,
  PRIMARY KEY (`admit_id`),
  INDEX `pat_id_idx` (`patient_id` ASC) VISIBLE,
  INDEX `treatments_idx` (`treatment_id` ASC) VISIBLE,
  CONSTRAINT `pat_id`
    FOREIGN KEY (`patient_id`)
    REFERENCES `hospital`.`patients` (`patient_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `treatments`
    FOREIGN KEY (`treatment_id`)
    REFERENCES `hospital`.`treatments` (`treatment_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB

CREATE TABLE IF NOT EXISTS `hospital`.`emergency_contacts` (
  `emergency_contact_id` VARCHAR(45) NOT NULL,
  `contact_name` VARCHAR(45) NULL,
  `contact_phone` VARCHAR(45) NULL,
  PRIMARY KEY (`emergency_contact_id`))
ENGINE = InnoDB

CREATE TABLE IF NOT EXISTS `hospital`.`employees` (
  `employee_id` VARCHAR(45) NOT NULL,
  `firstname` VARCHAR(45) NULL,
  `lastname` VARCHAR(45) NULL,
  `category` VARCHAR(45) NULL,
  PRIMARY KEY (`employee_id`),
  UNIQUE INDEX `lastname_UNIQUE` (`lastname` ASC) VISIBLE)
ENGINE = InnoDB

CREATE TABLE IF NOT EXISTS `hospital`.`insurance` (
  `insurance_id` VARCHAR(45) NOT NULL,
  `insurance_policy` VARCHAR(45) NULL,
  `insurance_company` VARCHAR(45) NULL,
  PRIMARY KEY (`insurance_id`))
ENGINE = InnoDB

CREATE TABLE IF NOT EXISTS `hospital`.`patients` (
  `patient_id` VARCHAR(45) NOT NULL,
  `firstname` VARCHAR(45) NULL DEFAULT NULL,
  `lastname` VARCHAR(45) NULL DEFAULT NULL,
  `room_id` VARCHAR(45) NULL DEFAULT NULL,
  `insurance_id` VARCHAR(45) NULL DEFAULT NULL,
  `emergency_contact_id` VARCHAR(45) NULL DEFAULT NULL,
  `employee_id` VARCHAR(45) NULL DEFAULT NULL,
  PRIMARY KEY (`patient_id`),
  UNIQUE INDEX `lastname_UNIQUE` (`lastname` ASC) VISIBLE,
  INDEX `rooms_idx` (`room_id` ASC) VISIBLE,
  INDEX `primary_doctor_idx` (`employee_id` ASC) VISIBLE,
  INDEX `insurance_idx` (`insurance_id` ASC) VISIBLE,
  INDEX `contact_idx` (`emergency_contact_id` ASC) VISIBLE,
  CONSTRAINT `rooms`
    FOREIGN KEY (`room_id`)
    REFERENCES `hospital`.`rooms` (`room_Id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `primary_doctor`
    FOREIGN KEY (`employee_id`)
    REFERENCES `hospital`.`employees` (`employee_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `insurance`
    FOREIGN KEY (`insurance_id`)
    REFERENCES `hospital`.`insurance` (`insurance_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `contact`
    FOREIGN KEY (`emergency_contact_id`)
    REFERENCES `hospital`.`emergency_contacts` (`emergency_contact_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB

CREATE TABLE IF NOT EXISTS `hospital`.`rooms` (
  `room_Id` VARCHAR(45) NOT NULL,
  `room_number` VARCHAR(45) NULL,
  PRIMARY KEY (`room_Id`))
ENGINE = InnoDB

CREATE TABLE IF NOT EXISTS `hospital`.`assigned_doctors` (
  `patient_id` VARCHAR(45) NOT NULL,
  `employee_id` VARCHAR(45) NULL,
  PRIMARY KEY (`patient_id`),
  INDEX `doctors_idx` (`employee_id` ASC) VISIBLE,
  CONSTRAINT `patients`
    FOREIGN KEY (`patient_id`)
    REFERENCES `hospital`.`patients` (`patient_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `doctors`
    FOREIGN KEY (`employee_id`)
    REFERENCES `hospital`.`employees` (`employee_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB

CREATE TABLE IF NOT EXISTS `hospital`.`treatments` (
  `treatment_id` VARCHAR(45) NOT NULL,
  `patient_id` VARCHAR(45) NULL,
  `employee_id` VARCHAR(45) NULL,
  `treatment_name` VARCHAR(45) NULL,
  `treatment_type` VARCHAR(45) NULL,
  `timestamp` DATETIME NULL,
  PRIMARY KEY (`treatment_id`),
  INDEX `patient_idx` (`patient_id` ASC) VISIBLE,
  INDEX `employee_idx` (`employee_id` ASC) VISIBLE,
  CONSTRAINT `patient`
    FOREIGN KEY (`patient_id`)
    REFERENCES `hospital`.`patients` (`patient_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `employee`
    FOREIGN KEY (`employee_id`)
    REFERENCES `hospital`.`employees` (`employee_id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
