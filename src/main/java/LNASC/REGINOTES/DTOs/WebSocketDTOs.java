package LNASC.REGINOTES.DTOs;

import java.util.UUID;

public class WebSocketDTOs {
    public record NoteEditPayload (UUID noteId, String content, UUID editorId){}
}
