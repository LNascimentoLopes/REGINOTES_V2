package LNASC.REGINOTES.Controllers;


import LNASC.REGINOTES.DTOs.WebSocketDTOs.NoteEditPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class NoteWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/note.edit")
    public void handleNoteEdit(NoteEditPayload payload){
        messagingTemplate.convertAndSend(
                "/topic/note."+ payload.noteId()
                ,payload);
    }
}
