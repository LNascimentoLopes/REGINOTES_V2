ALTER TYPE note_role ADD VALUE IF NOT EXISTS 'OWNER';

CREATE OR REPLACE FUNCTION add_owner_as_collaborator()
RETURNS TRIGGER AS $$
BEGIN
INSERT INTO note_collaborators (note_id, user_id, role)
VALUES (NEW.id, NEW.note_owner_id, 'OWNER');
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_note_add_owner
    AFTER INSERT ON notes
    FOR EACH ROW EXECUTE FUNCTION add_owner_as_collaborator();
 
