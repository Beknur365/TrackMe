package kz.beknur.controller;

import kz.beknur.services.UpdateProducer;
import kz.beknur.utils.MessageUtils;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import static kz.beknur.model.RabbitQueue.*;

@Component
@Log4j
public class UpdateController {
    private TelegramBot telegramBot;
    private MessageUtils messageUtils;
    private UpdateProducer updateProducer;
    @Autowired
    public UpdateController(MessageUtils messageUtils, UpdateProducer updateProducer) {
        this.messageUtils = messageUtils;
        this.updateProducer = updateProducer;
    }

    public void register(TelegramBot telegramBot){
        this.telegramBot = telegramBot;
    }

    public void processUpdate(Update update){
        if(update == null){
            log.error("Update is null");
            return;
        }
        if(update.getMessage() != null){
            distributedMessageByType(update);
        }
        else {
            log.error("Message is not supported: " + update);
        }
    }
    private void distributedMessageByType(Update update){
        Message msg = update.getMessage();
        if(msg.getText() != null){
            processTextMessage(update);
        }
        else if(msg.getDocument() != null){
            processDocMessage(update);
        }
        else if(msg.getPhoto() != null){
            processPhotoMessage(update);
        }
        else {
            setUnsupportedMessage(update);
        }
    }

    private void setUnsupportedMessage(Update update) {
        SendMessage sendMessage = messageUtils.generateSendMessage(update,
                "Неподдреживаемый тип сообщения");
        setView(sendMessage);
    }

    private void setFileReceiverView(Update update) {
        SendMessage sendMessage = messageUtils.generateSendMessage(update,
                "Файл получен, дорогой! Обрабатывается...");
        setView(sendMessage);
    }

    private void setView(SendMessage sendMessage) {
        telegramBot.sendAnswerMessage(sendMessage);
    }
    private void processPhotoMessage(Update update) {
        updateProducer.produce(PHOTO_MESSAGE_UPDATE, update);
        setFileReceiverView(update);
    }

    private void processDocMessage(Update update) {
        updateProducer.produce(DOC_MESSAGE_UPDATE, update);
        setFileReceiverView(update);
    }

    private void processTextMessage(Update update) {
        updateProducer.produce(TEXT_MESSAGE_UPDATE, update);
        setFileReceiverView(update);
    }
}
