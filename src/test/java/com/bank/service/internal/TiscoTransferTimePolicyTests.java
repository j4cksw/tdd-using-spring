package com.bank.service.internal;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalTime;

public class TiscoTransferTimePolicyTests {

    @Test
    public void allowedTimeShouldReturnTrue(){
        TiscoTimePolicy timePolicy = new TiscoTimePolicy();
        LocalTime localTime = LocalTime.of(6, 0);
        Assert.assertTrue(timePolicy.check(localTime));
    }

    @Test
    public void disAllowedTimeShouldReturnFalse(){
        TiscoTimePolicy timePolicy = new TiscoTimePolicy();
        LocalTime localTime = LocalTime.of(22, 0);
        Assert.assertFalse(timePolicy.check(localTime));
    }
}
