package com.bank.service.internal;

import com.bank.service.TransferTimePolicy;

import java.time.LocalTime;

public class TiscoTimePolicy implements TransferTimePolicy {

    LocalTime begin = LocalTime.of(5, 59);
    LocalTime end = LocalTime.of(21, 59);

    @Override
    public boolean check(LocalTime time) {
        return time.isAfter(begin) && time.isBefore(end);
    }
}
