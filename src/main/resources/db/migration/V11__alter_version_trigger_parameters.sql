CREATE OR REPLACE FUNCTION snapshot_note_version()
RETURNS TRIGGER AS $$
BEGIN
INSERT INTO note_versions (note_id, content, version, saved_by)
VALUES (
           OLD.id,
           OLD.content,
           (SELECT COALESCE(MAX(version), 0) + 1 FROM note_versions WHERE note_id = OLD.id),
           NEW.owner_id
       );
RETURN NEW;
END;
$$ LANGUAGE plpgsql;