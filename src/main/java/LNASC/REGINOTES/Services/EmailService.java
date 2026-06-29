package LNASC.REGINOTES.Services;

import LNASC.REGINOTES.DTOs.WorkspaceDTOs.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendResetCode(String to, String code){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Password redefinition code");
        message.setText("Your redefinition code is: "+ code);

        mailSender.send(message);
    }

    public void sendInviteEmail(String to, String inviterName, String name, UUID id, String resourceType) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(inviterName + " te convidou para colaborar no Reginotes");
        message.setText(
                inviterName + " convidou você para participar de \"" + name + "\".\n\n" +
                        "Acesse o link para aceitar: https://reginotes.com/" + resourceType.toLowerCase() + "/invite/" + id
        );
        mailSender.send(message);
    }
}
