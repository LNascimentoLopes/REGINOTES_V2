package LNASC.REGINOTES.RabbitMQ;

import LNASC.REGINOTES.Config.RabbitMQConfig;
import LNASC.REGINOTES.DTOs.WorkspaceDTOs.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmailProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void inviteByEmail(InviteEmailPayloadDTO request){
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EMAIL_EXCHANGE,
                RabbitMQConfig.EMAIL_ROUTING_KEY,
                request);
    }
}
