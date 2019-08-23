package com.bank.service;

import java.time.LocalTime;
import java.util.Date;

public interface TransferTimePolicy {

    boolean check(LocalTime localTime);
}
