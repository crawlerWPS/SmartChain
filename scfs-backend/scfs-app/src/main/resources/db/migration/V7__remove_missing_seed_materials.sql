-- V3 only created metadata for these demo files; no corresponding MinIO objects exist.
-- Remove the dangling material records so the UI does not expose broken preview/download actions.
DELETE FROM schema_verify.material_recognition_result
WHERE application_material_id IN (
    SELECT id FROM schema_verify.application_material WHERE file_object_id BETWEEN 101 AND 305
);

DELETE FROM schema_verify.application_material
WHERE file_object_id BETWEEN 101 AND 305;

DELETE FROM schema_common.file_object
WHERE id BETWEEN 101 AND 305
  AND content_hash LIKE 'hash_%';
