package LNASC.REGINOTES.Config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {


    public static final String EMAIL_QUEUE = "email.queue";
    public static final String EMAIL_EXCHANGE= "email.exchange";
    public static final String EMAIL_ROUTING_KEY = "email.routing";


    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public Queue emailQueue(){
        return new Queue(EMAIL_QUEUE, true);
    }
    @Bean
    public DirectExchange emailExchange(){
        return new DirectExchange(EMAIL_EXCHANGE);
    }
    @Bean
    public Binding emailBinding(Queue emailQueue,DirectExchange emailExchange){
        return BindingBuilder
                .bind(emailQueue)
                .to(emailExchange)
                .with(EMAIL_ROUTING_KEY);
    }


}
