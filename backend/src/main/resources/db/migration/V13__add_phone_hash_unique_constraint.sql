-- Clean up phone hashes for non-active users so they don't occupy the unique constraint.
-- Deleted/banned users can re-register later; their historical hashes are no longer needed for uniqueness.
UPDATE `user` SET phone_hash = NULL, guardian_phone_hash = NULL WHERE status != 0;

-- Clean up phone hashes for resigned coaches so their numbers can be reused.
UPDATE `coach` SET phone_hash = NULL WHERE status = 3;

-- Add unique index on user phone_hash.
-- Multiple NULL values are allowed, so cleared records don't block new registrations.
ALTER TABLE `user` ADD UNIQUE INDEX `uk_user_phone_hash` (`phone_hash`);
