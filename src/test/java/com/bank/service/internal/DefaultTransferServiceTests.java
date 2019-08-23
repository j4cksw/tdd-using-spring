package com.bank.service.internal;

import static com.bank.repository.internal.SimpleAccountRepository.Data.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import com.bank.service.TransferTimePolicy;
import org.junit.Before;
import org.junit.Test;

import com.bank.domain.InsufficientFundsException;
import com.bank.domain.TransferReceipt;
import com.bank.repository.AccountNotFoundException;
import com.bank.repository.AccountRepository;
import com.bank.repository.internal.SimpleAccountRepository;
import com.bank.service.FeePolicy;
import com.bank.service.TransferService;

import java.time.LocalTime;
import static org.mockito.Mockito.*;

public class DefaultTransferServiceTests {

    private AccountRepository accountRepository;
    private TransferService transferService;

    @Before
    public void setUp() {
        accountRepository = new SimpleAccountRepository();
        FeePolicy feePolicy = new ZeroFeePolicy();
        TransferTimePolicy timePolicy = new TiscoTimePolicy();
        transferService = new DefaultTransferService(accountRepository, feePolicy, timePolicy);

        assertThat(accountRepository.findById(A123_ID).getBalance(), equalTo(A123_INITIAL_BAL));
        assertThat(accountRepository.findById(C456_ID).getBalance(), equalTo(C456_INITIAL_BAL));
    }

    @Test
    public void testTransfer() throws Exception {
        double transferAmount = 100.00;

        TransferReceipt receipt = transferService.transfer(transferAmount, A123_ID, C456_ID);

        assertThat(receipt.getTransferAmount(), equalTo(transferAmount));
        assertThat(receipt.getFinalSourceAccount().getBalance(), equalTo(A123_INITIAL_BAL - transferAmount));
        assertThat(receipt.getFinalDestinationAccount().getBalance(), equalTo(C456_INITIAL_BAL + transferAmount));

        assertThat(accountRepository.findById(A123_ID).getBalance(), equalTo(A123_INITIAL_BAL - transferAmount));
        assertThat(accountRepository.findById(C456_ID).getBalance(), equalTo(C456_INITIAL_BAL + transferAmount));
    }

    @Test
    public void testInsufficientFunds() throws Exception {
        double overage = 9.00;
        double transferAmount = A123_INITIAL_BAL + overage;

        try {
            transferService.transfer(transferAmount, A123_ID, C456_ID);
            fail("expected InsufficientFundsException");
        } catch (InsufficientFundsException ex) {
            assertThat(ex.getTargetAccountId(), equalTo(A123_ID));
            assertThat(ex.getOverage(), equalTo(overage));
        }

        assertThat(accountRepository.findById(A123_ID).getBalance(), equalTo(A123_INITIAL_BAL));
        assertThat(accountRepository.findById(C456_ID).getBalance(), equalTo(C456_INITIAL_BAL));
    }

    @Test
    public void testNonExistentSourceAccount() throws Exception {
        try {
            transferService.transfer(1.00, Z999_ID, C456_ID);
            fail("expected AccountNotFoundException");
        } catch (AccountNotFoundException ex) {
        }

        assertThat(accountRepository.findById(C456_ID).getBalance(), equalTo(C456_INITIAL_BAL));
    }

    @Test
    public void testNonExistentDestinationAccount() throws Exception {
        try {
            transferService.transfer(1.00, A123_ID, Z999_ID);
            fail("expected AccountNotFoundException");
        } catch (AccountNotFoundException ex) {
        }

        assertThat(accountRepository.findById(A123_ID).getBalance(), equalTo(A123_INITIAL_BAL));
    }

    @Test
    public void testZeroTransferAmount() throws Exception {
        try {
            transferService.transfer(0.00, A123_ID, C456_ID);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }
    }

    @Test
    public void testNegativeTransferAmount() throws Exception {
        try {
            transferService.transfer(-100.00, A123_ID, C456_ID);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }
    }

    @Test
    public void testTransferAmountLessThanOneCent() throws Exception {
        try {
            transferService.transfer(0.009, A123_ID, C456_ID);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
        }
    }

    @Test
    public void testCustomizedMinimumTransferAmount() throws Exception {
        transferService.transfer(1.00, A123_ID, C456_ID); // should be fine
        transferService.setMinimumTransferAmount(10.00);
        transferService.transfer(10.00, A123_ID, C456_ID); // fine against new minimum
        try {
            transferService.transfer(9.00, A123_ID, C456_ID); // violates new minimum!
            fail("expected IllegalArgumentException on 9.00 transfer that violates 10.00 minimum");
        } catch (IllegalArgumentException ex) {
        }
    }

    @Test
    public void testNonZeroFeePolicy() throws Exception {
        double flatFee = 5.00;
        double transferAmount = 10.00;
        transferService = new DefaultTransferService(accountRepository, new FlatFeePolicy(flatFee), new TiscoTimePolicy());
        transferService.transfer(transferAmount, A123_ID, C456_ID);
        assertThat(accountRepository.findById(A123_ID).getBalance(), equalTo(A123_INITIAL_BAL - transferAmount - flatFee));
        assertThat(accountRepository.findById(C456_ID).getBalance(), equalTo(C456_INITIAL_BAL + transferAmount));
    }

    @Test
    public void testTransferInTimeShouldSuccess() throws Exception {

            double flatFee = 5.00;
            double transferAmount = 10.00;

            TransferTimePolicy timePolicy = mock(TransferTimePolicy.class);
            when(timePolicy.check(any())).thenReturn(true);

            transferService = new DefaultTransferService(accountRepository,
                    new FlatFeePolicy(flatFee),
                    timePolicy);
            transferService.transfer(transferAmount, A123_ID, C456_ID);

    }

    @Test(expected = ServiceTimeException.class)
    public void testTransferOverTimeTimeShouldThrowException() throws Exception {
        double flatFee = 5.00;
        double transferAmount = 10.00;

        TransferTimePolicy timePolicy = mock(TransferTimePolicy.class);
        when(timePolicy.check(any())).thenReturn(false);

        transferService = new DefaultTransferService(accountRepository,
                new FlatFeePolicy(flatFee),
                timePolicy);
        transferService.transfer(transferAmount, A123_ID, C456_ID);
    }
}
