UPDATE `user` SET phone_hash = NULL, guardian_phone_hash = NULL WHERE status != 0;
UPDATE `coach` SET phone_hash = NULL WHERE status = 3;
ALTER TABLE `user` ADD UNIQUE INDEX `uk_user_phone_hash` (`phone_hash`);
