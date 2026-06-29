CREATE OR REPLACE FUNCTION add_owner_as_collaborator()
RETURNS TRIGGER AS $$
BEGIN
INSERT INTO note_collaborators (note_id, user_id, role)
VALUES (NEW.id, NEW.owner_id, 'OWNER');
RETURN NEW;
END;
$$ LANGUAGE plpgsql;