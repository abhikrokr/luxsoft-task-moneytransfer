package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import org.springframework.stereotype.Service;

@Service
public interface NotificationService {

  void notifyAboutTransfer(Account account, String transferDescription);
}
