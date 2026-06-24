package LNASC.REGINOTES.RabbitMQ;

import LNASC.REGINOTES.Config.RabbitMQConfig;
import LNASC.REGINOTES.DTOs.WorkspaceDTOs.*;
import LNASC.REGINOTES.Services.EmailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmailConsumer {

    @Autowired
    private EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void consumeEmailMessage(InviteEmailPayloadDTO request){
        emailService.sendInviteEmail(request.email(), request.userName(), request.workspaceName(), request.workspaceId());

    }

}
