package LNASC.REGINOTES.RabbitMQ;

import LNASC.REGINOTES.Config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SearchProducer{

    @Autowired
    RabbitTemplate rabbitTemplate;

    public void sendToIndex(UUID noteId){
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SEARCH_EXCHANGE,
                RabbitMQConfig.SEARCH_ROUTING_KEY,
                noteId.toString());
    }
}
