package org.leralix.tan.dataclass.newhistory;

public enum TransactionHistoryEnum {
    CHUNK_SPENDING,
    MISCELLANEOUS,
    SALARY,
    DONATION,
    PLAYER_TAX,
    SUBJECT_TAX;

    public TransactionHistory createTransactionHistory(String date, String territoryDataID, String transactionParty, double amount) {
        return switch (this) {
            case CHUNK_SPENDING -> new ChunkPaymentHistory(date, territoryDataID, amount);
            case MISCELLANEOUS -> new MiscellaneousHistory(date, territoryDataID, amount);
            case SALARY -> new SalaryPaymentHistory(date, territoryDataID, transactionParty, amount);
            case DONATION -> new PlayerDonationHistory(date, territoryDataID, transactionParty, amount);
            case PLAYER_TAX -> new PlayerTaxHistory(date, territoryDataID, transactionParty, amount);
            case SUBJECT_TAX -> new SubjectTaxHistory(date, territoryDataID, transactionParty, amount);
            default -> null;
        };
    }
}