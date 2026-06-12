package LNASC.REGINOTES.Config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String SEARCH_QUEUE = "search.queue";
    public static final String SEARCH_EXCHANGE= "search.exchange";
    public static final String SEARCH_ROUTING_KEY = "search.routing";

    @Bean
    public Queue searchQueue(){
        return new Queue(SEARCH_QUEUE, true);
    }
    @Bean
    public DirectExchange searchExchange(){
        return new DirectExchange(SEARCH_EXCHANGE);
    }
    @Bean
    public Binding searchBinding(Queue searchQueue,DirectExchange searchExchange){
        return BindingBuilder
                .bind(searchQueue)
                .to(searchExchange)
                .with(SEARCH_ROUTING_KEY);
    }
}
