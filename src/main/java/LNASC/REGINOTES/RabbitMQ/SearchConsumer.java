package LNASC.REGINOTES.RabbitMQ;

import LNASC.REGINOTES.Config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class SearchConsumer {

    @RabbitListener(queues = RabbitMQConfig.SEARCH_QUEUE)
    public void consumeSearchMessage(String noteId){

    }
}
