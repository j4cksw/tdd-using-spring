package com.bank.service.internal;

import static java.lang.String.format;

import com.bank.service.TransferTimePolicy;
import org.springframework.transaction.annotation.Transactional;

import com.bank.domain.Account;
import com.bank.domain.InsufficientFundsException;
import com.bank.domain.TransferReceipt;
import com.bank.repository.AccountRepository;
import com.bank.service.FeePolicy;
import com.bank.service.TransferService;

import java.time.LocalTime;

public class DefaultTransferService implements TransferService {

    private final AccountRepository accountRepository;
    private final FeePolicy feePolicy;
    private double minimumTransferAmount = 1.00;
    private TransferTimePolicy timePolicy;

    public DefaultTransferService(AccountRepository accountRepository,
                                  FeePolicy feePolicy,
                                  TransferTimePolicy timePolicy) {
        this.accountRepository = accountRepository;
        this.feePolicy = feePolicy;
        this.timePolicy = timePolicy;
    }

    @Override
    public void setMinimumTransferAmount(double minimumTransferAmount) {
        this.minimumTransferAmount = minimumTransferAmount;
    }

    @Override
    @Transactional
    public TransferReceipt transfer(double amount, String srcAcctId, String dstAcctId) throws InsufficientFundsException, ServiceTimeException {
        if (!this.timePolicy.check(LocalTime.now())) {
            throw new ServiceTimeException();
        }

        if (amount < minimumTransferAmount) {
            throw new IllegalArgumentException(format("transfer amount must be at least $%.2f", minimumTransferAmount));
        }

        TransferReceipt receipt = new TransferReceipt();

        Account srcAcct = accountRepository.findById(srcAcctId);
        Account dstAcct = accountRepository.findById(dstAcctId);

        receipt.setInitialSourceAccount(srcAcct);
        receipt.setInitialDestinationAccount(dstAcct);

        double fee = feePolicy.calculateFee(amount);
        if (fee > 0) {
            srcAcct.debit(fee);
        }

        receipt.setTransferAmount(amount);
        receipt.setFeeAmount(fee);

        srcAcct.debit(amount);
        dstAcct.credit(amount);

        accountRepository.updateBalance(srcAcct);
        accountRepository.updateBalance(dstAcct);

        receipt.setFinalSourceAccount(srcAcct);
        receipt.setFinalDestinationAccount(dstAcct);

        return receipt;
    }
}
