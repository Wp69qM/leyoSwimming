ALTER TABLE `user`
    ADD COLUMN `phone_hash` VARCHAR(64) NULL DEFAULT NULL AFTER `phone`,
    ADD COLUMN `guardian_phone_hash` VARCHAR(64) NULL DEFAULT NULL AFTER `guardian_phone`;

ALTER TABLE `coach`
    ADD COLUMN `phone_hash` VARCHAR(64) NULL DEFAULT NULL AFTER `phone`;

-- Populate hashes for existing records using a fixed key assumption.
-- This is a one-time migration; actual hash values will be recomputed on next profile/login update
-- if the encryption key differs. The SHA-256 hash of the raw phone is independent of the key.
-- Since phone numbers are encrypted in DB, this migration decrypts them using the legacy fixed-IV
-- AES/CBC/PKCS5Padding and hashes the plaintext. This requires the application to run the
-- migration logic or a manual script with the correct key. For Flyway SQL we cannot decrypt,
-- so we leave hashes empty and let the application backfill on first read/write.
-- Backfill will be handled by a startup backfill bean (BackfillPhoneHashInitializer).
