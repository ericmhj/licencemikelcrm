package com.mikelcrm.licenseservice.payment.validation;

public final class CardValidator {
    private CardValidator() {}

    public static boolean isValidCardNumber(String number) {
        String clean = number.replaceAll("\\s+", "");
        if (!clean.matches("\\d{13,19}")) return false;
        return passesLuhn(clean);
    }

    public static boolean isValidExpiry(String expiry) {
        if (!expiry.matches("\\d{2}/\\d{2}")) return false;
        String[] parts = expiry.split("/");
        int month = Integer.parseInt(parts[0]);
        int year = Integer.parseInt(parts[1]) + 2000;
        if (month < 1 || month > 12) return false;
        java.time.YearMonth cardExpiry = java.time.YearMonth.of(year, month);
        return !cardExpiry.isBefore(java.time.YearMonth.now());
    }

    public static boolean isValidCvv(String cvv) {
        return cvv != null && cvv.matches("\\d{3,4}");
    }

    private static boolean passesLuhn(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = number.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }
}
