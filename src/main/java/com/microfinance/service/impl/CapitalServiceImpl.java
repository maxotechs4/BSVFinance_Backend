package com.microfinance.service.impl;

import com.microfinance.dto.request.CapitalTransactionRequest;
import com.microfinance.dto.request.SetCapitalRequest;
import com.microfinance.dto.response.CapitalAccountResponse;
import com.microfinance.dto.response.CapitalTransactionResponse;
import com.microfinance.entity.CapitalAccount;
import com.microfinance.entity.CapitalTransaction;
import com.microfinance.entity.enums.CapitalTransactionType;
import com.microfinance.exception.ResourceNotFoundException;
import com.microfinance.repository.CapitalAccountRepository;
import com.microfinance.repository.CapitalTransactionRepository;
import com.microfinance.repository.PaymentRepository;
import com.microfinance.repository.SavingsPaymentRepository;
import com.microfinance.service.CapitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CapitalServiceImpl implements CapitalService {

    /** Singleton row id — this app only ever has one capital account. */
    private static final Long SINGLETON_ID = 1L;

    private final CapitalAccountRepository capitalAccountRepository;
    private final CapitalTransactionRepository capitalTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final SavingsPaymentRepository savingsPaymentRepository;

    // ------------------------------------------------------------------
    // Capital account
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public CapitalAccountResponse getAccount() {
        return toResponse(getOrCreateAccount());
    }

    @Override
    @Transactional
    public CapitalAccountResponse setCapitalAmount(SetCapitalRequest request) {
        CapitalAccount account = getOrCreateAccount();
        // Adds to the existing capital amount (a top-up), not a replacement —
        // entering 10,100 when capital is already 10,060 should result in
        // 20,160, not overwrite it down to 10,100.
        BigDecimal current = account.getCapitalAmount() != null ? account.getCapitalAmount() : BigDecimal.ZERO;
        account.setCapitalAmount(current.add(request.getCapitalAmount()));
        capitalAccountRepository.save(account);
        return toResponse(account);
    }

    @Override
    @Transactional
    public BigDecimal[] drawForNewLoan(BigDecimal loanAmount) {
        if (loanAmount == null || loanAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
        }

        CapitalAccount account = getOrCreateAccount();
        BigDecimal available = account.getCapitalAmount() != null ? account.getCapitalAmount() : BigDecimal.ZERO;

        BigDecimal capitalPortion;
        BigDecimal collectionPortion;

        if (available.compareTo(loanAmount) >= 0) {
            // Capital alone covers the whole loan.
            capitalPortion = loanAmount;
            collectionPortion = BigDecimal.ZERO;
            account.setCapitalAmount(available.subtract(loanAmount));
        } else {
            // Capital covers what it can; the rest is drawn from the
            // Total Collection pool instead.
            capitalPortion = available;
            collectionPortion = loanAmount.subtract(available);
            account.setCapitalAmount(BigDecimal.ZERO);
            BigDecimal utilizedSoFar = account.getCollectionUtilized() != null
                    ? account.getCollectionUtilized()
                    : BigDecimal.ZERO;
            account.setCollectionUtilized(utilizedSoFar.add(collectionPortion));
        }

        capitalAccountRepository.save(account);
        return new BigDecimal[]{capitalPortion, collectionPortion};
    }

    @Override
    @Transactional
    public void reverseForDeletedLoan(BigDecimal capitalPortion, BigDecimal collectionPortion) {
        CapitalAccount account = getOrCreateAccount();

        if (capitalPortion != null && capitalPortion.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal current = account.getCapitalAmount() != null ? account.getCapitalAmount() : BigDecimal.ZERO;
            account.setCapitalAmount(current.add(capitalPortion));
        }

        if (collectionPortion != null && collectionPortion.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal current = account.getCollectionUtilized() != null ? account.getCollectionUtilized() : BigDecimal.ZERO;
            account.setCollectionUtilized(current.subtract(collectionPortion).max(BigDecimal.ZERO));
        }

        capitalAccountRepository.save(account);
    }

    /** Fetches the singleton capital account row, creating it (at zero) the first time it's ever needed. */
    private CapitalAccount getOrCreateAccount() {
        return capitalAccountRepository.findById(SINGLETON_ID)
                .orElseGet(() -> capitalAccountRepository.save(
                        CapitalAccount.builder()
                                .id(SINGLETON_ID)
                                .capitalAmount(BigDecimal.ZERO)
                                .collectionUtilized(BigDecimal.ZERO)
                                .build()
                ));
    }

    /**
     * Builds the API response: capitalAmount and collectionUtilized as
     * stored, totalCollectionAllTime summed fresh from payments, and
     * totalAmount = capitalAmount + totalCollectionAllTime - collectionUtilized
     * (never allowed below zero).
     */
    private CapitalAccountResponse toResponse(CapitalAccount account) {
        BigDecimal totalCollectionAllTime = paymentRepository.sumAllAmountPaid();
        if (totalCollectionAllTime == null) {
            totalCollectionAllTime = BigDecimal.ZERO;
        }
        // Monthly Savings deposits count toward Total Collection too.
        BigDecimal savingsCollected = savingsPaymentRepository.sumAllAmountPaid();
        if (savingsCollected == null) {
            savingsCollected = BigDecimal.ZERO;
        }
        totalCollectionAllTime = totalCollectionAllTime.add(savingsCollected);

        BigDecimal capitalAmount = account.getCapitalAmount() != null
                ? account.getCapitalAmount()
                : BigDecimal.ZERO;

        BigDecimal collectionUtilized = account.getCollectionUtilized() != null
                ? account.getCollectionUtilized()
                : BigDecimal.ZERO;

        BigDecimal totalAmount = capitalAmount
                .add(totalCollectionAllTime)
                .subtract(collectionUtilized)
                .max(BigDecimal.ZERO);

        return CapitalAccountResponse.builder()
                .capitalAmount(capitalAmount)
                .collectionUtilized(collectionUtilized)
                .totalCollectionAllTime(totalCollectionAllTime)
                .totalAmount(totalAmount)
                .build();
    }

    // ------------------------------------------------------------------
    // Income / Expense entries
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public List<CapitalTransactionResponse> listTransactions() {
        return capitalTransactionRepository.findAllByOrderByTransactionDateDescCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CapitalTransactionResponse createTransaction(CapitalTransactionRequest request) {
        CapitalTransaction transaction = CapitalTransaction.builder()
                .type(request.getType())
                .amount(request.getAmount())
                .purpose(request.getPurpose())
                .transactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : LocalDate.now())
                .build();

        CapitalTransaction saved = capitalTransactionRepository.save(transaction);

        applyToCollection(saved.getType(), saved.getAmount());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public CapitalTransactionResponse updateTransaction(Long id, CapitalTransactionRequest request) {
        CapitalTransaction transaction = capitalTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found with id: " + id));

        // Reverse the old amount's effect before applying the new one, so
        // editing an entry doesn't double-count.
        reverseFromCollection(transaction.getType(), transaction.getAmount());

        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setPurpose(request.getPurpose());
        transaction.setTransactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : transaction.getTransactionDate());

        CapitalTransaction saved = capitalTransactionRepository.save(transaction);

        applyToCollection(saved.getType(), saved.getAmount());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTransaction(Long id) {
        CapitalTransaction transaction = capitalTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found with id: " + id));

        reverseFromCollection(transaction.getType(), transaction.getAmount());

        capitalTransactionRepository.delete(transaction);
    }

    /**
     * Applies an entry's effect to the Total Collection pool — never to
     * capitalAmount, which only changes from the admin's Capital Amount box
     * and from loan funding (see drawForNewLoan). EXPENSE increases
     * collectionUtilized (less available from collection); INCOME decreases
     * it, which is allowed to go negative — that's a genuine boost to the
     * available collection balance beyond the raw payments total.
     */
    private void applyToCollection(CapitalTransactionType type, BigDecimal amount) {
        CapitalAccount account = getOrCreateAccount();
        BigDecimal current = account.getCollectionUtilized() != null ? account.getCollectionUtilized() : BigDecimal.ZERO;

        if (type == CapitalTransactionType.EXPENSE) {
            account.setCollectionUtilized(current.add(amount));
        } else {
            account.setCollectionUtilized(current.subtract(amount));
        }

        capitalAccountRepository.save(account);
    }

    /** Undoes an entry's effect on the collection pool — used before editing/deleting it. */
    private void reverseFromCollection(CapitalTransactionType type, BigDecimal amount) {
        CapitalAccount account = getOrCreateAccount();
        BigDecimal current = account.getCollectionUtilized() != null ? account.getCollectionUtilized() : BigDecimal.ZERO;

        if (type == CapitalTransactionType.EXPENSE) {
            account.setCollectionUtilized(current.subtract(amount));
        } else {
            account.setCollectionUtilized(current.add(amount));
        }

        capitalAccountRepository.save(account);
    }

    private CapitalTransactionResponse toResponse(CapitalTransaction transaction) {
        return CapitalTransactionResponse.builder()
                .id(transaction.getId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .purpose(transaction.getPurpose())
                .transactionDate(transaction.getTransactionDate())
                .build();
    }
}